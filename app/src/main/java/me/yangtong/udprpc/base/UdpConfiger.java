package me.yangtong.udprpc.base;


import android.os.Environment;

/**
 * 传输的端口号等配置文件保存
 *
 * Created by Terry on 2017/10/12.
 */

public class UdpConfiger {

    private static UdpConfiger sInstance = new UdpConfiger();

    private UdpConfiger(){}

    public static UdpConfiger getInstance(){
        return sInstance;
    }


    public static final String HOST_SERVER = "127.0.0.1";

    /**
     * C端接收数据超时时间
     */
    public static final int TIME_OUT_CLIENT_RECV = 3000;

    public static final int PORT_SERVER_DEFAULT = 10000;
    public static final int PORT_CLIENT_DEFAULT = 20000;
    public static final int PORT_CLIENT_MUSIC_DEFAULT = 20100;
    public static final int PORT_CLIENT_WECHAT_DEFAULT = 20200;
    
    /**
     * 端口文件
     */
    public static final String FILE_PORT = Environment.getExternalStorageDirectory()+"/udp_port.txz";

    private Integer mPortServer;
    
    /**
     * UDP传输最大长度最大传输长度
     */
    public static final int LENGTH_MAX_TRANSFER = 1024*256;
    /**
     * 24位功能预留位
     */
    public static final int LENGTH_RESERVE = 24;
    /**
     * 有效数据数据最大长度
     */
    public static final int LENGTH_MAX_USER = LENGTH_MAX_TRANSFER - LENGTH_RESERVE;

    /**
     *
     * @return userData 最大数据长度
     */
    public int getUserDataLength(){
        return LENGTH_MAX_USER;
    }

    /**
     * 
     * @return 封装后的传输长度
     */
    public int getTransferLength(){
    	return LENGTH_MAX_TRANSFER;
    }
    
    /**
     *
     * @return reserveData 长度
     */
    public int getReserveDataLength(){
        return LENGTH_RESERVE;
	}

	/**
	 * 得到服务器端口号(也就是Core端口号)
	 * 
	 * @return
	 */
	public int getServerPort() {
		return mPortServer == null ? PORT_SERVER_DEFAULT : mPortServer;
	}


    public int getClientPort(String packageName) {
        return PORT_CLIENT_DEFAULT;
    }

	public static class UdpAddress {
		public String host;
		public int port;
		public UdpAddress(String host,int port){
			this.host = host;
			this.port = port;
		}
	}
	
	
}
