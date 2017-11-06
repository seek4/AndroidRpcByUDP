package me.yangtong.udprpc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import me.yangtong.udprpc.util.LogUtil;


/**
 * Created by Terry on 2017/10/11.
 */
public class UdpClient {
    private static final String TAG = "UdpClient ";

    private DatagramSocket mSocketClient;
    private int mPort = UdpConfiger.PORT_CLIENT_DEFAULT;

    public int init() {
        if (mSocketClient == null) {
            int defaultPort = UdpConfiger.PORT_CLIENT_DEFAULT;
            mPort = defaultPort;
            while (true) {
                try {
                    mSocketClient = new DatagramSocket(mPort);
                    mSocketClient.setSoTimeout(UdpConfiger.TIME_OUT_CLIENT_RECV);
                    mSocketClient.setSendBufferSize(1024 * 1024);
                    LogUtil.logd(TAG + " sendBuffer:" + mSocketClient.getSendBufferSize());
                    return mPort;
                } catch (SocketException e) {
                    e.printStackTrace();
                    mPort++;
                    if (mPort - defaultPort > 20) {
                        return -2;
                    }
                }
            }
        }
        return mPort;
    }


	public UdpDataFactory.UdpData sendInvoke(UdpDataFactory.UdpData udpData, UdpConfiger.UdpAddress targetAddr) {
		if (udpData.invokeType == UdpDataFactory.UdpData.INVOKE_SYNC) {
			return sendInvokeSync(UdpDataFactory.getTransferData(udpData), targetAddr);
		} else {
			return sendInvoke(UdpDataFactory.getTransferData(udpData), targetAddr);
		}
	}
    

    public UdpDataFactory.UdpData sendInvokeSync(byte[] transferData, UdpConfiger.UdpAddress targetAddr) {
        InetAddress local = null;
        try {
            local = InetAddress.getByName(targetAddr.host);
            DatagramPacket dpSend = new DatagramPacket(transferData, transferData.length
                    , local, targetAddr.port);
            mSocketClient.send(dpSend);
            byte[] buffer = new byte[UdpConfiger.getInstance().getTransferLength()];
            DatagramPacket dpRecv = new DatagramPacket(buffer, buffer.length);
            mSocketClient.receive(dpRecv);
            UdpDataFactory.UdpData udpData = UdpDataFactory.getUdpData(dpRecv.getData());
            return udpData;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public UdpDataFactory.UdpData sendInvoke(final byte[] transferData, final UdpConfiger.UdpAddress targetAddr) {
//        new Thread() {
//            @Override
//            public void run() {
        try {
            InetAddress local = InetAddress.getByName(targetAddr.host);
            DatagramPacket dpSend = new DatagramPacket(transferData, transferData.length
                    , local, targetAddr.port);
            mSocketClient.send(dpSend);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//            }
//        }.start();
        return null;
    }
}
