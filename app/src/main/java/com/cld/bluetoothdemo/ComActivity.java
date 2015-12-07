package com.cld.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.cld.bluetooth.BluetoothDelegateAdapter;


public class ComActivity extends Activity implements OnClickListener{

    private static final String TAG = "ComActivity";
    private BluetoothDelegateAdapter mAdapter = DemoManager.getDelegateAdapter();
    private BluetoothDevice device;
    private Button btnCon;
    private Button btnDiscon;
    private Button btnSend;
    private Button btnClear;
    private EditText editData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com);
        setUp(this);
        Intent mainActivity = this.getIntent();
        device = mainActivity.getParcelableExtra("DEVICE");
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
                break;
            default:
                break;
        }
    }
}
