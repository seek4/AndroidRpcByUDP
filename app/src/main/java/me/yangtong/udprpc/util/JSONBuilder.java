package me.yangtong.udprpc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONBuilder {
	private JSONObject json;

	public JSONBuilder() {
		json = new JSONObject();
	}

	public JSONBuilder(byte[] data) {
		if (data == null) {
			this.json = new JSONObject();
			return;
		}
		try {
			this.json = new JSONObject(new String(data));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONBuilder(String data) {
		if (data == null) {
			this.json = new JSONObject();
			return;
		}
		try {
			this.json = new JSONObject(data);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONBuilder(JSONObject json) {
		this.json = json;
	}

	public JSONBuilder(File file) {
		StringBuffer sb = new StringBuffer();
		if (file.exists()) {
			InputStreamReader reader = null;
			try {
				reader = new InputStreamReader(new FileInputStream(file),
						"UTF-8");
				char[] buff = new char[1024];
				int hasRead = -1;
				while ((hasRead = reader.read(buff)) > 0) {
					sb.append(buff, 0, hasRead);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		try {
			this.json = new JSONObject(sb.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONObject getJSONObject() {
		return json;
	}

	public JSONBuilder put(String key, Object val) {
		if (json == null) {
			json = new JSONObject();
		}
		if (val != null) {
			try {
				if (val.getClass().isArray()) {
					Object[] objArr = (Object[]) val;
					JSONArray jArr = new JSONArray();
					for (Object obj : objArr) {
						jArr.put(obj);
					}
					json.put(key, jArr);
				} else if (val instanceof Collection) {
					JSONArray jArr = new JSONArray((Collection) val);
					json.put(key, jArr);
				} else {
					json.put(key, val);

				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	public JSONBuilder remove(String key) {
		json.remove(key);
		return this;
	}

	public <T> T getVal(String key, Class<T> clazz) {
		return getVal(key, clazz, null);
	}

	public <T> T getVal(String key, Class<T> clazz, T def) {
		if (json != null && json.has(key)) {
			try {
				Object obj = json.get(key);
				if (clazz == Double.class) {
					return (T) new Double(json.getDouble(key));
				} else if (clazz == Float.class) {
					return (T) new Float(json.getDouble(key));
				} else if (clazz == Integer.class) {
					return (T) new Integer(json.getInt(key));
				} else if (clazz == Long.class) {
					return (T) new Long(json.getLong(key));
				}

				if (obj instanceof JSONArray && clazz.isArray()) {
					JSONArray jArr = json.getJSONArray(key);
					Class componentType = clazz.getComponentType();
					Object[] arr = (Object[]) Array.newInstance(componentType,
							jArr.length());
					for (int i = 0, len = jArr.length(); i < len; i++) {
						if (componentType == Double.class) {
							arr[i] = jArr.getDouble(i);
						} else if (componentType == Float.class) {
							arr[i] = (float) jArr.getDouble(i);
						} else if (componentType == Integer.class) {
							arr[i] = jArr.getInt(i);
						} else if (componentType == Long.class) {
							arr[i] = jArr.getLong(i);
						} else {
							arr[i] = jArr.get(i);
						}
					}
					return (T) arr;
				}
				return (T) obj;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return def;
	}

	// public <T> T[] getArrVal(String key, Class<T> clazz) {
	// if (json.has(key)) {
	// try {
	// Object obj = json.get(key);
	// if (obj instanceof JSONArray) {
	// JSONArray jArr = json.getJSONArray(key);
	// T[] arr = (T[]) Array.newInstance(clazz, jArr.length());
	// for (int i = 0, len = jArr.length(); i < len; i++) {
	// arr[i] = (T) jArr.get(i);
	// }
	// return arr;
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }
	// return null;
	// }

	public JSONObject build() {
		return this.json;
	}

	@Override
	public String toString() {
		return json.toString();
	}

	public byte[] toBytes() {
		return toString().getBytes();
	}

	public static String toPostString(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	public static String toPostString(JSONObject obj) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		JSONArray names = obj.names();
		for (int i = 0; i < names.length(); ++i) {
			if (i > 0)
				sb.append(',');
			try {
				String name = names.getString(i);
				Object o = obj.get(name);
				sb.append(toPostString(name));
				sb.append(':');
				if (o instanceof JSONObject) {
					sb.append(toPostString((JSONObject) o));
					continue;
				}
				if (o instanceof JSONArray) {
					sb.append(toPostString((JSONArray) o));
					continue;
				}
				if (o instanceof String) {
					sb.append(toPostString((String) o));
					continue;
				}
				sb.append(o.toString());
			} catch (Exception e) {

			}
		}
		sb.append('}');
		return sb.toString();
	}

	public static String toPostString(JSONArray obj) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < obj.length(); ++i) {
			if (i > 0)
				sb.append(',');
			try {
				Object o = obj.get(i);
				if (o instanceof JSONObject) {
					sb.append(toPostString((JSONObject) o));
					continue;
				}
				if (o instanceof JSONArray) {
					sb.append(toPostString((JSONArray) o));
					continue;
				}
				if (o instanceof String) {
					sb.append(toPostString((String) o));
					continue;
				}
				sb.append(o.toString());
			} catch (Exception e) {

			}
		}
		sb.append(']');
		return sb.toString();
	}

	public String toPostString() {
		return toPostString(json);
	}
}
