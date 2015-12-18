package com.cld.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import android.os.Handler;

/**
 * 蓝牙适配器类
 * 只能在主线程初始化！
 * Created by taojian on 2015/11/25.
 */
public class BluetoothDelegateAdapter {

    private static final  String TAG = "CLDLOGTAG";
    private BluetoothAdapter mAdapter = null;
    private BroadcastReceiver deviceReceiver = null;
    private BluetoothConnManager connManager;
    private Context mContext = null;
    private ArrayList<BTEventListener> mEventListeners = new ArrayList<>();
    private boolean isEnabled = false;
    private MyHandler mHandler;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;

    public static final int MSG_DEVICE_FOUND       = 1;
    public static final int MSG_DISCOVERY_FINISHED = 2;
    public static final int MSG_CONNECTED          = 4;
    public static final int MSG_CONNECT_FAILED     = 8;
    public static final int MSG_DISCONNECTED       = 16;
    public static final int MSG_STATE_CHANGED      = 32;
    public static final int MSG_WRITE_FAILED       = 64;
    public static final int MSG_LE_SERVICES_DISCOVERED = 128;


    public BluetoothDelegateAdapter(Context context){
        this.mContext = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.deviceReceiver = new DeviceBroadcastReceiver();
        this.mHandler = new MyHandler(this);
        this.connManager = new BluetoothConnManager(context, this.mHandler);
        if(this.isEnabled()){
            connManager.start();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.mContext.registerReceiver(this.deviceReceiver, filter);
    }
    public boolean isEnabled(){
        if(mAdapter != null){
            this.isEnabled = mAdapter.isEnabled();
        }
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled){
        Log.i(TAG, "---tj-----setEnabled----------"+enabled);
        if(enabled == this.isEnabled()){
            return;
        }
        if(mAdapter != null){
            if(enabled){
                mAdapter.enable();
                this.isEnabled = true;
            }else{
                mAdapter.disable();
                this.isEnabled = false;
            }
        }
    }

    public void startDiscovery(){
        if(mAdapter == null){
            return;
        }
        if(this.isEnabled()){
            if(!mAdapter.isDiscovering()){
                mAdapter.startDiscovery();
            }
        }else{
            Log.i(TAG, "------tj----Bluetooth not enable----");
        }
    }

    public void stopDiscovery(){
        if(mAdapter == null){
            return;
        }
        if(this.isEnabled()){
            if(mAdapter.isDiscovering()){
                mAdapter.cancelDiscovery();
            }
        }else{
            Log.i(TAG, "------tj----Bluetooth not enable----");
        }

    }

    public void startLeScan(int timeInSecond) {
        if(this.isEnabled() && isLeSupported()) {
            this.mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    Message msg = BluetoothDelegateAdapter.this.mHandler.obtainMessage(MSG_DEVICE_FOUND);
                    msg.obj = device;
                    BluetoothDelegateAdapter.this.mHandler.sendMessage(msg);
                }
            };
            this.mAdapter.startLeScan(this.mLeScanCallback);
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    BluetoothDelegateAdapter.this.stopLeScan();
                }
            }, (long)(timeInSecond * 1000));
        }
    }

    public void stopLeScan() {
        if(this.isEnabled() && isLeSupported()) {
            this.mAdapter.stopLeScan(this.mLeScanCallback);
            if(!this.mAdapter.isDiscovering()) {
                this.onEventReceived(MSG_DISCOVERY_FINISHED, null, null);
            }
        }

    }

    public static boolean isLeSupported() {
        if(Build.VERSION.SDK_INT >= 18) {
            return true;
        } else {
            Log.e("BluetoothIBridgeAdapter", "BLE can not be supported");
            return false;
        }
    }

    public void setDiscoverable(boolean discoverable){

    }

    public String getName(){
        if(mAdapter != null)
            return mAdapter.getName();
        else
            return null;
    }

    public String getAddress(){
        if(mAdapter != null)
            return mAdapter.getAddress();
        else
            return null;
    }

    public boolean connectDevice(BluetoothDevice device){
        boolean result = connectDevice(device, 10);
        return result;
    }

    public boolean connectDevice(BluetoothDevice device, int bondTime){
        boolean result = false;
        if(this.isEnabled()){
            this.connManager.connect(device, bondTime);
            result = true;
        }
        return result;
    }

    public void disconnectDevice(BluetoothDevice device){
        if(this.isEnabled()){
            this.connManager.disconnect(device);
        }
    }

    public void send(BluetoothDevice device, byte[] buffer, int length){
        if(this.isEnabled() && device != null){
            this.connManager.write(device, buffer, length);
        }
    }

    public void destroy(){
        if(connManager != null){
            connManager.stop();
        }
        this.mContext.unregisterReceiver(this.deviceReceiver);
    }

    protected void onEventReceived(int what, BluetoothDevice device, String message){
        if(mEventListeners == null){
            return;
        }
        Iterator iterator = mEventListeners.iterator();
        BTEventListener listener;
        if(!mEventListeners.isEmpty()){
            while(iterator.hasNext()){
                listener = (BTEventListener)iterator.next();
                switch(what){
                    case MSG_DEVICE_FOUND:
                        listener.onDeviceFound(device);
                        break;
                    case MSG_DISCOVERY_FINISHED:
                        listener.onDiscoveryFinished();
                        break;
                    case MSG_CONNECTED:
                        listener.onDeviceConnected(device);
                        break;
                    case MSG_DISCONNECTED:
                        listener.onDeviceDisconnected(device);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void registerEventListeners(BTEventListener listener){
        if(listener == null || mEventListeners == null)
            return;

        if(!mEventListeners.contains(listener)){
            mEventListeners.add(listener);
        }
        Log.i(TAG, "registerEventListeners listener"+listener.toString());
    }

    public void unregisterEventListeners(BTEventListener listener){
        if(listener != null && mEventListeners != null){
            mEventListeners.remove(listener);
        }
    }

    public void registerDataReceivers(DataReceiver receiver){
        if(connManager != null){
            connManager.registerDataReceivers(receiver);
        }
    }

    public void unregisterDataReceivers(DataReceiver receiver){
        if(connManager != null){
            connManager.unregisterDataReceivers(receiver);
        }
    }


    private class MyHandler extends Handler{

        private WeakReference<BluetoothDelegateAdapter> mAdapter;
        MyHandler(BluetoothDelegateAdapter adapter){
            this.mAdapter = new WeakReference(adapter);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            BluetoothDelegateAdapter adapter = this.mAdapter.get();
            Bundle bundle = msg.getData();
            adapter.onEventReceived(msg.what, (BluetoothDevice)msg.obj, bundle.getString("exception"));

        }
    }

    private class DeviceBroadcastReceiver extends  BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String exceptionMessage = null;
            BluetoothDevice dev;
            String action = intent.getAction();
            Bundle bundle = intent.getExtras();
            if(bundle != null){
                exceptionMessage = bundle.getString("exception");
            }

            Log.i(TAG, "---tj----broadcast message------" + action.toString());
            if(action.compareTo(BluetoothDevice.ACTION_FOUND) == 0){
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDelegateAdapter.this.onEventReceived(MSG_DEVICE_FOUND, dev, exceptionMessage);

            }
            if(action.compareTo(BluetoothDevice.ACTION_BOND_STATE_CHANGED) == 0){
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            if(action.compareTo(BluetoothDevice.ACTION_PAIRING_REQUEST) == 0){
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                int paringKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                Log.i(TAG, "---tj----type---"+type+"----paringKey---"+paringKey);
//                BluetoothDelegateAdapter.this.connManager.onPairingRequested(dev, type, paringKey);
//                if(type == 2 || type == 4){
//                    dev.setPairingConfirmation(true);
//                }else if(type == 5){
//                    String mPairingKey = String.format("%04d", new Object[]{Integer.valueOf(paringKey)});
//                    dev.setPin(mPairingKey.getBytes());
//                }

            }
            if(action.compareTo(BluetoothAdapter.ACTION_STATE_CHANGED) == 0){
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON){
                    if(BluetoothDelegateAdapter.this.connManager != null){
                        BluetoothDelegateAdapter.this.connManager.start();
                    }
                }
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF ){
                    if(BluetoothDelegateAdapter.this.connManager != null){
                        BluetoothDelegateAdapter.this.connManager.stop();
                    }
                }
            }
            if(action.compareTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) == 0){
                BluetoothDelegateAdapter.this.onEventReceived(MSG_DISCOVERY_FINISHED, null, null);
            }

        }
    }

    public interface DataReceiver{
        void onDataReceive(BluetoothDevice device, byte[] data, int length);
    }

    public interface BTEventListener{
        void onDiscoveryFinished();

        void onDeviceFound(BluetoothDevice device);

        void onDeviceConnected(BluetoothDevice device);

        void onDeviceConnectFailed(BluetoothDevice device);

        void onDeviceDisconnected(BluetoothDevice device);
    }
}
