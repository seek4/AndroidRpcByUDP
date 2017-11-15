package me.yangtong.udprpc.base;

import android.text.TextUtils;

import java.util.HashMap;

import me.yangtong.udprpc.util.LogUtil;

/**
 * UdpId生成类。该类由server进程运行并维护，<p>
 * client初始化时请求并分配该ID，该ID根据processName来决定，<p>
 * 并且相同processName不同时刻 的ID都相同
 * 
 * @author Terry
 */
public class UdpIdManager {

	private UdpIdManager() {
	}

	private HashMap<String, Integer> mMapProcessIds = new HashMap<String, Integer>();
	
	/**
	 * C端可用id从2开始，1:Core,0:id未获取
	 */
	private int mCurIdPoint = 2;
	
	private static UdpIdManager sInstance = new UdpIdManager();

	public static UdpIdManager getInstance() {
		return sInstance;
	}

	public synchronized int getUdpId(String processName) {
		LogUtil.logd("UdpId processName:" + processName);
		if(TextUtils.isEmpty(processName)){
			throw new NullPointerException("processName can't be null");
		}
		if(processName.equals("")){
			return 1;
		}
		if (mMapProcessIds.get(processName) != null) {
			LogUtil.logd("UdpId processName:" + processName + ",id:" + mMapProcessIds.get(processName));
			return mMapProcessIds.get(processName);
		}
		LogUtil.logd("UdpId processName:" + processName + ",id:" + mCurIdPoint);
		mMapProcessIds.put(processName, mCurIdPoint++);
		return mMapProcessIds.get(processName);
	}
}
