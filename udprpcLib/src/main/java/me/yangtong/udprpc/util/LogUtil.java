package me.yangtong.udprpc.util;

import android.util.Log;

/**
 * Created by yangtong on 2017/8/23.
 */

public class LogUtil {
    private LogUtil(){}

    private static final String TAG = "yangtong";
    public static void logd(String log){
        Log.d(TAG,log);
    }
    public static void logi(String log){
        Log.i(TAG,log);
    }
    public static void logw(String log){
        Log.w(TAG,log);
    }
    public static void loge(String log){
        Log.e(TAG,log);
    }
}
