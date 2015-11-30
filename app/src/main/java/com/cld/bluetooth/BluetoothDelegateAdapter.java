package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;

/**
 * 蓝牙适配器类
 * Created by taojian on 2015/11/25.
 */
public class BluetoothDelegateAdapter {

    private static final  String TAG = "CLDLOGTAG";
    private BluetoothAdapter mAdapter = null;
    private BroadcastReceiver deviceReceiver = null;
    private Context mContext = null;
    private ArrayList<BTEventListener> mEventListeners = new ArrayList<>();
    private boolean isEnabled = false;
    private static final int MSG_DEVICE_FOUND       = 1;
    private static final int MSG_STATE_CHANGED      = 2;
    private static final int MSG_DISCOVERY_FINISHED = 3;

    public BluetoothDelegateAdapter(Context context){
        this.mContext = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.deviceReceiver = new DeviceBroadcastReceiver();

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
        return false;
    }
    protected void destroy(){
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

    private class DeviceBroadcastReceiver extends  BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String exceptionMessage = null;
            BluetoothDevice dev = null;
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

            }
            if(action.compareTo(BluetoothDevice.ACTION_PAIRING_REQUEST) == 0){

            }
            if(action.compareTo(BluetoothAdapter.ACTION_STATE_CHANGED) == 0){

            }
            if(action.compareTo(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) == 0){
                BluetoothDelegateAdapter.this.onEventReceived(MSG_DISCOVERY_FINISHED, null, null);
            }

        }
    }

    public interface BTEventListener{
        void onDiscoveryFinished();

        void onDeviceFound(BluetoothDevice device);

        void onDeviceConnected(BluetoothDevice device);

        void onDeviceConnectFailed(BluetoothDevice device);

        void onDeviceDisconnected(BluetoothDevice device);
    }
}
