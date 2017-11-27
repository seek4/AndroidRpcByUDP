package me.yangtong.udprpc.server;


import me.yangtong.udprpc.base.UdpDataFactory;

public abstract class BaseUdpInvoker {
	public abstract UdpDataFactory.UdpData onInvoke(UdpDataFactory.UdpData udpData);
}
