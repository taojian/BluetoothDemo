package com.cld.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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
import com.cld.bluetoothdemo.DemoManager.BluetoothAdapterListener;
import java.util.ArrayList;


public class MainActivity extends Activity implements OnClickListener, BTEventListener, OnItemClickListener, BluetoothAdapterListener{

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
    private DemoManager manager;

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

        manager = new DemoManager(this);
        manager.doBindService();
        manager.registerBluetoothAdapterListener(this);

        mAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayAdapter);
        lvDevice.setAdapter(mAdapter);
        lvDevice.setOnItemClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(manager == null){
            manager = new DemoManager(this);
            manager.doBindService();
            manager.registerBluetoothAdapterListener(this);
        }
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
                if(isDiscoveryFinished) {
                    mAdapter.clear();
                    deviceList.clear();
                }
                isDiscoveryFinished = false;
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "------");
        Intent intent = new Intent(this, ComActivity.class);
        intent.putExtra("DEVICE", deviceList.get(position));
        this.startActivity(intent);
    }

    @Override
    public void onDiscoveryFinished() {
        isDiscoveryFinished = true;
    }

    @Override
    protected void onDestroy() {
        manager.doUnbindService();
        manager.unregisterBluetoothAdapterListener(this);
        super.onDestroy();
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
    public void onBluetoothAdapterListenerCreated(BluetoothDelegateAdapter adapter) {
        this.mDelegateAdapter = adapter;
        this.mDelegateAdapter.registerEventListeners(this);
    }

    @Override
    public void onBluetoothAdapterListenerDestroy() {

    }
}
