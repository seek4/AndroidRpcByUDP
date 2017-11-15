package me.yangtong.udprpc.server;


public abstract class BaseUdpInvoker {
	public abstract UdpDataFactory.UdpData onInvoke(UdpDataFactory.UdpData udpData);
}
