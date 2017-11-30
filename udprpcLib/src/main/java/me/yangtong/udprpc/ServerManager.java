package me.yangtong.udprpc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.yangtong.udprpc.base.UdpConfiger;
import me.yangtong.udprpc.base.UdpIdManager;
import me.yangtong.udprpc.server.UdpServer;
import me.yangtong.udprpc.util.CommUtil;
import me.yangtong.udprpc.util.JSONBuilder;
import me.yangtong.udprpc.util.LogUtil;

/**
 * Udp server manager
 */
public class ServerManager {

    private ServerManager() {
    }

    private static ServerManager sInstance = new ServerManager();
    private static final String TAG = "ServerManager ";

    private UdpServer mServer;
    private Context mContext;
    private int mPort;

    public static ServerManager getInstance() {
        return sInstance;
    }

    /**
     * init server
     *
     * @param dispatcher the dispatcher to process client invoke
     * @param context    use application context to avoid memory leak
     */
    public void init(Context context, UdpServer.ICmdDispatcher dispatcher) {
        if (context == null) {
            throw new NullPointerException("context can't be null!");
        }
        mContext = context;
        mServer = new UdpServer();
        mServer.setCmdDispatcher(dispatcher);
        mPort = mServer.start();
        mContext.registerReceiver(mReceiverClient, new IntentFilter(UdpConfiger.ACTION_CLIENT_INIT));
        if (mPort > 0) {
            broadcastPortInfo(UdpConfiger.HOST_SERVER, mPort);
        } else {
            LogUtil.loge("udp server init error!");
        }
    }

    private BroadcastReceiver mReceiverClient = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.logi("onReceive:" + intent.getAction());
            if (UdpConfiger.ACTION_CLIENT_INIT.equals(intent.getAction())) {
                if (mPort > 0) {
                    broadcastPortInfo(UdpConfiger.HOST_SERVER, mPort);
                }
            }
        }
    };


    private void broadcastPortInfo(String hostName, int port) {
        String processName = CommUtil.getProcessName(mContext);
        String portInfo = processName + " = " + hostName + ":" + port;
        LogUtil.logi("broadcastPortInfo :" + portInfo);
        Intent intent = new Intent();
        intent.setAction(UdpConfiger.ACTION_HOST_PORT);
        intent.putExtra(UdpConfiger.EXTRA_PORT_INFO, portInfo);
        mContext.sendBroadcast(intent);
    }

    public void setCmdDispatcher(UdpServer.ICmdDispatcher cmdDispatcher) {
        mServer.setCmdDispatcher(cmdDispatcher);
    }

    /**
     *  use broadcast to notify port info instead {@link #broadcastPortInfo(String, int)}
     * @param hostName
     * @param port
     */
    @Deprecated
    private void writePortFile(String hostName, int port) {
        File file = new File(UdpConfiger.FILE_PORT);
        if (file.exists()) {
            file.delete();
        }
        String content = "server = " + hostName + ":" + port + "\n";
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(content.getBytes(), 0, content.getBytes().length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
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
