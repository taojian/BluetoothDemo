package com.cld.bluetooth;


import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.cld.bluetooth.BluetoothDelegateAdapter.DataReceiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by taojian on 2015/12/1.
 */
public class Connections {

    private String TAG = "CLDLOGTAG";
    private ConnectionsList mList;
    private Handler mHandler;
    private ArrayList<DataReceiver> dataReceivers;

    public Connections(Handler handler){
        this.mList = new ConnectionsList();
        this.mList.clear();
        this.mHandler = handler;
    }

    public void registerDataReceiver(DataReceiver listener){
        if(this.dataReceivers == null){
            this.dataReceivers = new ArrayList();
        }

        if(!dataReceivers.contains(listener)){
            dataReceivers.add(listener);
        }
    }

    public void unregisterDataReceiver(DataReceiver listener){
        if(dataReceivers != null){
            dataReceivers.remove(listener);
        }
    }

    public void connected(BluetoothSocket socket, CldBluetoothDevice device){
        Log.i(TAG, "-----tj------new  ConnectedThread----");
        ConnectedThread conn = new ConnectedThread(socket, device, this.mHandler, dataReceivers);
        conn.start();
        if(this.mList != null){
            this.mList.addConnection(conn);
        }
        if(device != null){
            device.connected(true);
            device.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_CONNECTED);
        }
        Message msg = mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_CONNECTED);
        msg.obj = device;
        mHandler.sendMessage(msg);
    }


    public void disconnect(CldBluetoothDevice device){
        if(device == null){
            return;
        }
        ConnectedThread mThread = this.mList.findConnection(device);
        this.mList.removeConnection(device);
        if(mThread != null){
            if(device != null){
                device.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_DISCONNECTTING);
            }
            mThread.cancel();
        }
    }

    public void disconnectAll(){
        this.mList.releaseAllConnections();
    }

    public List<CldBluetoothDevice> getCurrentConnectedDevice() {
        return this.mList.getCurrentConnectedDevice();
    }

    public void write(CldBluetoothDevice device, byte[] buffer, int length) {
        this.mList.write(device, buffer, length);
    }

    private class ConnectionsList{
        private byte[] LOCK;
        private ArrayList<ConnectedThread> connectedList;

        ConnectionsList(){
            this.connectedList = new ArrayList<>();
            this.LOCK = new byte[0];
        }
        public void clear(){
            if(this.connectedList != null){
                synchronized (this.LOCK) {
                    this.connectedList.clear();
                }
            }
        }

        public void write(CldBluetoothDevice device, byte[] buffer, int length){
            if(device != null && buffer != null && length > 0){
                ConnectedThread found = this.findConnection(device);
                if(found != null){
                    found.write(buffer, length);
                }
            }
        }

        public void addConnection(ConnectedThread conn){
            ConnectedThread found = findConnection(conn.getDevice());
            if(found != null){
                synchronized (this.LOCK){
                    connectedList.remove(found);
                }
            }

            if(connectedList != null){
                synchronized (this.LOCK){
                    connectedList.add(conn);
                }
            }
        }

        public void removeConnection(CldBluetoothDevice device){
            ConnectedThread found = findConnection(device);
            if(found != null){
                connectedList.remove(found);
            }
        }

        public List<CldBluetoothDevice> getCurrentConnectedDevice() {
            ArrayList devicesList = new ArrayList();
            synchronized(this.LOCK) {
                Iterator i$ = this.connectedList.iterator();

                while(i$.hasNext()) {
                    ConnectedThread ds = (ConnectedThread)i$.next();
                    CldBluetoothDevice device = ds.getDevice();
                    if(device != null && !devicesList.contains(device)) {
                        devicesList.add(device);
                    }
                }

                return devicesList;
            }
        }

        public void releaseAllConnections(){
            synchronized (this.LOCK) {
                Iterator iterator = connectedList.iterator();
                while (iterator.hasNext()) {
                    ConnectedThread conn = (ConnectedThread) iterator.next();
                    if (conn != null) {
                        conn.cancel();
                    }
                }
                connectedList.clear();
            }
        }

        private ConnectedThread findConnection(CldBluetoothDevice device){
            ConnectedThread conn = null;
            synchronized (this.LOCK) {
                Iterator iterator = connectedList.iterator();
                while (iterator.hasNext()) {
                    ConnectedThread mThread = (ConnectedThread) iterator.next();
                    if (device != null && mThread != null) {
                        if (device.equals(mThread.getDevice())) {
                            conn = mThread;
                            break;
                        }
                    }
                }
                return conn;
            }
        }


    }
}
