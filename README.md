# AndroidRpcByUDP
RPC Impl By Udp in Android(Android基于UDP的进程通信)

### Features
- Support mutil-clients and mutil-server exist in same time,but every client can only connect to one server.
One serve can have mutil-clients.
- Both support synchronous and asynchronous invoke. 
- Very minor performance cost.
- Aginst AIDL:1.more easy to use;2.more efficient,less performance cost;
- Aginst Broadcast:1.support sunchronous invoke;2.more efficient,less performance cost;
- In normal use stituation,will extremely rare lose data or in worng order(near nerver).
 
### Useage
1. Add the udprpcLib src file to your project,or download that module and add it to your project.<br><br>
2. Initialize Server and Client.<br> 
  In the server process,use ServerManager.getInstance().init(context,cmdDispatcher) to initialize your udp server.<br>
 In the client process,use ClientManager.getInstance().init(context,serverProcessName) to initialize your udp client.<br>
Ps:suggest do initialize work when application onCreate and use application context.<br><br>
3. Now you can send your data from client process and receive it in server process.<br><br>
 For example:<br>
The client send async invoke
```
ClientManager.getInstance().sendInvoke(UdpDataFactory.UdpData.CMD_TEST, "testData".getBytes());
```
The server receive and handle the receive data<br> 
```
 UdpServer.ICmdDispatcher mTestCmdDispatcher = new UdpServer.ICmdDispatcher() {
        @Override
        public UdpDataFactory.UdpData onInvoke(UdpDataFactory.UdpData udpData) {
            switch (udpData.cmd){
                case UdpDataFactory.UdpData.CMD_TEST:
                    mTvReceive.post(new Runnable1<byte[]>(udpData.data) {
                        @Override
                        public void run() {
                            if (udpData.data != null && udpData.length > 0) {
                                mTvReceive.setText(new String(udpData.data));
                            }
                        }
                    });
                    break;
            }
            return null;
        }
    };
```
the mTestCmdDispatcher is appointed when server initialize.

<br>
You can read the TestActivity to learn the useage.

### Contact Me
If you have any other questions,connect me:
[haoyunyangtong@qq.com](Mailto:haoyunyangtong@qq.com "haoyunyangtong@qq.com")


