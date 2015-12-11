package com.cld.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.cld.bluetooth.BluetoothDelegateAdapter.DataReceiver;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by taojian on 2015/12/1.
 */
public class Connections {

    private String TAG = "CLDLOGTAG";
    private ConnectionsList mList;
    private Handler mHandler;
    private ArrayList<DataReceiver> dataReceivers = new ArrayList<>();

    public Connections(Handler handler){
        this.mList = new ConnectionsList();
        this.mList.clear();
        this.mHandler = handler;
    }

    public void registerDataReceiver(DataReceiver listener){
        if(!dataReceivers.contains(listener)){
            dataReceivers.add(listener);
        }
    }

    public void unregisterDataReceiver(DataReceiver listener){
        if(dataReceivers != null){
            dataReceivers.remove(listener);
        }
    }

    public void connected(BluetoothSocket socket, BluetoothDevice device){
        Log.i(TAG, "-----tj------new  ConnectedThread----");
        ConnectedThread conn = new ConnectedThread(socket, device, this.mHandler, dataReceivers);
        conn.start();
        if(this.mList != null){
            this.mList.addConnection(conn);
        }
        Message msg = mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_CONNECTED);
        msg.obj = device;
        mHandler.sendMessage(msg);
    }


    public void disconnect(BluetoothDevice device){
        if(device == null){
            return;
        }
        ConnectedThread mThread = this.mList.findConnection(device);
        this.mList.removeConnection(device);
        if(mThread != null){
            mThread.cancel();
        }
    }

    public void disconnectAll(){
        this.mList.releaseAllConnections();
    }

    public void write(BluetoothDevice device, byte[] buffer, int length) {
        this.mList.write(device, buffer, length);
    }

    private class ConnectionsList{
        private ConnectedThread mThread;
        private ArrayList<ConnectedThread> connectedList;

        ConnectionsList(){
            this.connectedList = new ArrayList<>();
        }
        public void clear(){
            if(this.connectedList != null){
                this.connectedList.clear();
            }
        }

        public void write(BluetoothDevice device, byte[] buffer, int length){
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
                connectedList.remove(found);
            }

            if(connectedList != null){
                connectedList.add(conn);
            }
        }

        public void removeConnection(BluetoothDevice device){
            ConnectedThread found = findConnection(device);
            if(found != null){
                connectedList.remove(found);
            }
        }

        public void releaseAllConnections(){
            Iterator iterator = connectedList.iterator();
            while(iterator.hasNext()){
                ConnectedThread conn = (ConnectedThread)iterator.next();
                if(conn != null){
                    conn.cancel();
                }
            }
            connectedList.clear();
        }

        private ConnectedThread findConnection(BluetoothDevice device){
            Iterator iterator = connectedList.iterator();
            ConnectedThread conn = null;
            while(iterator.hasNext()){
                ConnectedThread mThread = (ConnectedThread)iterator.next();
                if(device != null && mThread != null){
                    if(device.equals(mThread.getDevice())){
                        conn = mThread;
                        break;
                    }
                }
            }
            return conn;
        }


    }
}
