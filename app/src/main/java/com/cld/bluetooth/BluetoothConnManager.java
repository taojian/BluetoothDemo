package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.cld.bluetooth.ConnectionListener.ConnectionReceiver;
import com.cld.bluetooth.tools.SystemPropertiesProxy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    public void connectFailed(BluetoothDevice device, String exception){
        synchronized(this){
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        Message msg = mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_CONNECT_FAILED);
        msg.obj = device;
        mHandler.sendMessage(msg);

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

        }

        @Override
        public void run() {
            if(mAdapter != null) {
                mAdapter.cancelDiscovery();
            }

            boolean connectResult = connectRfcommSocket();
            if(!connectResult){
                Log.i(TAG, "---tj----connectRfcommSocket failed----");
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(this.mmSocket != null){
                    try {
                        this.mmSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //端口6访问
                connectResult = connectWithChannel(6);
            }

            if(!connectResult){
                Log.i(TAG, "---tj----connectWithChannel failed----");
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(this.mmSocket != null){
                    try {
                        this.mmSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                BluetoothConnManager.this.connectFailed(this.mmDevice, null);
                synchronized(BluetoothConnManager.this){
                    BluetoothConnManager.this.mConnectThread = null;
                }
                return;
            }

            BluetoothConnManager.this.mConnections.connected(mmSocket, mmDevice);
        }

        private boolean connectRfcommSocket(){
            boolean result = false;
            int retryCount = 2;

            if(this.mmDevice != null) {
                Log.i(TAG, "---tj---name---"+mmDevice.getName()+"---address--"+mmDevice.getAddress());
                this.mmSocket = createSocket();
            }else{
                Log.e(TAG, "--tj---connect device is null-------");
            }

            while(true){
                try{
                    if(this.mmSocket != null){
                        this.mmSocket.connect();
                        result = true;
                        Log.i(TAG, "--tj---connect socket success!-----------");
                    }else {
                        Log.e(TAG, "--tj--connect socket is null-------");
                    }
                }catch(IOException e){
                    Log.i(TAG, "---tj----connect socket exception---" + e.getMessage());
                    if(retryCount > 0){
                        --retryCount;
                        try {
                            sleep(500);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        continue;
                    }
                    break;
                }

                break;
            }

            return result;
        }

        private boolean connectWithChannel(int channel) {
            boolean result;
            Log.i(TAG, "---tj---connectWithChannel-----" + channel + "---");
            this.mmSocket = createSocketWithChannel(channel);

            try {
                this.mmSocket.connect();
                result = true;
            } catch (IOException e) {
                result = false;
                Log.e(TAG, "---tj----connectWithChanne---failed" + e.getMessage());
            }
            return result;
        }

        private BluetoothSocket createSocket() {
            BluetoothSocket socket = null;
            if(Build.VERSION.SDK_INT >= 10 && !SystemPropertiesProxy.isMediatekPlatform()) {
                Class e = BluetoothDevice.class;
                Method m = null;

                try {
                    m = e.getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                } catch (NoSuchMethodException var9) {
                    var9.printStackTrace();
                }

                if(m != null) {
                    try {
                        socket = (BluetoothSocket)m.invoke(this.mmDevice, SDPUUID);
                    } catch (IllegalArgumentException e1) {
                        e1.printStackTrace();
                    } catch (IllegalAccessException e2) {
                        e2.printStackTrace();
                    } catch (InvocationTargetException e3) {
                        e3.printStackTrace();
                    }
                }
            } else {
                try {
                    socket = this.mmDevice.createRfcommSocketToServiceRecord(SDPUUID);
                } catch (IOException e) {
                    Log.e(TAG, "--tj--createRfcommSocketToServiceRecord--error--" + e.getMessage());
                }
            }

            return socket;
        }

        private BluetoothSocket createSocketWithChannel(int channel) {
            BluetoothSocket socket = null;
            Class cls = BluetoothDevice.class;
            Method m = null;

            try {
                m = cls.getMethod("createRfcommSocket", Integer.TYPE);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            if(m != null) {
                try {
                    socket = (BluetoothSocket)m.invoke(this.mmDevice, Integer.valueOf(channel));
                } catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                } catch (IllegalAccessException e2) {
                    e2.printStackTrace();
                } catch (InvocationTargetException e3) {
                    e3.printStackTrace();
                }
            }

            return socket;
        }


        public void cancel(){
            try{
                if(mmSocket != null){
                    mmSocket.close();
                    mmSocket = null;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "---tj---connectThread cancel----socket close---");
        }

    }
}
