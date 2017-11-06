package me.yangtong.udprpc.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * Created by yangtong on 2017/9/14.
 */

public class CommUtil {
    private CommUtil(){}

    public static int getMinNum(List<Integer> data){
        if(data==null||data.size()==0){
            LogUtil.loge("data can't be null!");
            throw new RuntimeException("data can't be null!");
        }
        int minNum = Integer.MAX_VALUE;
        for(int i:data){
            if(i<minNum){
                minNum = i;
            }
        }
        return minNum;
    }

    public static String getProcessName(Context context) {
        String currentProcName = "";
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager
                .getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                currentProcName = processInfo.processName;
                break;
            }
        }
        return currentProcName;
    }

}
