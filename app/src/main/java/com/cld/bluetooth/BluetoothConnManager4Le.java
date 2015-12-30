package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.cld.bluetooth.BluetoothDelegateAdapter.*;

/**
 * Created by taojian on 2015/12/17.
 */
public class BluetoothConnManager4Le {

    private final String TAG = "CLDLOGTAG";
    private Context mContext;
    private Handler mHandler;
    private BluetoothManager mBluetoothmanager;
    private ArrayList<DataReceiver> mDataReceivers;
    private BluetoothConnManager4Le.ConnectionList mList;
    private int mMtu = 20;
    private int credit = 0;

    public BluetoothConnManager4Le(Context context, Handler handler){
        this.mContext = context;
        this.mHandler = handler;
        this.mBluetoothmanager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mList = new BluetoothConnManager4Le.ConnectionList();
        this.mList.clear();
    }

    public void registerDataReceiver(DataReceiver receiver){
        if(mDataReceivers == null){
            this.mDataReceivers = new ArrayList();
        }
        if(!mDataReceivers.contains(receiver)){
            mDataReceivers.add(receiver);
        }
    }

    public void unregisterDataReceiver(DataReceiver receiver){
        if(mDataReceivers != null){
            mDataReceivers.remove(receiver);
        }
    }

    public void connect(CldBluetoothDevice device){
        if(device != null && !device.isConnected()) {
            device.setBondStatus(CldBluetoothDevice.BondStatus.STATE_BONDED);
            device.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_CONNECTTING);
            new GattConnection(this.mContext, device, this.mHandler, this.mDataReceivers);
        }else{
            Log.e(TAG, "----tj----device is connected or is null----");
        }
    }

    public void disconnect(CldBluetoothDevice device){
        BluetoothConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        if(found != null){
            found.disconnect();
        }
    }

    public void disconnectAll(){
        this.mList.releaseAllConnections();
    }

    public void destory() {
        this.mList.releaseAllConnections();
        this.mHandler = null;
        this.mDataReceivers = null;
    }

    public void write(CldBluetoothDevice device, byte[] buf, int length){
        this.mList.write(device, buf, length);
    }

    public List<CldBluetoothDevice> getCurrentConnectedDevice() {
        return this.mList.getCurrentConnectedDevice();
    }

    public void setTargetUUIDs(CldBluetoothDevice device, String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
        BluetoothConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        if(found != null) {
            found.setTargetUUIDs(serviceUUID, notifyCharacteristicUUID, writeCharacteristicUUID);
        }

    }

    public void setMtu(CldBluetoothDevice device, int mtu) {
        BluetoothConnManager4Le.GattConnection found = this.mList.foundDevice(device);
        if(found != null) {
            this.mMtu = mtu;
            found.setMtu(mtu);
        }

    }

    class GattConnection{

        static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
        private String mGattServiceUUID = "0000ff00-0000-1000-8000-00805f9b34fb";
        private String mNotifyCharacteristicUUID = "0000ff01-0000-1000-8000-00805f9b34fb";
        private String mWriteCharacteristicUUID = "0000ff02-0000-1000-8000-00805f9b34fb";
        private String mMTUCharacteristicUUID = "0000ff03-0000-1000-8000-00805f9b34fb";
        public BluetoothGattCharacteristic mWriteCharacteristic;
        private BluetoothGattCharacteristic mNotifyCharacteristic;
        private BluetoothGattCharacteristic mMTUCharacteristic;
        private final BluetoothConnManager4Le.GattConnection mGattConnection = this;
        private byte[] buffer;
        private int length;
        private Context mContext;
        private Handler mHandler;
        private CldBluetoothDevice device;
        private BluetoothGatt mBluetoothGatt;
        private BluetoothAdapter mBluetoothAdapter;
        private ArrayList<DataReceiver> mDataReceivers;

        private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Message msg;
                if(gatt == GattConnection.this.mBluetoothGatt && newState == BluetoothProfile.STATE_CONNECTED){
                    BluetoothConnManager4Le.this.mList.addConnection(GattConnection.this.mGattConnection);
                    GattConnection.this.discoveryServices();
                    if(GattConnection.this.mHandler != null) {
                        msg = GattConnection.this.mHandler.obtainMessage(MSG_CONNECTED);
                        msg.obj = GattConnection.this.device;
                        GattConnection.this.device.connected(true);
                        GattConnection.this.device.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_CONNECTED);
                        GattConnection.this.mHandler.sendMessage(msg);
                    }
                }else if(gatt == GattConnection.this.mBluetoothGatt && newState == BluetoothProfile.STATE_DISCONNECTED){
                    Log.i(TAG, "BluetoothGattCallback STATE_DISCONNECTED");
                    BluetoothConnManager4Le.this.credit = 0;
                    GattConnection.this.mWriteCharacteristic = null;
                    GattConnection.this.mNotifyCharacteristic = null;
                    GattConnection.this.mMTUCharacteristic = null;
                    if(GattConnection.this.mHandler != null) {
                        msg = GattConnection.this.mHandler.obtainMessage(MSG_DISCONNECTED);
                        msg.obj = GattConnection.this.device;
                        GattConnection.this.device.connected(false);
                        GattConnection.this.device.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_DISCONNECTED);
                        GattConnection.this.mHandler.sendMessage(msg);
                    }

                    GattConnection.this.close();
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){
                    GattConnection.this.onServicesFound(gatt.getServices());
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){
                    GattConnection.this.onDataChanged(characteristic);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){
                    GattConnection.this.onDataChanged(characteristic);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if(gatt == GattConnection.this.mBluetoothGatt){
                    GattConnection.this.onDataChanged(characteristic);
                }
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                if(GattConnection.this.mNotifyCharacteristicUUID.equals(descriptor.getCharacteristic().getUuid().toString()) && GattConnection.this.mMTUCharacteristic != null) {
                    GattConnection.this.setCharacteristicNotification(GattConnection.this.mMTUCharacteristic, true);
                }
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){
                    BluetoothConnManager4Le.this.mMtu = mtu;
                }else{
                    Log.i(TAG, "request mtu fail");
                    BluetoothConnManager4Le.this.mMtu = 20;
                }
            }
        };


        public GattConnection(Context context, CldBluetoothDevice mDevice, Handler handler, ArrayList<DataReceiver> dataReceivers){
            this.mHandler = handler;
            this.mDataReceivers = dataReceivers;
            this.mBluetoothAdapter = BluetoothConnManager4Le.this.mBluetoothmanager.getAdapter();
            this.device = mDevice;
            BluetoothDevice dev = this.mBluetoothAdapter.getRemoteDevice(this.device.getDeviceAddress());
            if(Build.VERSION.SDK_INT < 21) {
                this.mBluetoothGatt = dev.connectGatt(context, false, this.mBluetoothGattCallback);
            } else {
                Class cls = BluetoothDevice.class;
                Method m = null;

                try {
                    m = cls.getMethod("connectGatt", new Class[]{Context.class, Boolean.TYPE, BluetoothGattCallback.class, Integer.TYPE});
                    if(m != null) {
                        try {
                            this.mBluetoothGatt = (BluetoothGatt)m.invoke(dev, new Object[]{context, Boolean.valueOf(false), this.mBluetoothGattCallback, Integer.valueOf(2)});
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }

        public void setTargetUUIDs(String serviceUUID, String notifyCharacteristicUUID, String writeCharacteristicUUID) {
            this.mGattServiceUUID = serviceUUID;
            this.mWriteCharacteristicUUID = writeCharacteristicUUID;
            this.mNotifyCharacteristicUUID = notifyCharacteristicUUID;
            if(serviceUUID == "0000ff00-0000-1000-8000-00805f9b34fb") {
                this.mMTUCharacteristicUUID = "0000ff03-0000-1000-8000-00805f9b34fb";
            }

            Iterator iterator = this.getSupportedGattServices().iterator();

            while(iterator.hasNext()) {
                BluetoothGattService gattService = (BluetoothGattService)iterator.next();
                String serviceUUIDString = gattService.getUuid().toString();
                if(serviceUUIDString != null && serviceUUIDString.equals(this.mGattServiceUUID)) {
                    List gattCharacteristics = gattService.getCharacteristics();
                    Iterator iterator1 = gattCharacteristics.iterator();

                    while(iterator1.hasNext()) {
                        BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic)iterator1.next();
                        String characteristicUUIDString = gattCharacteristic.getUuid().toString();
                        if(characteristicUUIDString.equals(this.mWriteCharacteristicUUID)) {
                            this.mWriteCharacteristic = gattCharacteristic;
                        }

                        if(characteristicUUIDString.equals(this.mNotifyCharacteristicUUID)) {
                            this.mNotifyCharacteristic = gattCharacteristic;
                            this.setCharacteristicNotification(this.mNotifyCharacteristic, true);
                        }

                        if(characteristicUUIDString.equals(this.mMTUCharacteristicUUID)) {
                            this.mMTUCharacteristic = gattCharacteristic;
                            BluetoothConnManager4Le.this.credit = 0;
                        }
                    }

                    return;
                }
            }

        }

        public void setMtu(int mtu) {
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.requestMtu(mtu);
            }

        }

        public void discoveryServices() {
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.discoverServices();
            }

        }

        public void close() {
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.close();
                this.mBluetoothGatt = null;
            }

        }

        public void disconnect(){
            if(mBluetoothGatt != null){
                mBluetoothGatt.disconnect();
            }
        }

        public void write(byte[] buf, int length) {
            if(this.mWriteCharacteristic != null) {
                int len = length;

                for(int off = 0; len > 0; len -= BluetoothConnManager4Le.this.mMtu) {
                    byte[] buffer = new byte[BluetoothConnManager4Le.this.mMtu];
                    if(len >= BluetoothConnManager4Le.this.mMtu) {
                        System.arraycopy(buf, off, buffer, 0, BluetoothConnManager4Le.this.mMtu);
                    } else {
                        System.arraycopy(buf, off, buffer, 0, len);
                    }

                    if(this.mMTUCharacteristic != null) {
                        if(BluetoothConnManager4Le.this.credit > 0) {
                            this.mWriteCharacteristic.setValue(buffer);
                            this.mWriteCharacteristic.setWriteType(1);
                            this.writeCharacteristic(this.mWriteCharacteristic);
//                            BluetoothConnManager4Le.access$310(BluetoothConnManager4Le.this);
                        }
                    } else {
                        this.mWriteCharacteristic.setValue(buffer);
                        this.mWriteCharacteristic.setWriteType(1);
                        this.writeCharacteristic(this.mWriteCharacteristic);
                    }

                    off += BluetoothConnManager4Le.this.mMtu;
                }
            }

        }

        void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
                if(this.mNotifyCharacteristicUUID.equals(characteristic.getUuid().toString()) || this.mMTUCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                    characteristic.setWriteType(2);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    if(descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }

                    this.mBluetoothGatt.writeDescriptor(descriptor);
                }
            }

        }

        void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.readCharacteristic(characteristic);
            }

        }

        void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
            boolean flag = false;
            if(this.mBluetoothGatt != null) {
                this.mBluetoothGatt.writeCharacteristic(characteristic);
            }

        }

        List<BluetoothGattService> getSupportedGattServices() {
            return this.mBluetoothGatt == null?null:this.mBluetoothGatt.getServices();
        }

        void onServicesFound(List<BluetoothGattService> gattServices) {
            Message msg = this.mHandler.obtainMessage(MSG_LE_SERVICES_DISCOVERED);
            this.device.setGattServices(gattServices);
            msg.obj = this.device;
            this.mHandler.sendMessage(msg);
            Iterator iterator = gattServices.iterator();

            while(iterator.hasNext()) {
                BluetoothGattService gattService = (BluetoothGattService)iterator.next();
                String serviceUUIDString = gattService.getUuid().toString();
                if(serviceUUIDString != null && serviceUUIDString.equals(this.mGattServiceUUID)) {
                    List gattCharacteristics = gattService.getCharacteristics();
                    Iterator i$1 = gattCharacteristics.iterator();

                    while(i$1.hasNext()) {
                        BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic)i$1.next();
                        String characteristicUUIDString = gattCharacteristic.getUuid().toString();
                        if(characteristicUUIDString.equals(this.mWriteCharacteristicUUID)) {
                            this.mWriteCharacteristic = gattCharacteristic;
                        }

                        if(characteristicUUIDString.equals(this.mNotifyCharacteristicUUID)) {
                            this.mNotifyCharacteristic = gattCharacteristic;
                            this.setCharacteristicNotification(this.mNotifyCharacteristic, true);
                        }

                        if(characteristicUUIDString.equals(this.mMTUCharacteristicUUID)) {
                            this.mMTUCharacteristic = gattCharacteristic;
                            BluetoothConnManager4Le.this.credit = 0;
                        }
                    }

                    return;
                }
            }

        }

        void onDataChanged(BluetoothGattCharacteristic characteristic) {
            byte[] data;
            int num;
            if(this.mNotifyCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                if(data != null && data.length > 0) {
                    this.device.buffer = data;
                    this.device.length = data.length;
                    if(this.mDataReceivers != null) {
                        ArrayList d = (ArrayList)this.mDataReceivers.clone();
                        num = d.size();

                        for(int n = 0; n < num; ++n) {
                            DataReceiver er = (DataReceiver)d.get(n);
                            if(this.device.isValidDevice()) {
                                er.onDataReceive(this.device, this.device.buffer, this.device.length);
                            }
                        }
                    }
                }
            }

            if(this.mMTUCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                byte b = data[0];
                if(b == 1) {
                    BluetoothConnManager4Le.this.credit = data[1];
                } else if(b == 2) {
                    num = data[1] + (data[2] << 8);
                }
            }

        }

        public boolean equals(Object o) {
            if(o == null) {
                return false;
            } else if(!(o instanceof GattConnection)) {
                return false;
            } else {
                GattConnection conn = (GattConnection)o;
                return conn.device.equals(this.device);
            }
        }

        public CldBluetoothDevice getDevice() {
            return this.device;
        }

    }


    private class ConnectionList {
        private List<GattConnection> mConnectedDevices;
        private byte[] LOCK;

        private ConnectionList() {
            this.mConnectedDevices = new ArrayList();
            this.LOCK = new byte[0];
        }

        public void write(CldBluetoothDevice device, byte[] buffer, int length) {
            if(null != device && null != buffer && length > 0) {
                GattConnection found = this.foundDevice(device);
                if(null != found) {
                    found.write(buffer, length);
                }
            }
        }

        public void addConnection(GattConnection connection) {
            GattConnection found = this.foundDevice(connection.getDevice());
            if(found != null) {
                synchronized(this.LOCK) {
                    this.mConnectedDevices.remove(found);
                }
            }
            synchronized(this.LOCK) {
                this.mConnectedDevices.add(connection);
            }
        }

        private GattConnection foundDevice(CldBluetoothDevice device) {
            GattConnection found = null;
            synchronized(this.LOCK) {
                Iterator iterator = this.mConnectedDevices.iterator();

                while(iterator.hasNext()) {
                    GattConnection ds = (GattConnection)iterator.next();
                    if(device.equals(ds.getDevice())) {
                        found = ds;
                        break;
                    }
                }
                return found;
            }
        }

        public List<CldBluetoothDevice> getCurrentConnectedDevice() {
            ArrayList devicesList = new ArrayList();
            synchronized(this.LOCK) {
                Iterator iterator = this.mConnectedDevices.iterator();

                while(iterator.hasNext()) {
                    GattConnection ds = (GattConnection)iterator.next();
                    CldBluetoothDevice device = ds.getDevice();
                    if(device != null && !devicesList.contains(device)) {
                        devicesList.add(device);
                    }
                }

                return devicesList;
            }
        }

        public void clear() {
            synchronized(this.LOCK) {
                this.mConnectedDevices.clear();
            }
        }

        public void releaseAllConnections() {
            synchronized(this.LOCK) {
                Iterator iterator = this.mConnectedDevices.iterator();

                while(true) {
                    if(!iterator.hasNext()) {
                        break;
                    }

                    GattConnection ds = (GattConnection)iterator.next();
                    if(ds != null) {
                        ds.close();
                    }
                }
            }

            this.mConnectedDevices.clear();
        }
    }

}
