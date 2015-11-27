package com.cld.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.cld.bluetooth.BluetoothDelegateAdapter;
import com.cld.bluetooth.BluetoothDelegateAdapter.BTEventListener;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements OnClickListener, BTEventListener, OnItemClickListener {

    private static final String TAG = "CLDLOGTAG";
    private Button btnOpen;
    private Button btnClose;
    private Button btnScan;
    private Button btnStop;
    private ListView lvDevice;
    private BluetoothDelegateAdapter mDelegateAdapter;
    private ArrayAdapter mAdapter;
    private ArrayList<String> arrayAdapter = new ArrayList<>();
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private boolean isDiscoveryFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnOpen = (Button) this.findViewById(R.id.bt_open);
        btnOpen.setOnClickListener(this);
        btnClose = (Button) this.findViewById(R.id.bt_close);
        btnClose.setOnClickListener(this);
        btnScan = (Button)this.findViewById(R.id.bt_scan);
        btnScan.setOnClickListener(this);
        btnStop = (Button)this.findViewById(R.id.bt_stop);
        btnStop.setOnClickListener(this);
        lvDevice = (ListView)this.findViewById(R.id.lv_device);
        mDelegateAdapter = new BluetoothDelegateAdapter(this);
        mDelegateAdapter.registerEventListeners(this);
        mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayAdapter);
        lvDevice.setAdapter(mAdapter);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_open:
                mDelegateAdapter.setEnabled(true);
                break;
            case R.id.bt_close:
                mDelegateAdapter.setEnabled(false);
                break;
            case R.id.bt_scan:
                isDiscoveryFinished = false;
                if(isDiscoveryFinished) {
                    mAdapter.clear();
                }
                mDelegateAdapter.startDiscovery();
                break;
            case R.id.bt_stop:
                isDiscoveryFinished = true;
                mDelegateAdapter.stopDiscovery();
                break;
            default:
                break;
        }
    }


    @Override
    public void onDiscoveryFinished() {
        isDiscoveryFinished = true;
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {
        arrayAdapter.add(device.getName() + "\n" + device.getAddress());
        deviceList.add(device);
        mAdapter.notifyDataSetChanged();
        Log.i(TAG, "---tj---onDeviceFound---" + device.getName() + "----" + device.getAddress());
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnectFailed(BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
