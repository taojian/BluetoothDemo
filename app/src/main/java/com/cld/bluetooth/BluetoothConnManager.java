package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.cld.bluetooth.ConnectionListener.ConnectionReceiver;
import com.cld.bluetooth.tools.SystemPropertiesProxy;
import com.cld.bluetooth.CldBluetoothDevice.BondStatus;
import com.cld.bluetooth.CldBluetoothDevice.ConnectStatus;
import com.cld.bluetooth.CldBluetoothDevice.Direction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Created by taojian on 2015/12/1.
 */
public class BluetoothConnManager implements ConnectionReceiver {

    private static final String TAG = "CLDLOGTAG";
    private static final UUID SDPUUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB");
    private ConnectionListener mListener;
    private Connections mConnections;
    private ConnectThread mConnectThread;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private Handler mHandler;
    private String mPincode = "1234";
    private boolean auth = true;
    private boolean autoPair = true;
    String lastExceptionMsg = null;


    public BluetoothConnManager(Context context, Handler handler){
        this.mHandler = handler;
        this.mConnections = new Connections(handler);
    }

    public synchronized void start(){
        if(this.mListener == null){
            this.mListener = new ConnectionListener(this, this.auth);
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

    public synchronized void connect(CldBluetoothDevice device, int bondTime){
        if(this.mConnectThread != null){
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        device.setBondStatus();
        if(this.autoPair && device.getBondStatus().equals(BondStatus.STATE_BONDNONE)){
            device.setBondStatus(BondStatus.STATE_BONDING);
        }

        if(device != null && !device.isConnected()){
            device.setConnectStatus(ConnectStatus.STATUS_CONNECTTING);
            this.mConnectThread = new ConnectThread(device, bondTime);
            this.mConnectThread.start();
        }
    }

    public synchronized void cancelBond() {
        if(this.mConnectThread != null) {
            this.mConnectThread.cancelBondProcess();
        }

    }

    public synchronized void disconnect(CldBluetoothDevice device){
        this.mConnections.disconnect(device);
    }

    public void write(CldBluetoothDevice device, byte[] buffer, int length){
        this.mConnections.write(device, buffer, length);
    }

    public void connectFailed(CldBluetoothDevice device, String exception){
        synchronized(this){
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }
        Message msg = mHandler.obtainMessage(BluetoothDelegateAdapter.MSG_CONNECT_FAILED);
        msg.obj = device;
        Bundle bundle = new Bundle();
        bundle.putString("exception", exception);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        synchronized (this){
            this.mConnectThread = null;
        }

    }

    List<CldBluetoothDevice> getCurrentConnectedDevice() {
        List devicesList = null;
        devicesList = this.mConnections.getCurrentConnectedDevice();
        return devicesList;
    }

    public void setPincode(String pincode) {
        this.mPincode = pincode;
    }

    public void setAutoBond(boolean auto) {
        this.autoPair = auto;
    }

    void setLinkKeyNeedAuthenticated(boolean authenticated) {
        if(this.mListener != null) {
            this.auth = authenticated;
            this.mListener.setLinkKeyNeedAuthenticated(authenticated);
        }

    }

    void onPairingRequested(CldBluetoothDevice device, int type, int pairingKey) {
        String mPairingKey;
        switch(type) {
            case 0:
                device.setPin(this.mPincode.getBytes());
            case 1:
            default:
                break;
            case 2:
            case 3:
                device.setPairingConfirmation(true);
                break;
            case 4:
                mPairingKey = String.format("%06d", new Object[]{Integer.valueOf(pairingKey)});
                device.setPairingConfirmation(true);
                break;
            case 5:
                mPairingKey = String.format("%04d", new Object[]{Integer.valueOf(pairingKey)});
                device.setPin(mPairingKey.getBytes());
        }

    }

    @Override
    public void onConnectionEstablished(BluetoothSocket socket) {
        CldBluetoothDevice device = CldBluetoothDeviceFactory
                .getDefaultFactory().createDevice(socket.getRemoteDevice(),
                        CldBluetoothDevice.DEVICE_TYPE_CLASSIC);
        if(device != null){
            device.setConnectionDirection(Direction.DIRECTION_BACKWARD);
            device.setBondStatus();
        }
        this.mConnections.connected(socket, device);
    }

    public void registerDataReceivers(BluetoothDelegateAdapter.DataReceiver receiver) {
        if(this.mConnections != null){
            this.mConnections.registerDataReceiver(receiver);
        }
    }

    public void unregisterDataReceivers(BluetoothDelegateAdapter.DataReceiver receiver) {
        if(this.mConnections != null) {
            this.mConnections.unregisterDataReceiver(receiver);
        }
    }

    private class ConnectThread extends Thread{

        private CldBluetoothDevice mmDevice = null;
        private BluetoothSocket mmSocket = null;
        private int mBondTime;
        private String name;
        private boolean cancelBond = false;

        public ConnectThread(CldBluetoothDevice device, int bondTime){
            this.mmDevice = device;
            this.mBondTime = bondTime;
            this.name = device.getDeviceName();
        }

        @Override
        public void run() {
            this.setName("ConnectThread" + this.name);
            if(mAdapter != null) {
                mAdapter.cancelDiscovery();
            }

            if(this.mmDevice != null){
                this.mmDevice.setConnectStatus(ConnectStatus.STATUS_CONNECTTING);
            }

            if(BluetoothConnManager.this.autoPair){
                Log.i(TAG, "---tj----start auto pair----");
                this.doAutoPair();
            }

            boolean connectResult = connectRfcommSocket();
            if(!connectResult){
                Log.i(TAG, "---tj----connectRfcommSocket failed----");
                if(this.mmDevice.getBondStatus() == BondStatus.STATE_BONDED){
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(this.mmSocket != null){
                        try {
                            this.mmSocket.close();
                            this.mmSocket = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    //端口1无法连接，端口6可以
                    connectResult = connectWithChannel(6);
                }
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
                        this.mmSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                BluetoothConnManager.this.connectFailed(this.mmDevice, BluetoothConnManager.this.lastExceptionMsg);
                synchronized(BluetoothConnManager.this){
                    BluetoothConnManager.this.mConnectThread = null;
                }
                return;
            }

            if(this.mmDevice != null){
                this.mmDevice.setConnectionDirection(CldBluetoothDevice.Direction.DIRECTION_FORWARD);
                this.mmDevice.setBondStatus();
            }

            BluetoothConnManager.this.mConnections.connected(mmSocket, mmDevice);
            Log.i(TAG, "----tj---connected!---");
        }

        private void doAutoPair() {
            boolean isPaired = false;
            boolean bonding = false;
            int during = 0;
            for(; during < this.mBondTime * 2; ++during){
                BluetoothDevice device = BluetoothConnManager.this.mAdapter.getRemoteDevice(this.mmDevice.getDeviceAddress());
                // 连接建立之前的先配对
                Log.i(TAG, "---tj----start paring---"+this.mmDevice.getBondStatus());
                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.i(TAG, "----tj---bond success------------");
                    isPaired = true;
                    bonding = false;
                    this.mmDevice.setBondStatus(BondStatus.STATE_BONDED);
                    break;
                }
                if (device.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.i(TAG, "----tj---bonding------------");
                    this.mmDevice.setBondStatus(BondStatus.STATE_BONDING);
                }else if (device.getBondState() == BluetoothDevice.BOND_NONE){
                    try{
                        if(!bonding){
                            this.mmDevice.createBond();
                            Log.i("TAG", "---tj---start pairing------");
                            bonding = true;
                            this.mmDevice.setBondStatus(BondStatus.STATE_BONDING);
                        }else{
                            Log.i("TAG", "---tj---pair failed------");
                            bonding = false;
                            this.mmDevice.setBondStatus(BondStatus.STATE_BONDFAILED);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }

                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if(this.cancelBond){
                Log.i(TAG, "----tj----bond canceled---");
                this.mmDevice.setBondStatus(BondStatus.STATE_BOND_CANCLED);
            }else if(!isPaired && during >= this.mBondTime){
                Log.i(TAG, "---tj-----bond timeout-----");
                this.mmDevice.setBondStatus(BondStatus.STATE_BOND_OVERTIME);
            }

            Log.i(TAG , "---tj----doAutoPair--");
        }

        private boolean connectRfcommSocket(){
            boolean result = false;
            int retryCount = 2;

            if(this.mmDevice != null) {
                Log.i(TAG, "---tj---name---"+mmDevice.getDeviceName()+"---address--"+mmDevice.getDeviceAddress());
                this.mmSocket = this.mmDevice.createSocket();
            }else{
                Log.e(TAG, "--tj---connect device is null-------");
            }

            if(SystemPropertiesProxy.isMediatekPlatform()) {
                try {
                    Log.i(TAG, "---tj-----it is MTK platform");
                    sleep(3000L);
                } catch (InterruptedException var6) {
                    var6.printStackTrace();
                }
            }

            while(true){
                try{
                    if(this.mmSocket != null){
                        //阻塞操作，返回成功连接或者异常
                        this.mmSocket.connect();
                        result = true;
                        Log.i(TAG, "--tj---connect socket success!-----------");
                    }else {
                        Log.e(TAG, "--tj--connect socket is null-------");
                        BluetoothConnManager.this.lastExceptionMsg = "socket is null";
                    }
                }catch(IOException e){
                    result = false;
                    Log.e(TAG, "---tj----connect socket exception---" + e.getMessage());
                    if(e.getMessage() != null && e.getMessage().equals("Service discovery failed")){
                        if(retryCount > 0){
                            --retryCount;
                            try {
                                sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            continue;
                        }else{
                            BluetoothConnManager.this.lastExceptionMsg = e.getMessage();
                        }
                        break;
                    }else{
                        Log.e(TAG, "---tj---connect failed----");
                        BluetoothConnManager.this.lastExceptionMsg = e.getMessage();
                    }
                }
                break;
            }

            return result;
        }

        private boolean connectWithChannel(int channel) {
            boolean result;
            Log.i(TAG, "---tj---start connectWithChannel-----" + channel + "---");
            this.mmSocket = this.mmDevice.createSocketWithChannel(channel);

            try {
                //阻塞操作，返回成功连接或者异常
                this.mmSocket.connect();
                result = true;
            } catch (IOException e) {
                result = false;
                Log.e(TAG, "---tj----connectWithChannel---failed" + e.getMessage());
                BluetoothConnManager.this.lastExceptionMsg = e.getMessage();
            }
            Log.i(TAG, "----tj----end connectWithChannel-------");
            return result;
        }

        private BluetoothSocket createSocket() {
            BluetoothSocket socket = null;
            if(Build.VERSION.SDK_INT >= 10 && !SystemPropertiesProxy.isMediatekPlatform()) {
                Class e = BluetoothDevice.class;
                Method m = null;
                Log.i(TAG, "---tj-----create socket reflect to service record----");
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
//                try {
////                    socket = this.mmDevice.createRfcommSocketToServiceRecord(SDPUUID);
//                } catch (IOException e) {
//                    Log.e(TAG, "--tj--createRfcommSocketToServiceRecord--error--" + e.getMessage());
//                }
            }

            return socket;
        }

        private BluetoothSocket createSocketWithChannel(int channel) {
            BluetoothSocket socket = null;
            Class cls = BluetoothDevice.class;
            Method m = null;

            try {
                m = cls.getMethod("createRfcommSocket", new Class[]{int.class});
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

        public void cancelBondProcess(){
            Log.i(TAG, "----tj---cancelBondProcess--");
            this.cancelBond = true;
        }
    }
}
