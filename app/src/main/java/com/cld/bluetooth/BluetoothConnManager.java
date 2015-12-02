package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.cld.bluetooth.ConnectionListener.ConnectionReceiver;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by taojian on 2015/12/1.
 */
public class BluetoothConnManager implements ConnectionReceiver {

    private static final String TAG = "CLDLOGTAG";
    private static final UUID SDPUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private ConnectionListener mListener;
    private Connections mConnections;
    private ConnectThread mConnectThread;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler mHandler;


    public BluetoothConnManager(Context context, Handler handler){
        this.mHandler = handler;
        this.mConnections = new Connections(handler);
    }

    public synchronized void start(){
        if(this.mListener == null){
            this.mListener = new ConnectionListener(this, mAdapter);
        }
        this.mListener.start();
        if(this.mConnectThread != null){
            this.mConnectThread.cancel();
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
            this.mConnections.disconnectAll();
        }
    }

    public synchronized void connect(BluetoothDevice device){
        if(this.mConnectThread != null){
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        this.mConnectThread = new ConnectThread(device);
        this.mConnectThread.start();
    }

    public synchronized void disconnect(BluetoothDevice device){
        this.mConnections.disconnect(device);
    }

    public void write(BluetoothDevice device, byte[] buffer, int length){
        this.mConnections.write(device, buffer, length);
    }

    @Override
    public void onConnectionEstablished(BluetoothSocket socket) {
        BluetoothDevice device = socket.getRemoteDevice();
        this.mConnections.connected(socket, device);
    }

    private class ConnectThread extends Thread{

        private BluetoothDevice mmDevice;
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device){
            this.mmDevice = device;
            /**
             *取消已经的监听线程句柄和通信线程句柄
             *
             *
             */
            BluetoothSocket tmp = null;
            try{
                tmp = this.mmDevice.createRfcommSocketToServiceRecord(SDPUUID);
            } catch (IOException e) {
                Log.e(TAG, "--tj--createRfcommSocketToServiceRecord--error--");
                e.printStackTrace();
            }

            mmSocket = tmp;
        }

        @Override
        public void run() {
            if(mAdapter != null)
                mAdapter.cancelDiscovery();

            try{
                //阻塞连接，直到成功或者抛出异常
                mmSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mConnections.connected(mmSocket, mmDevice);
        }

        public void cancel(){
            try{
                if(mmSocket != null){
                    mmSocket.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "---tj---connectTthread cancel----socket close---");
        }

    }
}
