package com.cld.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Created by taojian on 2015/12/1.
 */
public class Connections {

    private ConnectionsList mList;
    private Handler mHandler;

    public Connections(Handler handler){
        this.mList = new ConnectionsList();
        this.mList.clear();
        this.mHandler = handler;
    }

    public void connected(BluetoothSocket socket, BluetoothDevice device){
        ConnectedThread conn = new ConnectedThread(socket, device, this.mHandler);
        conn.start();
        if(this.mList != null){
            this.mList.addConnection(conn);
        }
    }

    public void disconnect(BluetoothDevice device){
        ConnectedThread mThread = this.mList.findConnection(device);
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

        public void clear(){
            this.connectedList.clear();
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
                if(device.equals(mThread.getDevice())){
                    conn = mThread;
                    break;
                }
            }
            return conn;
        }


    }
}
