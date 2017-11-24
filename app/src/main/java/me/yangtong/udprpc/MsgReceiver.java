package me.yangtong.udprpc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.yangtong.udprpc.base.UdpConfiger;
import me.yangtong.udprpc.base.UdpIdManager;
import me.yangtong.udprpc.server.UdpServer;
import me.yangtong.udprpc.util.JSONBuilder;

/**
 * Udp server manager
 */
public class MsgReceiver {

	private MsgReceiver(){}
	
	private static MsgReceiver sInstance = new MsgReceiver();
	private static final String TAG = "MsgReceiver ";
	
	private UdpServer mServer;

	public static MsgReceiver getInstance(){
		return sInstance;
	}

	public void init(UdpServer.ICmdDispatcher dispatcher) {
		mServer = new UdpServer();
		mServer.setCmdDispatcher(dispatcher);
		int port = mServer.start();
		if (port > 0) {
			writePortFile("127.0.0.1", port);
		}
	}

	public void setCmdDispatcher(UdpServer.ICmdDispatcher cmdDispatcher) {
		mServer.setCmdDispatcher(cmdDispatcher);
	}
	
	private void writePortFile(String hostName, int port) {
		File file = new File(UdpConfiger.FILE_PORT);
		if (file.exists()) {
			file.delete();
		}
		String content = "com.txznet.txz = " + hostName + ":" + port + "\n";
		FileOutputStream fos = null;
		try {
			file.createNewFile();
			fos = new FileOutputStream(file);
			fos.write(content.getBytes(), 0, content.getBytes().length);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(fos!=null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public byte[] getInitData(String processName) {
		JSONBuilder jsonBuilder = new JSONBuilder();
		jsonBuilder.put("port", mServer.getPort());
		int udpId = UdpIdManager.getInstance().getUdpId(processName);
		jsonBuilder.put("udpId", udpId);
		return jsonBuilder.toBytes();
	}

	
}
