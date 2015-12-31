package com.cld.bluetooth;


import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

import com.cld.bluetooth.BluetoothDelegateAdapter.DataReceiver;

/**
 * Created by taojian on 2015/12/1.
 */
public class ConnectedThread extends Thread{

    private final String TAG = "CLDLOGTAG";
    private final BluetoothSocket mSocket;      //socket句柄
    private final CldBluetoothDevice mDevice;      //主动连接设备
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private static final int MAX_LEN = 65536;
    private byte[] buffer;
    private boolean isSocketReset = false;
    private Handler mHandler;
    private ArrayList<DataReceiver> dataReceivers;

    public ConnectedThread(BluetoothSocket socket, CldBluetoothDevice device, Handler handler, ArrayList<DataReceiver> receivers){

        this.mSocket = socket;
        this.mDevice = device;
        this.mHandler = handler;
        this.dataReceivers = receivers;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.buffer = new byte[MAX_LEN];

        try{
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        }catch(IOException e){

        }

//        this.mmInStream = new BufferedInputStream(tmpIn);
//        this.mmOutStream = new BufferedOutputStream(tmpOut);
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
            Log.i(TAG, "---tj---send data----"+new String(this.buffer, 0, length));
        }catch(IOException e){
            Message msg = this.mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_WRITE_FAILED);
            msg.obj = this.mDevice;
            Bundle bundle = new Bundle();
            bundle.putString("exception", e.getMessage());
            msg.setData(bundle);
            this.mHandler.sendMessage(msg);
        }
    }

    public void cancel(){
        this.isSocketReset = true;
        resetSocket(this.mSocket);
    }

    public boolean equals(Object o) {
        if(o == null) {
            return false;
        } else if(!(o instanceof ConnectedThread)) {
            return false;
        } else {
            ConnectedThread conn = (ConnectedThread)o;
            return conn.mDevice.equals(this.mDevice);
        }
    }

    public CldBluetoothDevice getDevice(){
        return this.mDevice;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        while(true){
            try{
                Log.i(TAG, "----tj------mmInStream.read-----");
                //非阻塞操作
                int bytes = this.mmInStream.read(buffer);
                this.mDevice.buffer = buffer;
                this.mDevice.length = bytes;
                if(this.dataReceivers != null){
                    Iterator iterator = dataReceivers.iterator();
                    while(iterator.hasNext()){
                        DataReceiver receiver = (DataReceiver) iterator.next();
                        receiver.onDataReceive(this.mDevice, buffer, bytes);
                    }
                }

            }catch(IOException e){
                Log.i(TAG, "--tj-----IOException-----"+e.getMessage());
                this.connectionLost(e.getMessage());
                return;
            }
        }
    }

    private void connectionLost(String exceptionMsg) {
        Log.i(TAG, "----tj-----Connect lost-------");
        if(!this.isSocketReset) {
            resetSocket(this.mSocket);
        }

        if(this.mDevice != null){
            this.mDevice.connected(false);
            this.mDevice.setConnectStatus(CldBluetoothDevice.ConnectStatus.STATUS_DISCONNECTED);
        }
        Message msg = this.mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_DISCONNECTED);
        msg.obj = this.mDevice;
        Bundle bundle = new Bundle();
        bundle.putString("exception", exceptionMsg);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
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
