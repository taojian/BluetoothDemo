package com.cld.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by taojian on 2015/12/1.
 */
public class Connections {

    private ConnectionsList mList;

    public Connections(){
        this.mList = new ConnectionsList();
        this.mList.clear();
    }

    public void connect(BluetoothSocket socket, BluetoothDevice device){
        ConnectedThread conn = new ConnectedThread(socket, device);
        conn.start();
        if(this.mList != null){
            this.mList.addConnection(conn);
        }
    }

    public void disConnect(BluetoothDevice device){

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
            Iterator iterator = connectedList.iterator();
            while (iterator.hasNext()){
                ConnectedThread conn = (ConnectedThread)iterator.next();
                conn.write(buffer, length);
            }
        }

        public void addConnection(ConnectedThread conn){
            if(connectedList != null){

            }
        }


    }
}
