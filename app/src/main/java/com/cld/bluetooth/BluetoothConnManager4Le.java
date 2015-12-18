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
import android.os.Message;
import android.util.Log;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.cld.bluetooth.BluetoothDelegateAdapter.*;

/**
 * Created by taojian on 2015/12/17.
 */
public class BluetoothConnManager4Le {

    private final String TAG = "CLDLOGTAG";
    private Context mContext;
    private Handler mHandler;
    private BluetoothManager mBluetoothmanager;
    private ArrayList<DataReceiver> mDataReceivers = new ArrayList<>();
    private int mMtu = 20;
    private int credit = 0;

    public BluetoothConnManager4Le(Context context, Handler handler){
        this.mContext = context;
        this.mHandler = handler;
        this.mBluetoothmanager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public void connect(BluetoothDevice device){
        if(device != null) {
            new GattConnection(this.mContext, device, this.mHandler, this.mDataReceivers);
        }
    }

    public void disconnect(BluetoothDevice device){

    }

    public void disconnectAll(){

    }

    public void close(){

    }

    public void write(BluetoothDevice device, byte[] buf, int length){

    }

    public void registerDataReceiver(DataReceiver receiver){
        if(mDataReceivers == null){
            return;
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
        private BluetoothDevice device;
        private BluetoothGatt mBluetoothGatt;
        private BluetoothAdapter mBluetoothAdapter;
        private ArrayList<DataReceiver> mDataReceivers;

        private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if(gatt == GattConnection.this.mBluetoothGatt && newState == BluetoothProfile.STATE_CONNECTED){
                    Message msg = GattConnection.this.mHandler.obtainMessage(MSG_CONNECTED);
                    msg.obj = GattConnection.this.device;
                    GattConnection.this.mHandler.sendMessage(msg);
                    Log.i(TAG, "-------Connected to GATT server----");
                    Log.i(TAG, "-------Attempting to start service discovery----" +
                            mBluetoothGatt.discoverServices());
                }else if(gatt == GattConnection.this.mBluetoothGatt && newState == BluetoothProfile.STATE_DISCONNECTED){
                    Log.i(TAG, "------Disconnected from GATT server-------");
                    Message msg = GattConnection.this.mHandler.obtainMessage(MSG_DISCONNECTED);
                    msg.obj = GattConnection.this.device;
                    GattConnection.this.mHandler.sendMessage(msg);
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
//                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){
//                    GattConnection.this.onDataChanged(characteristic);
//                }
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
                super.onDescriptorWrite(gatt, descriptor, status);
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                if(gatt == GattConnection.this.mBluetoothGatt && status == BluetoothGatt.GATT_SUCCESS){

                }
            }
        };


        public GattConnection(Context context, BluetoothDevice dev, Handler handler, ArrayList<DataReceiver> dataReceivers){
            this.mContext = context;
            this.mHandler = handler;
            this.device = dev;
            this.mDataReceivers = dataReceivers;

            if(BluetoothConnManager4Le.this.mBluetoothmanager != null) {
                this.mBluetoothAdapter = BluetoothConnManager4Le.this.mBluetoothmanager.getAdapter();
            }
            if(connect(dev)){
                Log.i(TAG, "------GattDevice connect success----");
            }


        }

        private boolean connect(BluetoothDevice device){
            // Previously connected device.  Try to reconnect.
            if (mBluetoothGatt != null) {
                Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
                if (mBluetoothGatt.connect()) {
                    return true;
                } else {
                    return false;
                }
            }

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(this.mContext, false, this.mBluetoothGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
            return true;
        }

        public void disconnect(){
            if(mBluetoothGatt != null){
                mBluetoothGatt.disconnect();
            }
        }

        public void close(){
            if(mBluetoothGatt != null){
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }

        public void discoverServices(){
            if(mBluetoothGatt != null){
                mBluetoothGatt.discoverServices();
            }
        }

        /**
         * Retrieves a list of supported GATT services on the connected device. This should be
         * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
         *
         * @return A {@code List} of supported services.
         */
        public List<BluetoothGattService> getSupportedGattServices() {
            if (mBluetoothGatt == null) return null;

            return mBluetoothGatt.getServices();
        }

        /**
         * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
         * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
         * callback.
         *
         * @param characteristic The characteristic to read from.
         */
        public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            mBluetoothGatt.readCharacteristic(characteristic);
        }

        /**
         * Enables or disables notification on a give characteristic.
         *
         * @param characteristic Characteristic to act on.
         * @param enabled If true, enable notification.  False otherwise.
         */
        public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                                  boolean enabled) {
            if (mBluetoothAdapter == null || mBluetoothGatt == null) {
                Log.w(TAG, "BluetoothAdapter not initialized");
                return;
            }
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

            /* This is specific to Heart Rate Measurement.
            if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                        UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }*/
        }

        void onServicesFound(List<BluetoothGattService> gattServices) {
            Message msg = this.mHandler.obtainMessage(MSG_LE_SERVICES_DISCOVERED);
            msg.obj = this.device;
            this.mHandler.sendMessage(msg);
            Iterator iterator = gattServices.iterator();

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

        void onDataChanged(BluetoothGattCharacteristic characteristic) {
            byte[] data;
            int i;
            if(this.mNotifyCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                if(data != null && data.length > 0) {
                    this.buffer = data;
                    this.length = data.length;
                    if(this.mDataReceivers != null) {
                        Iterator iterator = mDataReceivers.iterator();
                        while (iterator.hasNext()){
                            DataReceiver receiver = (DataReceiver)iterator.next();
                            if(receiver != null){
                                receiver.onDataReceive(this.device, this.buffer, this.length);
                            }
                        }
                    }
                }
            }

            if(this.mMTUCharacteristicUUID.equals(characteristic.getUuid().toString())) {
                data = characteristic.getValue();
                byte tmp = data[0];
                if(tmp == 1) {
                    BluetoothConnManager4Le.this.credit = data[1];
                } else if(tmp == 2) {
                    i = data[1] + (data[2] << 8);
                }
            }

        }



    }

}
