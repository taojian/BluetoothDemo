package com.cld.bluetoothdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.cld.bluetooth.BluetoothDelegateAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by taojian on 2015/12/3.
 */
public class DemoManager {

    private static String TAG = "CLDLOGTAG";
    private static Context mContext;
    private static boolean mIsBound;
    private static BluetoothDelegateAdapter mAdapter;
    private List<BluetoothAdapterListener> mAdapterList = new ArrayList<>();

    DemoManager(Context context){
        this.mContext = context;
    }

    void doBindService() {
        mContext.bindService(new Intent(mContext, TestService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            mContext.unbindService(mConnection);
            mIsBound = false;
        }
    }
    static BluetoothDelegateAdapter getDelegateAdapter(){
        return mAdapter;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mAdapter = ((TestService.LocalBinder)service).getBluetoothAdapter();
            for(BluetoothAdapterListener listener : mAdapterList){
                listener.onBluetoothAdapterListenerCreated(mAdapter);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAdapter = null;
            for (BluetoothAdapterListener listener : mAdapterList){
                listener.onBluetoothAdapterListenerDestroy();
            }
        }
    };
    public void registerBluetoothAdapterListener(BluetoothAdapterListener listener){
        synchronized (mAdapterList){
            if(!mAdapterList.contains(listener)) {
                mAdapterList.add(listener);
            }
        }
    }

    public void unregisterBluetoothAdapterListener(BluetoothAdapterListener listener){
        synchronized (mAdapterList){
            if(!mAdapterList.contains(listener)) {
                mAdapterList.remove(listener);
            }
        }
    }

    public interface BluetoothAdapterListener{
        void onBluetoothAdapterListenerCreated(BluetoothDelegateAdapter adapter);
        void onBluetoothAdapterListenerDestroy();
    }
}
