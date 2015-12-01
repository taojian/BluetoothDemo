package com.cld.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;

/**
 * Created by taojian on 2015/12/1.
 */
public class BluetoothConnManager {

    private ConnectionListener mListener;
    private Connections mConnections;
    private ConnectThread mConnectThread;


    public BluetoothConnManager(Context context, Handler handler){

    }

    public synchronized void start(){
        if(this.mListener == null){
            this.mListener = new ConnectionListener();
        }
        this.mListener.start();
        if(this.mConnectThread != null){
            this.ConnectThread.cancel();
            this.mConnectThread = null;
        }
    }

    public synchronized void stop(){
        if(this.mListener != null){
            this.mListener.stop();
            this.mListener = null;
        }

        if(this.mConnectThread != null){
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        if(this.mConnections != null){
            this.mConnections.disConnect();
        }
    }

    public void connect(BluetoothDevice device){

    }

    public void disconnect(BluetoothDevice device){

    }

    private class ConnectThread extends Thread{

        public ConnectThread(){

        }

        @Override
        public void run() {
            super.run();
        }
    }
}
