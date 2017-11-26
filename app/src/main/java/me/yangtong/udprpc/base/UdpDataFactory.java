package me.yangtong.udprpc.base;

import me.yangtong.udprpc.util.LogUtil;

/**
 * 数据封装及解封装类
 * 
 * 传输数据格式:<p>
 * 前24位用于功能性保留位<p>
 * 第一位代表C端ID,根据processName生成,唯一<p>
 * 第二位用来表示是同步调用还是异步调用<p>
 * 第三位到第五位用来表示cmd类型<p>
 * 第六位到第十位用来表示data长度<p>
 * Created by Terry on 2017/10/12.
 */

public class UdpDataFactory {

    private UdpDataFactory(){
    }

	public static class UdpData {
		public int udpId = 0;
		public int invokeType = INVOKE_SYNC;
		public int cmd = CMD_NONE;
		public int length = 0;
		public byte[] data;

		public UdpData(){
		}
		
		public UdpData(int udpId,int invokeType, int cmd, byte[] data) {
			this.udpId = udpId;
			this.invokeType = invokeType;
			this.cmd = cmd;
			this.data = data;
		}
		
	    /**
	     * 同步调用
	     */
	    public static final int INVOKE_SYNC = 1;
	    /**
	     * 异步调用
	     */
	    public static final int INVOKE_ASYNC = 2;

	    /**
	     * 函数返回值等纯传输数据
	     */
	    public static final int CMD_NONE = 0;
	    /**
	     * 监测对端连通性
	     */
	    public static final int CMD_CHECK_CONNECTION = 1;
	    /**
	     * 回复{@link #CMD_CHECK_CONNECTION}
	     */
	    public static final int CMD_RESP_CONNECTION = 2;
		/**
		 * 测试
		 */
		public static final int CMD_TEST = 11;
	}

	// public static byte[] getTransferData(String msg){
	// return getTransferData(msg.getBytes());
	// }
	//
	// public static byte[] getTransferData(byte[] originalData) {
	// return getTransferData(UdpData.INVOKE_ASYNC, UdpData.CMD_NONE,
	// originalData);
	// }
	//
	// public static byte[] getTransferData(int invokeType, int cmd, String msg)
	// {
	// return getTransferData(invokeType, cmd, msg.getBytes());
	// }

	public static class ReportData{
		public int type;
		public byte[] data;
		
		public ReportData(){}
		public ReportData(int type,byte[] data){
			this.type = type;
			this.data = data;
		}
	}
	
	/**
	 * 合并上报数据
	 * @param type
	 * @param data
	 * @return
	 */
	public static byte[] combineReportData(int type,byte[] data) {
		byte[] addedData = new byte[data.length + 4];
		byte[] typeData = intToByteArray(type);
		addedData[0] = typeData[0];
		addedData[1] = typeData[1];
		addedData[2] = typeData[2];
		addedData[3] = typeData[3];
		for(int i=0;i<data.length;i++){
			addedData[i + 4] = data[i];
		}
		return addedData;
	}
	
	/**
	 * 解析分离上报数据
	 * @param data
	 * @return
	 */
	public static ReportData separateReportData(byte[] data) {
		byte[] typeData = new byte[4];
		typeData[0] = data[0];
		typeData[1] = data[1];
		typeData[2] = data[2];
		typeData[3] = data[3];
		byte[] reportData = new byte[data.length - 4];
		for (int i = 0; i < reportData.length; i++) {
			reportData[i] = data[i+4];
		}
		return new ReportData(byteArrayToInt(typeData), reportData);
	}
	
	
	public static byte[] getTransferData(UdpData udpData) {
		return getTransferData(udpData.udpId, udpData.invokeType, udpData.cmd, udpData.data);
	}

    /**
     * 根据原始数据构造出需传输的数据，跟{@link #getUdpData(byte[])} 相对应
     *
     * @param invokeType
     *             调用类型,同步还是异步 {@link #INVOKE_SYNC} {@link #INVOKE_ASYNC}
     * @param cmd
     *              对方执行哪个方法 打日志{@link #CMD_LOG} 上报数据{@link #CMD_REPORT}
     * @param originalData
     *              原始数据
     * @return
     */
    public static byte[] getTransferData(int udpId,int invokeType, int cmd, byte[] originalData) {
    	if(originalData==null){
    		originalData = new byte[0];
    	}
        byte[] bytes = new byte[originalData.length <= UdpConfiger.getInstance().getUserDataLength() ?
                originalData.length + UdpConfiger.getInstance().getReserveDataLength() : UdpConfiger.getInstance().getUserDataLength() + UdpConfiger.getInstance().getReserveDataLength()];
        bytes[0] = (byte) udpId;
        bytes[1] = invokeType == UdpData.INVOKE_SYNC ? (byte) 1 : (byte) 0;
        byte[] cmdByte = intToByteArray(cmd);
        bytes[2] = cmdByte[0];
        bytes[3] = cmdByte[1];
        bytes[4] = cmdByte[2];
        bytes[5] = cmdByte[3];
		byte[] lengthByte = intToByteArray(originalData.length);
		bytes[6] = lengthByte[0];
		bytes[7] = lengthByte[1];
		bytes[8] = lengthByte[2];
		bytes[9] = lengthByte[3];
		for (int i = 0; i < originalData.length && i < UdpConfiger.getInstance().getUserDataLength(); i++) {
			bytes[UdpConfiger.getInstance().getReserveDataLength() + i] = originalData[i];
		}
		LogUtil.logd("send:");
		printBytes(bytes);
		return bytes;
    }


    /**
     * 从传输数据解析出原始数据，跟{@link #getTransferData(int, int, byte[])}相对应
     * @param transferData
     * @return
     */
    public static UdpData getUdpData(byte[] transferData){
		if (transferData.length < UdpConfiger.getInstance().getReserveDataLength()) {
			throw new RuntimeException("transfer data can't be shorter than reserve length!");
		}
		LogUtil.logd("receive:");
		printBytes(transferData);
		UdpData udpData = new UdpData();
		udpData.udpId = transferData[0];
		udpData.invokeType = transferData[1] == 0 ? UdpData.INVOKE_ASYNC : UdpData.INVOKE_SYNC;
		byte[] cmdBytes = new byte[4];
		cmdBytes[0] = transferData[2];
		cmdBytes[1] = transferData[3];
		cmdBytes[2] = transferData[4];
		cmdBytes[3] = transferData[5];
		udpData.cmd = byteArrayToInt(cmdBytes);
		byte[] lengthBytes = new byte[4];
		lengthBytes[0] = transferData[6];
		lengthBytes[1] = transferData[7];
		lengthBytes[2] = transferData[8];
		lengthBytes[3] = transferData[9];
		udpData.length = byteArrayToInt(lengthBytes);
		byte[] bytes = new byte[udpData.length];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = transferData[UdpConfiger.getInstance().getReserveDataLength() + i];
		}
		udpData.data = bytes;
        return udpData;
    }

    public static void printBytes(byte[] bytes){
//		if (bytes == null || bytes.length == 0) {
//			LogUtil.logi("bytes null");
//			return;
//		}
//		LogUtil.logi("111111111111111111");
//		String byteStr="";
//		for (int i = 0; i < bytes.length; i++) {
//			byteStr += "" + bytes[i];
//		}
//		LogUtil.logi("22222222222222222222222");
//		LogUtil.logi("bytes:"+byteStr);
		return;
	}

	private static int byteArrayToInt(byte[] b) {
		return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
	}

	private static byte[] intToByteArray(int a) {
		return new byte[] { (byte) ((a >> 24) & 0xFF), (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
				(byte) (a & 0xFF) };
	}

}
