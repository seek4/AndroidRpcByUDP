package me.yangtong.udprpc;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.yangtong.udprpc.base.UdpConfiger;
import me.yangtong.udprpc.base.UdpConfiger.UdpAddress;
import me.yangtong.udprpc.base.UdpDataFactory.UdpData;
import me.yangtong.udprpc.client.UdpClient;
import me.yangtong.udprpc.util.CommUtil;
import me.yangtong.udprpc.util.LogUtil;
import me.yangtong.udprpc.util.Runnable1;
import me.yangtong.udprpc.util.JSONBuilder;
import me.yangtong.udprpc.util.StringUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

/**
 *	manage the client and send invoke to target server,
 *  should run in the client process.
 * @author Terry
 *
 */
public class ClientManager {

	private ClientManager(){}
	
	private static ClientManager sInstance = new ClientManager();
	
	public static ClientManager getInstance(){
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
	private UdpAddress mServerAddr;
	private static final String TAG = "ClientManager ";
	private String mProcessName = "";
	private String mSeverProcessName = null;

//	/**
//	 * init client
//	 *
//	 * @param context use application to avoid memory leak
//	 */
//	public void init(Context context) {
//		init(context,null);
//	}

	/**
	 * init client with specific server,advise use this method
	 * @param context use application to avoid memory leak
	 * @param serverProcessName 	server process name
	 */
	public void init(Context context, String serverProcessName) {
		if (context == null) {
			throw new NullPointerException("context can't be null!");
		}
		if (serverProcessName == null) {
			throw new NullPointerException("server process name can't be null!");
		}
		mProcessName = CommUtil.getProcessName(context);
		mThreadCheck = new HandlerThread("udpCheckConnection");
		mThreadCheck.start();
		mHandlerCheck = new Handler(mThreadCheck.getLooper());
		mHandlerCheck.post(mTaskCheckConnection);
		mThreadSendInvoke = new HandlerThread("udpProcess");
		mThreadSendInvoke.start();
		mHandlerSendInvoke = new Handler(mThreadSendInvoke.getLooper());
		mSeverProcessName = serverProcessName;
		IntentFilter intentFilter = new IntentFilter(UdpConfiger.ACTION_HOST_PORT);
		context.registerReceiver(mServerReceiver,intentFilter);
		context.sendBroadcast(new Intent(UdpConfiger.ACTION_CLIENT_INIT));
	}

	/**
	 * receive port info and other info from server,.
	 */
	private BroadcastReceiver mServerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (UdpConfiger.ACTION_HOST_PORT.equals(intent.getAction())) {
				// data format: processName = hostName:port
				String info = intent.getStringExtra(UdpConfiger.EXTRA_PORT_INFO);
				LogUtil.logi("receive port info :" + info);
				if (TextUtils.isEmpty(info)) {
					return;
				}
				String[] tmp1 = info.split("=");
				if (tmp1.length != 2) {
					return;
				}
				if (mSeverProcessName != null && !mSeverProcessName.equals(tmp1[0].trim())) {
					LogUtil.logw("process name not fit, return.");
					return;
				}
				String[] tmp2 = tmp1[1].split(":");
				mServerAddr = new UdpAddress(StringUtils.replaceBlank(tmp2[0]),
						Integer.parseInt(StringUtils.replaceBlank(tmp2[1])));
				// check connection immediately
				mHandlerCheck.post(mTaskCheckConnection);
			}
		}
	};


	private Runnable mTaskCheckConnection = new Runnable() {
		@Override
		public void run() {
			mHandlerCheck.removeCallbacks(mTaskCheckConnection);
//			checkPortFile();
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
			UdpData udpDataSend = mListInvokes.get(0);
			mClient.sendInvoke(udpDataSend, mServerAddr);
			mListInvokes.remove(i);
		}
	}

	/**
	 *
	 * @param cmd
	 * 			希望对端执行的cmd
	 * @param data
	 * 			发送的数据
	 * @return
	 */
	public int sendInvoke(int cmd, byte[] data) {
		mHandlerSendInvoke.post(new Runnable1<UdpData>(new UdpData(mUdpId, UdpData.INVOKE_ASYNC, cmd, data)) {
			@Override
			public void run() {
				mListInvokes.add(mP1);
				procQueue();
			}
		});
		return 0;
	}

	/**
	 * 同步调用接口,注意线程，不要在主线程执行
	 * 
	 * @param cmd
	 * @param data
	 * @return
	 */
	public ServiceData sendInvokeSync(int cmd, byte[] data) {
		UdpData udpData = mClient.sendInvoke(new UdpData(mUdpId, UdpData.INVOKE_SYNC, cmd, data), mServerAddr);
		return new ServiceData(udpData.data);
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


//	private boolean checkPortFile() {
//		Log.i("yangtong", "udp check port file");
//		File file = new File(UdpConfiger.FILE_PORT);
//		if (file.exists()) {
//			try {
//				FileInputStream in = new FileInputStream(file);
//				byte[] bs = new byte[(int) file.length()];
//				int t = 0;
//				while (t < bs.length) {
//					int r = in.read(bs, t, bs.length - t);
//					if (r < 0)
//						break;
//					t += r;
//				}
//				in.close();
//				String info = new String(bs);
//				if(TextUtils.isEmpty(info)){
//					return false;
//				}
//				String[] tmp1 = info.split("=");
//				if (tmp1.length != 2) {
//					return false;
//				}
//				String[] tmp2 = tmp1[1].split(":");
//				mServerAddr = new UdpAddress(StringUtils.replaceBlank(tmp2[0]),
//						Integer.parseInt(StringUtils.replaceBlank(tmp2[1])));
////				mMapAddress.put(tmp1[0].trim(), address);
//				return true;
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//		mIsInConnection = false;
//		return false;
//	}

	private boolean checkConnection() {
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
