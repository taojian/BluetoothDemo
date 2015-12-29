package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by taojian on 2015/12/24.
 */
public class CldBluetoothDeviceFactory{

    private static CldBluetoothDeviceFactory INSTANCE = null;
    private static byte[] LOCK = new byte[0];
    private List<CldBluetoothDevice> mList = new ArrayList();

    private CldBluetoothDeviceFactory() {
    }

    public static CldBluetoothDeviceFactory getDefaultFactory() {
        synchronized(LOCK) {
            if(INSTANCE == null) {
                INSTANCE = new CldBluetoothDeviceFactory();
            }
        }

        return INSTANCE;
    }

    public CldBluetoothDevice createDevice(BluetoothDevice device, int deviceTye) {
        if(null != device) {
            Iterator it = this.mList.iterator();

            CldBluetoothDevice newDev;
            do {
                if(!it.hasNext()) {
                    newDev = new CldBluetoothDevice(device);
                    newDev.setDeviceType(deviceTye);
                    this.mList.add(newDev);
                    return newDev;
                }

                newDev = (CldBluetoothDevice)it.next();
            } while(newDev == null || !newDev.isSameDevice(device, deviceTye));

            return newDev;
        } else {
            return null;
        }
    }

    public CldBluetoothDevice createDevice(String address, int deivceType) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null?this.createDevice(adapter.getRemoteDevice(address), deivceType):null;
    }
}
