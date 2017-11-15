package me.yangtong.udprpc;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.yangtong.udprpc.base.UdpConfiger.UdpAddress;
import me.yangtong.udprpc.base.UdpDataFactory.UdpData;
import me.yangtong.udprpc.client.UdpClient;
import me.yangtong.udprpc.util.CommUtil;
import me.yangtong.udprpc.util.LogUtil;
import me.yangtong.udprpc.util.Runnable1;
import me.yangtong.udprpc.util.JSONBuilder;
import me.yangtong.udprpc.util.StringUtils;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

/**
 * UdpRpc的访问入口类及连接等的管理类
 * @author Terry
 *
 */
public class MsgSender {

	private MsgSender(){}
	
	private static MsgSender sInstance = new MsgSender();
	
	public static MsgSender getInstance(){
		return sInstance;
	}
	
	private boolean mIsInConnection = false;
	private UdpClient mClient;
	private int mUdpId = 0;
	private HandlerThread mThreadCheck;
	private Handler mHandlerCheck;
	private HandlerThread mThreadSendInvoke;
	private Handler mHandlerSendInvoke;
	private static final int INTERVAL_CHECK = 5000;
	private static final String FILE_PORT = Environment.getExternalStorageDirectory()+"/txz/udp_port.txz";
//	private Map<String, UdpAddress> mMapAddress = new HashMap<String, UdpConfiger.UdpAddress>();
	private UdpAddress mServerAddr;
	private static final String TAG = "MsgSender ";
	private String mProcessName = "";

	public void init(Context context) {
		mProcessName = CommUtil.getProcessName(context);
		mThreadCheck = new HandlerThread("udpCheckConnection");
		mThreadCheck.start();
		mHandlerCheck = new Handler(mThreadCheck.getLooper());
		mHandlerCheck.post(mTaskCheckConnection);
		mThreadSendInvoke = new HandlerThread("udpProcess");
		mThreadSendInvoke.start();
		mHandlerSendInvoke = new Handler(mThreadSendInvoke.getLooper());
	}


	private Runnable mTaskCheckConnection = new Runnable() {
		@Override
		public void run() {
			mHandlerCheck.removeCallbacks(mTaskCheckConnection);
			checkPortFile();
			checkConnection();
			mHandlerCheck.postDelayed(mTaskCheckConnection, INTERVAL_CHECK);
		}
	};
	
	/**
	 * 是否可用
	 * @return
	 */
	public boolean isInConnection(){
		return mIsInConnection;
	}


	private List<UdpData> mListInvokes = new ArrayList<UdpData>();

	private void procQueue() {
		if (!mIsInConnection) {
			mHandlerCheck.post(mTaskCheckConnection);
			return;
		}
		for (int i = 0; i < mListInvokes.size(); ) {
			UdpData udpData = mListInvokes.get(i);
			if (udpData == null) {
				mListInvokes.remove(i);
				continue;
			}
			final UdpData udpDataSend = mListInvokes.get(0);
			// if (udpDataSend.cmd == UdpData.CMD_LOG) {
			// invokeLog(mUdpId, udpDataSend.data);
			// }
			mClient.sendInvoke(udpDataSend, mServerAddr);
			mListInvokes.remove(i);
		}
	}

//	private int mNextSeq = 0;

//	public void invokeLog(int udpId, byte[] data) {
//		JSONBuilder jsonDoc = new JSONBuilder(new String(data));
//		int level = jsonDoc.getVal("level", Integer.class);
//		String tag = jsonDoc.getVal("tag", String.class);
//		String content = jsonDoc.getVal("content", String.class);
//		Integer pid = jsonDoc.getVal("pid", Integer.class, 0);
//		Long tid = jsonDoc.getVal("tid", Long.class, 0L);
//		Integer seq = jsonDoc.getVal("seq", Integer.class);
//		String packageName = jsonDoc.getVal("package", String.class);
//		if (udpId == 0) {
//			Log.i("yangtong", TAG+"[" + udpId + "/" + seq + "]" + content + "[by udp]");
//			return;
//		}
//		int nextSeq = mNextSeq;
//		if (seq > nextSeq) {
//			Log.i("yangtong", TAG+"error" + (seq - nextSeq) + " logs are missing or in wrong order");
//			nextSeq = seq < Integer.MAX_VALUE ? seq + 1 : 0;
//		} else if (seq < nextSeq) {
//			Log.i("yangtong", TAG+"error" + "this log in wrong order-> nextSeq:" + nextSeq);
//		} else {
//			nextSeq = seq < Integer.MAX_VALUE ? seq + 1 : 0;
//		}
//		mNextSeq = nextSeq;
//		Log.i("yangtong", TAG+"sendLog [" + udpId + "/" + seq + "]" + content + "[by udp]");
//	}

//	/**
//	 *	按顺序依次到达
//	 * @return
//	 */
//	public int sendNoLostInvoke(int invokeMethod, byte[] data) {
//		mHandlerSendInvoke.post(new Runnable1<UdpData>(new UdpData(mUdpId, UdpData.INVOKE_SYNC, invokeMethod, data)) {
//			@Override
//			public void run() {
//				mListInvokes.add(mP1);
//				procQueue();
//			}
//		});
//		return 0;
//	}


	public int sendInvoke(int invokeMethod, byte[] data) {
		return sendInvoke("",invokeMethod,data);
	}

	/**
	 * 
	 * @param packageName
	 *            预留，暂时没用，目前只支持C端直接发送到Core
	 * @param invokeMethod
	 * @param data
	 * @return
	 */
	public int sendInvoke(String packageName, int invokeMethod, byte[] data) {
		mHandlerSendInvoke.post(new Runnable1<UdpData>(new UdpData(mUdpId, UdpData.INVOKE_ASYNC, invokeMethod, data)) {
			@Override
			public void run() {
				mListInvokes.add(mP1);
				procQueue();
			}
		});
		return 0;
	}


	public ServiceData sendInvokeSync(int invokeMethod, byte[] data) {
		return sendInvokeSync("", invokeMethod, data);
	}

	/**
	 * 同步调用接口,注意线程，不要在主线程执行
	 * 
	 * @param invokeMethod
	 * @param data
	 * @return
	 */
	public ServiceData sendInvokeSync(String packageName, int invokeMethod, byte[] data) {
		UdpData udpData = mClient.sendInvoke(new UdpData(mUdpId, UdpData.INVOKE_SYNC, invokeMethod, data), mServerAddr);
		return new ServiceData(udpData.data);
	}
	
	/**
	 * 
	 * @param packageName
	 * @param command
	 * @param data
	 * @return
	 */
	public byte[] onInvoke(String packageName, String command, byte[] data) {
		if("comm.udp.initInfo".equals(command)){
			JSONBuilder jsonBuilder = new JSONBuilder(data);
			mServerAddr = new UdpAddress("255.255.255.255", jsonBuilder.getVal("port", Integer.class));
			mUdpId = jsonBuilder.getVal("udpId", Integer.class);
			// int logSeq = jsonBuilder.getVal("logSeq", Integer.class);
			// LogUtil.setUdpSeq(logSeq);
			LogUtil.logd(TAG + "processName:" + mProcessName + ",udpId:" + mUdpId);
			return null;
		}
		return null;
	}
	
	public static class ServiceData {
		byte[] mData;

		ServiceData(byte[] data) {
			mData = data;
		}

		public String getString() {
			try {
				return new String(mData);
			} catch (Exception e) {
				return null;
			}
		}

		public byte[] getBytes() {
			return mData;
		}

		public Integer getInt() {
			try {
				return Integer.parseInt(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}

		public Long getLong() {
			try {
				return Long.parseLong(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}

		public Double getDouble() {
			try {
				return Double.parseDouble(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}

		public Boolean getBoolean() {
			try {
				return Boolean.parseBoolean(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}

		public JSONObject getJSONObject() {
			try {
				return new JSONObject(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}

		public JSONArray getJSONArray() {
			try {
				return new JSONArray(new String(mData));
			} catch (Exception e) {
				return null;
			}
		}
	}


	private boolean checkPortFile() {
		Log.i("yangtong", "udp check port file");
		File file = new File(FILE_PORT);
		if (file.exists()) {
			try {
				FileInputStream in = new FileInputStream(file);
				byte[] bs = new byte[(int) file.length()];
				int t = 0;
				while (t < bs.length) {
					int r = in.read(bs, t, bs.length - t);
					if (r < 0)
						break;
					t += r;
				}
				in.close();
				String info = new String(bs);
				if(TextUtils.isEmpty(info)){
					return false;
				}
				String[] tmp1 = info.split("=");
				if (tmp1.length != 2) {
					return false;
				}
				String[] tmp2 = tmp1[1].split(":");
				mServerAddr = new UdpAddress(StringUtils.replaceBlank(tmp2[0]),
						Integer.parseInt(StringUtils.replaceBlank(tmp2[1])));
//				mMapAddress.put(tmp1[0].trim(), address);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mIsInConnection = false;
		return false;
	}

	private boolean checkConnection() {
		Log.i("yangtong", "udp check connection");
		if (mClient == null) {
			mClient = new UdpClient();
			mClient.init();
		}
		if (mServerAddr == null) {
			return false;
		}
		UdpData resp = null;
		try {
			resp = mClient.sendInvoke(new UdpData(mUdpId,UdpData.INVOKE_SYNC, UdpData.CMD_CHECK_CONNECTION, 
					mProcessName.getBytes()),mServerAddr);
		} catch (Exception e) {
			e.printStackTrace();
			mIsInConnection = false;
			return false;
		}
		if (resp != null) {
			try {
				JSONBuilder jsonBuilder = new JSONBuilder(resp.data);
				int udpId = jsonBuilder.getVal("udpId", Integer.class);
				if (mIsInConnection == false || mUdpId == 0 || udpId != mUdpId) {
//					int logSeq = jsonBuilder.getVal("logSeq", Integer.class);
//					LogUtil.setUdpSeq(logSeq);
					mUdpId = udpId;
					Log.i("yangtong", "udp in connection udpId:" + mUdpId);
					mIsInConnection = true;
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mIsInConnection = false;
		return false;
	}



}
