package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;
import android.os.Build.VERSION;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


final class ConnectionListener {
    private static final String TAG = "CLDLOGTAG";
    static final UUID SDPUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothAdapter mAdapter;
    private ConnectionListener.ConnectionReceiver mReceiver;
    private ConnectionListener.AcceptThread mThread;


    public ConnectionListener(ConnectionReceiver receiver, BluetoothAdapter adapter) {
        this.mReceiver = receiver;
        this.mAdapter = adapter;
    }

    public void start() {
        if(this.mThread != null) {
            this.mThread.cancel();
        }

        this.mThread = new ConnectionListener.AcceptThread();
        this.mThread.start();
    }

    public void stop() {
        if(this.mThread != null) {
            this.mThread.cancel();
        }

    }

    private class AcceptThread extends Thread {
        private static final String SERVICE_NAME = "Bluetooth";
        private final BluetoothServerSocket mmServerSocket;
        private volatile boolean running;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            this.running = true;

            try {
                if(VERSION.SDK_INT >= 10) {
                    tmp = this.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, SDPUUID);
                    Log.i(TAG, "insecure rfcomm " + tmp);
                } else {
                    tmp = ConnectionListener.this.mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SDPUUID);
                    Log.i(TAG, "secure rfcomm " + tmp);
                }
            } catch (IOException var4) {
                Log.e("ConnListener", "Connection listen failed", var4);
            }

            this.mmServerSocket = tmp;
        }

        public void run() {
            this.setName("AcceptThread");
            BluetoothSocket socket;

            while(this.running) {
                try {
                    if(this.mmServerSocket == null) {
                        return;
                    }
                    //阻塞等待,返回成功的连接或者异常
                    socket = this.mmServerSocket.accept();
                } catch (IOException var3) {
                    Log.i(TAG, "accept failed");
                    return;
                }

                if(socket != null && ConnectionListener.this.mReceiver != null) {
                    ConnectionListener.this.mReceiver.onConnectionEstablished(socket);
                }
            }

        }

        public void cancel() {
            try {
                if(this.mmServerSocket != null) {
                    this.mmServerSocket.close();
                }
            } catch (IOException var2) {
            }

        }

        private BluetoothServerSocket listenUsingInsecureRfcommWithServiceRecord(String serviceName, UUID serviceUUID) {
            BluetoothServerSocket socket = null;

            try {
                Class e = BluetoothAdapter.class;
                Method m = e.getMethod("listenUsingInsecureRfcommWithServiceRecord", new Class[]{String.class, UUID.class});
                socket = (BluetoothServerSocket)m.invoke(ConnectionListener.this.mAdapter, new Object[]{serviceName, serviceUUID});
            } catch (NoSuchMethodException var6) {
                var6.printStackTrace();
            } catch (IllegalArgumentException var7) {
                var7.printStackTrace();
            } catch (IllegalAccessException var8) {
                var8.printStackTrace();
            } catch (InvocationTargetException var9) {
                var9.printStackTrace();
            }

            return socket;
        }
    }

    public interface ConnectionReceiver {
        void onConnectionEstablished(BluetoothSocket var1);
    }
}
