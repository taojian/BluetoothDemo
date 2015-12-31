package com.cld.bluetooth;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private BluetoothConnManager4Le connManager4Le;
    private Context mContext = null;
    private ArrayList<BTEventListener> mEventListeners = new ArrayList<>();
    private boolean isEnabled = false;
    private boolean isAutoWritePincode = false;
    private MyHandler mHandler;
    private BluetoothAdapter.LeScanCallback mLeScanCallback = null;
    private boolean mDiscoveryOnlyBonded;
    private static BluetoothDelegateAdapter deleteAdapter;
    private static final String VERSION_CODE = "cld_bluetooth_1.0";

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

        if(isLeSupported()){
            this.connManager4Le = new BluetoothConnManager4Le(context, this.mHandler);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.mContext.registerReceiver(this.deviceReceiver, filter);
//        if(deleteAdapter != null){
//            deleteAdapter.clean();
//
//        }
        deleteAdapter = this;
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

    public boolean startDiscovery(boolean onlyBonded){
        if(mAdapter == null){
            return false;
        }
        boolean result = false;
        if(this.isEnabled()){
            this.mDiscoveryOnlyBonded = onlyBonded;
            if(mAdapter.isDiscovering()){
                mAdapter.cancelDiscovery();
            }

            if(onlyBonded){
                Log.i(TAG, "---tj-----startDiscovery only bonded----");
            }else{
                Log.i(TAG, "----tj-----startDiscovery----");
            }
            mAdapter.startDiscovery();
            result = true;
        }else{
            Log.i(TAG, "------tj----Bluetooth not enable----");
        }

        return result;
    }

    public boolean startDiscovery() {
        return this.startDiscovery(false);
    }

    public void stopDiscovery(){
        if(mAdapter == null){
            return;
        }
        if(this.isEnabled()){
            mAdapter.cancelDiscovery();
        }else{
            Log.i(TAG, "------tj----Bluetooth not enable----");
        }

    }

    public boolean startLeScan(int timeInSecond) {
        boolean result = false;
        if(this.isEnabled() && isLeSupported()) {
            this.mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    CldBluetoothDevice dev = CldBluetoothDeviceFactory.getDefaultFactory().createDevice(device, CldBluetoothDevice.DEVICE_TYPE_BLE);
                    dev.setConnectionDirection(CldBluetoothDevice.Direction.DIRECTION_FORWARD);
                    Message msg = BluetoothDelegateAdapter.this.mHandler.obtainMessage(MSG_DEVICE_FOUND);
                    msg.obj = dev;
                    BluetoothDelegateAdapter.this.mHandler.sendMessage(msg);
                }
            };
            this.mAdapter.startLeScan(this.mLeScanCallback);
            //为了尽量减少功能，在扫描到设备或持续扫描一段时间后，应该停止扫描
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    BluetoothDelegateAdapter.this.stopLeScan();
                }
            }, (long) (timeInSecond * 1000));
            result = true;
        }
        return result;
    }

    public void stopLeScan() {
        if(this.isEnabled() && isLeSupported()) {
            this.mAdapter.stopLeScan(this.mLeScanCallback);
            if(!this.mAdapter.isDiscovering()) {
                this.onEventReceived(MSG_DISCOVERY_FINISHED, null, null);
            }
        }

    }

    public static String getVersion(){
        return VERSION_CODE;
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

    public boolean connectDevice(CldBluetoothDevice device) {
        boolean result = this.connectDevice(device, 10);
        if(!result) {
            this.onEventReceived(MSG_CONNECT_FAILED, device, "parameter invalid");
        }

        return result;
    }

    public boolean connectDevice(CldBluetoothDevice device, int bondTime) {
        Log.i("BluetoothIBridgeAdapter", "connectDevice...");
        Log.i("BluetoothIBridgeAdapter", "bondTime = " + bondTime);
        boolean result = false;
        if(this.isEnabled()) {
            if(device != null) {
                Log.i("BluetoothIBridgeAdapter", "start to connect");
                if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    this.connManager.connect(device, bondTime);
                    result = true;
                } else if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_BLE) {
                    this.connManager4Le.connect(device);
                    result = true;
                }
            } else {
                Log.e("BluetoothIBridgeAdapter", "device is null");
            }
        } else {
            Log.e("BluetoothIBridgeAdapter", "bluetooth is not enabled");
        }

        Log.i("BluetoothIBridgeAdapter", "connectDevice.");
        return result;
    }

    public void cancelBondProcess() {
        Log.i("BluetoothIBridgeAdapter", "cancelBondProcess...");
        if(this.connManager != null) {
            this.connManager.cancelBond();
        }

        Log.i("BluetoothIBridgeAdapter", "cancelBondProcess.");
    }

    public void disconnectDevice(CldBluetoothDevice device){
        if(this.isEnabled()){
            if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_CLASSIC){
                this.connManager.disconnect(device);
            }else if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_BLE){
                this.connManager4Le.disconnect(device);
            }
        }
    }

    public void send(CldBluetoothDevice device, byte[] buffer, int length){
        if(this.isEnabled() && device != null){
            if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_CLASSIC) {
                this.connManager.write(device, buffer, length);
            }else if(device.getDeviceType() == CldBluetoothDevice.DEVICE_TYPE_BLE){
                this.connManager4Le.write(device, buffer, length);
            }
        }
    }

    public List<CldBluetoothDevice> getCurrentConnectedDevice() {
        Log.i("BluetoothIBridgeAdapter", "getCurrentConnectedDevice...");
        List devicesList = this.connManager.getCurrentConnectedDevice();
        List devicesList4Gatt = this.connManager4Le.getCurrentConnectedDevice();
        ArrayList devicesListTotal = new ArrayList();
        Iterator i$;
        CldBluetoothDevice device;
        if(devicesList != null) {
            i$ = devicesList.iterator();

            while(i$.hasNext()) {
                device = (CldBluetoothDevice)i$.next();
                devicesListTotal.add(device);
            }
        }

        if(devicesList4Gatt != null) {
            i$ = devicesList4Gatt.iterator();

            while(i$.hasNext()) {
                device = (CldBluetoothDevice)i$.next();
                devicesListTotal.add(device);
            }
        }

        Log.i("BluetoothIBridgeAdapter", devicesListTotal.size() + " devices got");
        Log.i("BluetoothIBridgeAdapter", "getCurrentConnectedDevice.");
        return devicesList;
    }

    public CldBluetoothDevice getLastConnectedDevice() {
        Log.i("BluetoothIBridgeAdapter", "getLastConnectedDevice...");
        CldBluetoothDevice device = null;
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        if(sp != null) {
            String deviceName = sp.getString("last_connected_device_name", "");
            String deviceAddress = sp.getString("last_connected_device_address", "");
            if(deviceAddress != null && deviceAddress != "" && deviceAddress != " ") {
                device = CldBluetoothDevice.createCldBluetoothDevice(deviceAddress, CldBluetoothDevice.DEVICE_TYPE_CLASSIC);
            }
        }

        if(device == null) {
            Log.i("BluetoothIBridgeAdapter", "no device found");
        } else {
            Log.i("BluetoothIBridgeAdapter", "name:" + device.getDeviceName() + "/" + "address:" + device.getDeviceAddress());
        }

        Log.i("BluetoothIBridgeAdapter", "getLastConnectedDevice.");
        return device;
    }

    public boolean setLastConnectedDevice(CldBluetoothDevice device) {
        Log.i("BluetoothIBridgeAdapter", "setLastConnectedDevice...");
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("last_connected_device_name", device.getDeviceName());
        editor.putString("last_connected_device_address", device.getDeviceAddress());
        boolean flag = editor.commit();
        if(device == null) {
            Log.i("BluetoothIBridgeAdapter", "device is null");
        } else {
            Log.i("BluetoothIBridgeAdapter", "name:" + device.getDeviceName() + "/" + "address:" + device.getDeviceAddress());
        }

        Log.i("BluetoothIBridgeAdapter", "setLastConnectedDevice.");
        return flag;
    }

    public boolean clearLastConnectedDevice() {
        Log.i("BluetoothIBridgeAdapter", "clearLastConnectedDevice...");
        SharedPreferences sp = this.mContext.getSharedPreferences("last_connected_device", 0);
        boolean flag = false;
        if(sp != null) {
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            flag = editor.commit();
        }

        Log.i("BluetoothIBridgeAdapter", "clearLastConnectedDevice.");
        return flag;
    }

    public String getLocalName() {
        Log.i("BluetoothIBridgeAdapter", "getLocalName.");
        Log.i("BluetoothIBridgeAdapter", "local name is " + this.mAdapter.getName());
        return this.mAdapter.getName();
    }

    public boolean setLocalName(String name) {
        Log.i("BluetoothIBridgeAdapter", "setLocalName to " + name);
        return this.mAdapter.setName(name);
    }

    public void setLinkKeyNeedAuthenticated(boolean authenticated) {
        Log.i("BluetoothIBridgeAdapter", "setLinkKeyNeedAuthenticated to " + authenticated);
        if(this.connManager != null) {
            this.connManager.setLinkKeyNeedAuthenticated(authenticated);
        }

    }

    public void setAutoBondBeforConnect(boolean auto) {
        Log.i("BluetoothIBridgeAdapter", "setAutoBondBeforConnect to " + auto);
        if(this.connManager != null) {
            this.connManager.setAutoBond(auto);
        }

    }

    public void setPincode(String pincode) {
        Log.i("BluetoothIBridgeAdapter", "setPincode to " + pincode);
        this.connManager.setPincode(pincode);
    }

    public void setAutoWritePincode(boolean autoWrite) {
        Log.i("BluetoothIBridgeAdapter", "setAutoWritePincode to " + autoWrite);
        this.isAutoWritePincode = autoWrite;
    }

    public void setDisvoverable(boolean bDiscoverable) {
        Log.i("BluetoothIBridgeAdapter", "setDisvoverable to " + bDiscoverable);
        if(this.isEnabled()) {
            int duration = bDiscoverable?120:1;
            Intent discoverableIntent;
            if(bDiscoverable) {
                discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
                discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", duration);
                this.mContext.startActivity(discoverableIntent);
            } else {
                discoverableIntent = new Intent("android.bluetooth.adapter.action.REQUEST_DISCOVERABLE");
                discoverableIntent.putExtra("android.bluetooth.adapter.extra.DISCOVERABLE_DURATION", 1);
                this.mContext.startActivity(discoverableIntent);
            }
        }

    }


    public void setTargetUUIDs(CldBluetoothDevice device, String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
        if(isLeSupported()) {
            this.connManager4Le.setTargetUUIDs(device, serviceUUID, notifyCharacteristicUUID, writeCharacteristicUUID);
        }

    }

    public void setMtu(CldBluetoothDevice device, int mtu) {
        if(isLeSupported() && Build.VERSION.SDK_INT >= 21) {
            this.connManager4Le.setMtu(device, mtu);
        }

    }

    private static String messageString(int message) {
        switch(message) {
            case 1:
                return "MESSAGE_DEVICE_CONNECTED";
            case 2:
                return "MESSAGE_DEVICE_DISCONNECTED";
            case 3:
            default:
                return "MESSAGE";
            case 4:
                return "MESSAGE_DEVICE_CONNECT_FAILED";
        }
    }

    private void clean() {
        if(this.connManager != null) {
            this.connManager.stop();
            this.connManager = null;
        }

        this.mContext = null;
        deleteAdapter = null;
    }

    public void destroy() {
        if(this.connManager4Le != null) {
            this.connManager4Le.destory();
            this.connManager4Le = null;
        }

        if(this.connManager != null) {
            this.connManager.stop();
            this.connManager = null;
        }

        if(this.mContext != null) {
            this.mContext.unregisterReceiver(this.deviceReceiver);
        }

        this.mContext = null;
        deleteAdapter = null;
    }

    protected void onEventReceived(int what, CldBluetoothDevice device, String message){
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
            adapter.onEventReceived(msg.what, (CldBluetoothDevice)msg.obj, bundle.getString("exception"));

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
                BluetoothDelegateAdapter.this.onEventReceived(MSG_DEVICE_FOUND, CldBluetoothDeviceFactory.getDefaultFactory().createDevice(dev, CldBluetoothDevice.DEVICE_TYPE_CLASSIC), exceptionMessage);

            }
            if(action.compareTo(BluetoothDevice.ACTION_BOND_STATE_CHANGED) == 0){
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                CldBluetoothDevice device = CldBluetoothDeviceFactory.getDefaultFactory().createDevice(dev, CldBluetoothDevice.DEVICE_TYPE_CLASSIC);
                if(device != null){
                    device.setBondStatus();
                }
            }
            if(action.compareTo(BluetoothDevice.ACTION_PAIRING_REQUEST) == 0){
                dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                CldBluetoothDevice device = CldBluetoothDeviceFactory.getDefaultFactory().createDevice(dev, CldBluetoothDevice.DEVICE_TYPE_CLASSIC);
                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                int paringKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR);
                Log.i(TAG, "---tj----type---"+type+"----paringKey---"+paringKey);
                BluetoothDelegateAdapter.this.connManager.onPairingRequested(device, type, paringKey);
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
        void onDataReceive(CldBluetoothDevice device, byte[] data, int length);
    }

    public interface BTEventListener{
        void onDiscoveryFinished();

        void onDeviceFound(CldBluetoothDevice device);

        void onDeviceConnected(CldBluetoothDevice device);

        void onDeviceConnectFailed(CldBluetoothDevice device);

        void onDeviceDisconnected(CldBluetoothDevice device);

        void onWriteFailed(CldBluetoothDevice var1, String var2);

        void onLeServiceDiscovered(CldBluetoothDevice var1, String var2);
    }
}
