package com.cld.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

import com.cld.bluetooth.BluetoothDelegateAdapter;
import com.cld.bluetooth.BluetoothDelegateAdapter.DataReceiver;
import com.cld.bluetooth.CldBluetoothDevice;


public class ComActivity extends Activity implements OnClickListener, DataReceiver{

    private static final String TAG = "ComActivity";
    private BluetoothDelegateAdapter mAdapter = DemoManager.getDelegateAdapter();
    private CldBluetoothDevice device;
    private Button btnCon;
    private Button btnDiscon;
    private Button btnSend;
    private Button btnClear;
    private EditText editData;
    private TextView tvDataShow;
    private MyHandler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com);
        setUp(this);
        Intent mainActivity = this.getIntent();
        device = mainActivity.getParcelableExtra("DEVICE");
        mAdapter.registerDataReceivers(this);
        mHandler = new MyHandler();
    }

    private void setUp(Context context){
        btnCon = (Button)this.findViewById(R.id.btn_con);
        btnCon.setOnClickListener(this);
        btnDiscon = (Button)this.findViewById(R.id.btn_discon);
        btnDiscon.setOnClickListener(this);
        btnSend = (Button)this.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(this);
        btnClear = (Button)this.findViewById(R.id.btn_clear);
        btnClear.setOnClickListener(this);
        editData = (EditText)this.findViewById(R.id.et_data);
        tvDataShow = (TextView)this.findViewById(R.id.tv_receiveData);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_con:
                mAdapter.connectDevice(this.device);
                break;
            case R.id.btn_discon:
                mAdapter.disconnectDevice(this.device);
                break;
            case R.id.btn_send:
                String data = editData.getText().toString().trim();
                byte[] buffer = data.getBytes();
                mAdapter.send(device, buffer, buffer.length);
                break;
            case R.id.btn_clear:
                tvDataShow.setText("");
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        mAdapter.unregisterDataReceivers(this);
        super.onDestroy();
    }

    @Override
    public void onDataReceive(CldBluetoothDevice device, byte[] data, int length) {
        String receiveData = new String(data, 0, length);
        tvDataShow.setText(receiveData);
    }

    private class MyHandler extends Handler{
        public static final int MSG_RECEIVED_SPEED = 0x01;
        public static final int MSG_RECEIVED_STRING = 0x02;
        public static final int MESSAGE_CLEAR = 0x03;
        public static final int MSG_AUTO_WRITE_STARTED = 0x04;
        public static final int MSG_AUTO_WRITE_SPEED = 0x05;
        public static final int MSG_AUTO_WRITE_COMPLETED = 0x06;
        public static final int MSG_AUTO_CONNECT_STARTED = 0x07;
        public static final int MSG_AUTO_CONNECT = 0x08;
        public static final int MSG_AUTO_CONNECT_COMPLETED = 0x09;

        MyHandler(){
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case MSG_RECEIVED_STRING:
                    break;
                case MSG_AUTO_CONNECT_STARTED:
                    break;
                default:
                    break;

            }
        }
    }
}
