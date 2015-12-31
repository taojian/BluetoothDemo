package com.cld.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.cld.bluetooth.tools.SystemPropertiesProxy;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Created by taojian on 2015/12/24.
 */
public class CldBluetoothDevice implements Parcelable {

    static final UUID SPPUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice mDevice;
    private String mDeviceAddress;
    private String mDeviceName;
    private int mDeviceType;
    private boolean mIsConnected;
    private Direction mDirection;
    private ConnectStatus mConnectStatus;
    private BondStatus mBondStatus;
    private int mDeviceClass;
    private List<BluetoothGattService> mGattServices;
    static final String EXTRA_PAIRING_VARIANT = BluetoothDevice.EXTRA_PAIRING_VARIANT;
    static final String EXTRA_PAIRING_KEY = BluetoothDevice.EXTRA_PAIRING_KEY;
    static final String ACTION_PAIRING_REQUEST = BluetoothDevice.ACTION_PAIRING_REQUEST;
    static final String ACTION_PAIRING_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL";
    static final int PAIRING_VARIANT_PIN = 0;
    static final int PAIRING_VARIANT_PASSKEY = 1;
    static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    static final int PAIRING_VARIANT_CONSENT = 3;
    static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    static final int PAIRING_VARIANT_OOB_CONSENT = 6;
    public static int DEVICE_TYPE_CLASSIC = 0;
    public static int DEVICE_TYPE_BLE = 1;
    byte[] buffer;
    int length;

    public static final Creator<CldBluetoothDevice> CREATOR = new Creator<CldBluetoothDevice>() {
        @Override
        public CldBluetoothDevice createFromParcel(Parcel in) {
            return new CldBluetoothDevice(in);
        }

        @Override
        public CldBluetoothDevice[] newArray(int size) {
            return new CldBluetoothDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeviceName);
        dest.writeString(this.mDeviceAddress);
        dest.writeInt(this.mDeviceClass);
        dest.writeInt(this.mIsConnected?1:0);
        dest.writeInt(this.mDirection.ordinal());
        dest.writeInt(this.mConnectStatus.ordinal());
        dest.writeInt(this.mBondStatus.ordinal());

    }

    private void readFromParcel(Parcel source) {
        this.mDeviceName = source.readString();
        this.mDeviceAddress = source.readString();
        this.mDeviceClass = source.readInt();
        this.mIsConnected = source.readInt() == 1;
        int i = source.readInt();
        Direction[] ds = Direction.values();
        if(i < ds.length) {
            this.mDirection = Direction.values()[i];
        } else {
            this.mDirection = Direction.DIRECTION_NONE;
        }

        int j = source.readInt();
        ConnectStatus[] cs = ConnectStatus.values();
        if(i < cs.length) {
            this.mConnectStatus = ConnectStatus.values()[j];
        } else {
            this.mConnectStatus = ConnectStatus.STATUS_DISCONNECTED;
        }

        int k = source.readInt();
        BondStatus[] bs = BondStatus.values();
        if(i < cs.length) {
            this.mBondStatus = BondStatus.values()[j];
        } else {
            this.mBondStatus = BondStatus.STATE_BONDNONE;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = adapter.getRemoteDevice(this.mDeviceAddress);
    }

    @Deprecated
    public CldBluetoothDevice(String address) {
        this.mDirection = CldBluetoothDevice.Direction.DIRECTION_NONE;
        this.mConnectStatus = CldBluetoothDevice.ConnectStatus.STATUS_DISCONNECTED;
        this.mBondStatus = CldBluetoothDevice.BondStatus.STATE_BONDNONE;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.mDeviceAddress = address;
        this.mDevice = adapter.getRemoteDevice(address);
        this.mDeviceName = this.mDevice.getName();
        this.mDeviceType = DEVICE_TYPE_CLASSIC;
        BluetoothClass bluetoothClass = this.mDevice.getBluetoothClass();
        if(bluetoothClass != null) {
            this.mDeviceClass = this.mDevice.getBluetoothClass().getDeviceClass();
        } else {
            this.mDeviceClass = -1;
        }

    }

    public CldBluetoothDevice(BluetoothDevice device) {
        this.mDirection = Direction.DIRECTION_NONE;
        this.mConnectStatus = ConnectStatus.STATUS_DISCONNECTED;
        this.mBondStatus = BondStatus.STATE_BONDNONE;
        this.mDeviceAddress = device.getAddress();
        this.mDevice = device;
        this.mDeviceName = this.mDevice.getName();
        BluetoothClass bluetoothClass = null;

        try {
            bluetoothClass = this.mDevice.getBluetoothClass();
        } catch (NullPointerException var4) {
        }

        if(bluetoothClass != null) {
            this.mDeviceClass = this.mDevice.getBluetoothClass().getDeviceClass();
        } else {
            this.mDeviceClass = -1;
        }

    }

    public static CldBluetoothDevice createCldBluetoothDevice(String address, int deviceType) {
        CldBluetoothDeviceFactory factory = CldBluetoothDeviceFactory.getDefaultFactory();
        return factory.createDevice(address, deviceType);
    }

    private CldBluetoothDevice(Parcel source) {
        this.mDirection = CldBluetoothDevice.Direction.DIRECTION_NONE;
        this.mConnectStatus = CldBluetoothDevice.ConnectStatus.STATUS_DISCONNECTED;
        this.mBondStatus = CldBluetoothDevice.BondStatus.STATE_BONDNONE;
        this.readFromParcel(source);
    }

    public String getDeviceName() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        this.mDevice = adapter.getRemoteDevice(this.mDeviceAddress);
        this.mDeviceName = this.mDevice.getName();
        return this.mDeviceName;
    }

    public String getDeviceAddress() {
        return this.mDeviceAddress;
    }

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public void setDeviceType(int deviceType) {
        this.mDeviceType = deviceType;
    }


    private int getDeviceClass() {
        return this.mDeviceClass;
    }

    boolean isValidDevice() {
        return true;
    }

    public boolean equals(Object o) {
        if(o == null) {
            return false;
        } else if(!(o instanceof CldBluetoothDevice)) {
            return false;
        } else {
            String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
            CldBluetoothDevice dev = (CldBluetoothDevice)o;
            String another = dev.mDeviceAddress == null?"00:00:00:00:00:00":dev.mDeviceAddress;
            return addr.equals(another) && dev.getDeviceType() == this.mDeviceType;
        }
    }

    public String toString() {
        String name = this.mDeviceName == null?"Device":this.mDeviceName;
        String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
        return super.toString() + " [" + name + " - " + addr + "]";
    }

    public BluetoothSocket createSocket() {
        BluetoothSocket socket = null;
        if(Build.VERSION.SDK_INT >= 10 && !SystemPropertiesProxy.isMediatekPlatform()) {
            Class<?> cls = BluetoothDevice.class;
            Method m = null;

            try {
                m = cls.getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            if(m != null) {
                try {
                    socket = (BluetoothSocket)m.invoke(this.mDevice, new Object[]{SPPUUID});
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                socket = this.mDevice.createRfcommSocketToServiceRecord(SPPUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    BluetoothSocket createSocketWithChannel(int channel) {
        BluetoothSocket socket = null;
        Class<?> cls = BluetoothDevice.class;
        Method m = null;

        try {
            m = cls.getMethod("createRfcommSocket", new Class[]{Integer.TYPE});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        if(m != null) {
            try {
                socket = (BluetoothSocket)m.invoke(this.mDevice, new Object[]{Integer.valueOf(channel)});
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    public void connected(boolean connected) {
        this.mIsConnected = connected;
    }

    public boolean isConnected() {
        return this.mIsConnected;
    }

    void setConnectionDirection(Direction d) {
        this.mDirection = d;
    }

    Direction connectionDirection() {
        return this.mDirection;
    }

    void setConnectStatus(ConnectStatus d) {
        this.mConnectStatus = d;
    }

    public ConnectStatus getConnectStatus() {
        return this.mConnectStatus;
    }

    void setBondStatus() {
        if(this.mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            this.mBondStatus = BondStatus.STATE_BONDED;
        }

        if(this.mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            this.mBondStatus = BondStatus.STATE_BONDING;
        }

        if(this.mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
            this.mBondStatus = BondStatus.STATE_BONDNONE;
        }

    }

    void setBondStatus(BondStatus d) {
        this.mBondStatus = d;
    }

    public BondStatus getBondStatus() {
        return this.mBondStatus;
    }

    public void createBond() {
        try {
            this.mDevice.getClass().getMethod("createBond", (Class[]) null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void createBond(BluetoothDevice device) {
        try {
            BluetoothDevice.class.getMethod("createBond", (Class[])null).invoke(device, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public void removeBond() {
        try {
            this.mDevice.getClass().getMethod("removeBond", (Class[])null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public static void removeBond(BluetoothDevice device) {
        try {
            BluetoothDevice.class.getMethod("removeBond", (Class[])null).invoke(device, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    void setPin(byte[] pin) {
        try {
            Class<?> c = Class.forName(this.mDevice.getClass().getName());
            Method setPin = c.getMethod("setPin", new Class[]{byte[].class});
            setPin.setAccessible(true);
            setPin.invoke(this.mDevice, new Object[]{pin});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    void setPasskey(int passkey) {
        try {
            Class<?> c = Class.forName(this.mDevice.getClass().getName());
            Method setPasskey = c.getMethod("setPasskey", new Class[]{Integer.TYPE});
            setPasskey.setAccessible(true);
            setPasskey.invoke(this.mDevice, new Object[]{Integer.valueOf(passkey)});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    void setPairingConfirmation(boolean confirm) {
        try {
            Class<?> c = Class.forName(this.mDevice.getClass().getName());
            Method setPairingConfirmation = c.getMethod("setPairingConfirmation", new Class[]{Boolean.TYPE});
            setPairingConfirmation.setAccessible(true);
            setPairingConfirmation.invoke(this.mDevice, new Object[]{Boolean.valueOf(confirm)});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    void cancelPairingUserInput() {
        try {
            this.mDevice.getClass().getMethod("cancelPairingUserInput", (Class[])null).invoke(this.mDevice, new Object[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    boolean isBonded() {
        return this.mDevice != null?this.mDevice.getBondState() == BluetoothDevice.BOND_BONDED:false;
    }

    boolean isSameDevice(BluetoothDevice device, int deviceTye) {
        String addr = this.mDeviceAddress == null?"00:00:00:00:00:00":this.mDeviceAddress;
        String another = device.getAddress() == null?"00:00:00:00:00:00":device.getAddress();
        return addr.equals(another) && deviceTye == this.mDeviceType;
    }

    public void setGattServices(List<BluetoothGattService> gattServices) {
        this.mGattServices = gattServices;
    }

    public List<BluetoothGattService> getGattServices() {
        return this.mGattServices;
    }


    public static enum BondStatus {
        STATE_BONDED,
        STATE_BONDING,
        STATE_BONDNONE,
        STATE_BONDFAILED,
        STATE_BOND_OVERTIME,
        STATE_BOND_CANCLED;

    }

    public static enum ConnectStatus {
        STATUS_DISCONNECTED,
        STATUS_CONNECTED,
        STATUS_DISCONNECTTING,
        STATUS_CONNECTTING,
        STATUS_CONNECTFAILED,
        STATE_BONDED,
        STATE_BONDING,
        STATE_BONDNONE;
    }

    public static enum Direction {
        DIRECTION_NONE,
        DIRECTION_FORWARD,
        DIRECTION_BACKWARD;
    }
}
