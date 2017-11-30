package me.yangtong.udprpc;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import me.yangtong.udprpc.base.UdpDataFactory;
import me.yangtong.udprpc.server.UdpServer;
import me.yangtong.udprpc.util.Runnable1;

public class TestActivity extends Activity {

    private EditText mEditMsg;
    private Button mBtnSend;
    private TextView mTvReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // init server,should be invoked in server process
        initServer();
        // init client,should be invoked in client process
        initClient();
        mEditMsg = (EditText) findViewById(R.id.edit_msg);
        mBtnSend = (Button) findViewById(R.id.btn_send);
        mTvReceive = (TextView) findViewById(R.id.tv_receive);

        mBtnSend.setOnClickListener((View view) -> {
            String msgSend = "" + mEditMsg.getText();
            if (!TextUtils.isEmpty(msgSend)) {
                ClientManager.getInstance().sendInvoke(UdpDataFactory.UdpData.CMD_TEST, msgSend.getBytes());
            }
        });
    }

    private void initClient() {
        ClientManager.getInstance().init(this.getApplicationContext(),"me.yangtong.udprpc");
    }

    private void initServer() {
        ServerManager.getInstance().init(this.getApplicationContext(),mTestCmdDispatcher);
    }

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

}
