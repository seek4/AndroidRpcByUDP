package me.yangtong.udprpc;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import me.yangtong.udprpc.util.LogUtil;


/**
 * Created by Terry on 2017/10/11.
 */

public class UdpServer {
	
	private static final String TAG = "UDP_SERVER ";

    private DatagramSocket mServerSocket;
    private DatagramPacket mDatagramPacket;

    private Thread mThreadServer;
    private int mPort;

    private static ICmdDispatcher mCmdDispatcher;


    public UdpServer() {
    }

	public int getPort() {
		return mPort;
	}
    
	public void stop() {
		if (mServerSocket != null) {
			mServerSocket.close();
		}
		if(mThreadServer!=null){
			mThreadServer.stop();
		}
	}

	public interface ICmdDispatcher{
		UdpDataFactory.UdpData onInvoke(UdpDataFactory.UdpData udpData);
	}
	
	
	public static void setCmdDispatcher(ICmdDispatcher d) {
		LogUtil.logd(TAG + "setCmdDispatcher:" + d);
		mCmdDispatcher = d;
	}
	
    public int start() {
        try {
            if (mServerSocket == null) {
                int defaultPort = UdpConfiger.getInstance().getServerPort();
                int port = defaultPort;
                while (true) {
                    try {
                        InetAddress addr = InetAddress.getByName(UdpConfiger.HOST_SERVER);
                        mServerSocket = new DatagramSocket(port,addr);
                        mServerSocket.setReceiveBufferSize(1024*1024);
                        mPort = port;
                        break;
                    } catch (BindException e) {
                        e.printStackTrace();
                        port++;
                        if (port - defaultPort > 20) {
                            return -2;
                        }
                    } catch (SecurityException e) {
                        LogUtil.loge("need network permission");
                        return -1;
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                        return -3;
                    }
                }
            }
            mServerSocket.setReuseAddress(true);
            if (mThreadServer != null) {
                mThreadServer.stop();
            }
            mThreadServer = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
							byte[] receiveData = new byte[UdpConfiger.getInstance().getTransferLength()];
                            mDatagramPacket = new DatagramPacket(receiveData,receiveData.length);
                            mServerSocket.receive(mDatagramPacket);
                            byte[] transferData = mDatagramPacket.getData();
                            if (transferData != null && transferData.length > UdpConfiger.getInstance().getReserveDataLength()) {
								UdpDataFactory.UdpData udpData = UdpDataFactory.getUdpData(transferData);
                                if (mCmdDispatcher == null) {
									continue;
								}
								if (udpData.invokeType == UdpDataFactory.UdpData.INVOKE_SYNC) {
									InetAddress clientAddr = mDatagramPacket.getAddress();
									UdpDataFactory.UdpData response = mCmdDispatcher.onInvoke(udpData);
									byte[] dataResp = UdpDataFactory.getTransferData(response);
									mServerSocket.send(new DatagramPacket(dataResp, dataResp.length, clientAddr,
											mDatagramPacket.getPort()));
								} else {
									mCmdDispatcher.onInvoke(udpData);
								}
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            mThreadServer.start();
            return mPort;
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return -3;
    }

}
