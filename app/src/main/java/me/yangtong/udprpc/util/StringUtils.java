package me.yangtong.udprpc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	public static boolean isEmpty(String value) {
		return null == value || value.isEmpty();
	}

	public static boolean isNotEmpty(String value) {
		return !isEmpty(value);
	}

	/**
	 * 替换掉字符串中的换行、空格、制表符等
	 * @param str
	 * @return
	 */
    public static String replaceBlank(String str) {  
        String dest = "";  
        if (str!=null) {  
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");  
            Matcher m = p.matcher(str);  
            dest = m.replaceAll("");  
        }  
        return dest;  
    }  
	
	public static String getSource(int sid) {
		switch (sid) {
		case 0:
			return "本地音乐";
		case 1:
			return "考拉";
		case 2:
			return "QQ";
		default:
			return "未知来源";
		}
	}

	public static String toString(String[] val) {
		StringBuffer sb = new StringBuffer();
		if (null != val && val.length > 0) {

			for (int i = 0; i < val.length; i++) {
				if (i != 0) {
					sb.append(",");
				}
				sb.append(val[i]);
			}
		}

		return sb.toString();
	}

}
