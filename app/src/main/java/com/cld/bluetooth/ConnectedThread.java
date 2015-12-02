package com.cld.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by taojian on 2015/12/1.
 */
public class ConnectedThread extends Thread{

    private final BluetoothSocket mSocket;      //socket句柄
    private final BluetoothDevice mDevice;      //主动连接设备
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private static final int MAX_LEN = 65536;
    private byte[] buffer;
    private boolean isSocketReset = false;
    private Handler mHandler;

    public ConnectedThread(BluetoothSocket socket, BluetoothDevice device, Handler handler){
        this.mSocket = socket;
        this.mDevice = device;
        this.mHandler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.buffer = new byte[MAX_LEN];

        try{
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        }catch(IOException e){

        }

        this.mmInStream = tmpIn;
        this.mmOutStream = tmpOut;
        this.isSocketReset = false;
    }

    public void write(byte[] buf, int length){
        try{
            int num = Math.min(length, 1024);
            System.arraycopy(buf, 0, this.buffer, 0, num);
            this.mmOutStream.write(this.buffer, 0, length);
            this.mmOutStream.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void cancel(){
        this.isSocketReset = true;
        resetSocket(this.mSocket);
    }

    public BluetoothDevice getDevice(){
        return this.mDevice;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while(true){
            try{
                int bytes = this.mmInStream.read(buffer);

            }catch(IOException e){
                this.connectionLost(e.getMessage());
            }
        }
    }

    private void connectionLost(String exception){
        if(!this.isSocketReset){
            resetSocket(this.mSocket);
        }
    }

    private void resetSocket(BluetoothSocket socket){
        if(socket != null){
            try{
                InputStream is = socket.getInputStream();
                if(is != null){
                    is.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

            try{
                OutputStream os = socket.getOutputStream();
                if(os != null){
                    os.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }

            try{
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}
