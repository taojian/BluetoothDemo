package com.cld.bluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cld.bluetooth.BluetoothDelegateAdapter;
import com.cld.bluetooth.BluetoothDelegateAdapter.BTEventListener;
import com.cld.bluetooth.CldBluetoothDevice;
import com.cld.bluetoothdemo.ServiceBinder.BluetoothAdapterListener;
import java.util.ArrayList;
import java.util.Iterator;


public class MainActivity extends Activity implements OnClickListener, BTEventListener, OnItemClickListener, BluetoothAdapterListener{

    private static final String TAG = "CLDLOGTAG";
    private Button btnOpen;
    private Button btnClose;
    private Button btnScan;
    private Button btnStop;
    private ListView lvDevice;
    private BluetoothDelegateAdapter mDelegateAdapter;
    private ArrayList<CldBluetoothDevice> deviceList = new ArrayList<>();
    private boolean isDiscoveryFinished = false;
    private ServiceBinder manager;
    private Context mContext;

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

        mContext = this;

        manager = new ServiceBinder(this);
        manager.doBindService();
        manager.registerBluetoothAdapterListener(this);

        lvDevice.setAdapter(mListAdapter);
        lvDevice.setOnItemClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mListAdapter.notifyDataSetChanged();
        if(manager == null){
            manager = new ServiceBinder(this);
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
                    clearDevices();
                    mListAdapter.notifyDataSetChanged();
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

        CldBluetoothDevice device = (CldBluetoothDevice) mListAdapter.getItem(position);
        Log.i(TAG, "---tj-----原先地址-----" + device.toString());
        ServiceBinder.setCurrentDevice(device);
        Intent intent = new Intent(this, ComActivity.class);
        this.startActivity(intent);
    }

    private BaseAdapter mListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CldBluetoothDevice device = deviceList.get(position);
            String name = device.getDeviceName();
            String addr = device.getDeviceAddress();
            LayoutInflater inflater = (LayoutInflater)mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (null == deviceList || position < 0 || position >= deviceList.size()) {
                return null;
            }

            View view = null;
            if (null != convertView) {
                view = convertView;
            }
            if (null == view) {
                view = inflater.inflate(R.layout.device_list, parent, false);
            }

            if (null != view && (view instanceof LinearLayout)) {
                TextView nameView = (TextView) view.findViewById(R.id.name);
                TextView addressView = (TextView) view
                        .findViewById(R.id.address);
                if ((name != null) && (name.length() > 0)) {
                    nameView.setText(name);
                } else {
                    nameView.setText("unknown");
                }
                if (device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    nameView.setTextColor(Color.BLACK);
                } else {
                    nameView.setTextColor(Color.BLUE);
                }
                addressView.setText(addr);
                if (device.isConnected()) {
                    view.setBackgroundColor(Color.argb(0xFF, 0x33, 0xB5, 0xE5));
                } else {
                    view.setBackgroundColor(Color.argb(0x0A, 0x0A, 0x0A, 0x0A));
                }
            }
            return view;

        }

    };

    private void clearDevices() {
        if (deviceList != null) {
            ArrayList<CldBluetoothDevice> newList = new ArrayList();
            Iterator<CldBluetoothDevice> it = deviceList.iterator();
            while (it.hasNext()) {
                CldBluetoothDevice d = it.next();
                if (d != null && d.isConnected()) {
                    newList.add(d);
                }
            }
            if (newList != null) {
                synchronized (deviceList) {
                    deviceList = newList;
                }
            }
        }
    }

    @Override
    public void onDiscoveryFinished() {
        isDiscoveryFinished = true;
    }

    @Override
    protected void onDestroy() {
        manager.doUnbindService();
        if(this.mDelegateAdapter != null) {
            this.mDelegateAdapter.unregisterEventListeners(this);
        }
        manager.unregisterBluetoothAdapterListener(this);
        super.onDestroy();
    }

    @Override
    public void onDeviceFound(CldBluetoothDevice device) {
        if(!deviceList.contains(device)){
            deviceList.add(device);
        }
        mListAdapter.notifyDataSetChanged();
        Log.i(TAG, "---tj---onDeviceFound---" + device.getDeviceName() + "----" + device.getDeviceAddress());
    }

    @Override
    public void onDeviceConnected(CldBluetoothDevice device) {
        if (!deviceExisted(device)) {
            synchronized (deviceList) {
                deviceList.add(device);
            }
        }
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDeviceConnectFailed(CldBluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(CldBluetoothDevice device) {
        mListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onWriteFailed(CldBluetoothDevice var1, String var2) {

    }

    @Override
    public void onLeServiceDiscovered(CldBluetoothDevice var1, String var2) {

    }

    @Override
    public void onBluetoothAdapterListenerCreated(BluetoothDelegateAdapter adapter) {
        this.mDelegateAdapter = adapter;
        this.mDelegateAdapter.registerEventListeners(this);
    }

    @Override
    public void onBluetoothAdapterListenerDestroy() {
        if(this.mDelegateAdapter!= null) {
            this.mDelegateAdapter.unregisterEventListeners(this);
        }
    }


    private boolean deviceExisted(CldBluetoothDevice device) {
        if (device == null)
            return false;

        Iterator<CldBluetoothDevice> it = deviceList.iterator();
        while (it.hasNext()) {
            CldBluetoothDevice d = it.next();
            if (d != null && d.equals(device)) {
                if(d != device){
                    Log.i(TAG, "---tj----对象地址不一样---"+d.toString()+"----"+device.toString());
                }
                return true;
            }
        }
        return false;
    }
}
