package com.cld.bluetoothdemo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.cld.bluetooth.BluetoothDelegateAdapter;
import com.cld.bluetooth.BluetoothDelegateAdapter.BTEventListener;
import com.cld.bluetooth.CldBluetoothDevice;

public class TestService extends Service implements BTEventListener {
	private final String TAG = "CLDLOGTAG";
	private BluetoothDelegateAdapter mAdapter;
	private IBinder mBinder = new LocalBinder();
	private PowerManager.WakeLock mWakeLock;
	
	@Override
	public void onCreate() {
		mAdapter = new BluetoothDelegateAdapter(this);
		if(!mAdapter.isEnabled()){
			mAdapter.setEnabled(true);
		}

		mAdapter.registerEventListeners(this);
		acquireWakeLock(); 
		Log.i(TAG, "----TestService---onCreate----------");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		releaseWakeLock();
		mAdapter.unregisterEventListeners(this);
		mAdapter.destroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "---TestService--onBind---");
		return mBinder ;
	}

	public class LocalBinder extends Binder {
		BluetoothDelegateAdapter getBluetoothAdapter() {
			return mAdapter;
		}
	}
	
	private void startForeground() {
		Intent it = new Intent(this, MainActivity.class);
		it.setAction(Intent.ACTION_MAIN);
		it.addCategory(Intent.CATEGORY_LAUNCHER);
		it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pit = PendingIntent.getActivity(this, 0, it,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Notification ntfc = new Notification(R.mipmap.ic_launcher, getString(R.string.app_name),
				System.currentTimeMillis());
		ntfc.setLatestEventInfo(this, getString(R.string.connected), null,
				pit);
		ntfc.flags |= Notification.FLAG_ONGOING_EVENT;
		startForeground(0x37512433, ntfc);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly 
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDeviceConnected(CldBluetoothDevice device) {
		ComponentName comp = new ComponentName(getPackageName(),
				MainActivity.class.getName());
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(comp);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("com.cld.bleSpp.device", device);
		startActivity(intent);
		startForeground();
	}

	@Override
	public void onDeviceConnectFailed(CldBluetoothDevice device) {

	}

	@Override
	public void onDeviceDisconnected(CldBluetoothDevice device) {
		stopForeground(true);
		NotificationManager nm =(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(0x37512433);
	}

	@Override
	public void onWriteFailed(CldBluetoothDevice var1, String var2) {

	}

	@Override
	public void onLeServiceDiscovered(CldBluetoothDevice var1, String var2) {

	}

	@Override
	public void onDeviceFound(CldBluetoothDevice device) {
	}

	@Override
	public void onDiscoveryFinished() {
	}

	private void acquireWakeLock() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this
				.getClass().getCanonicalName());
		mWakeLock.acquire();
	}

	private void releaseWakeLock() {
		if (mWakeLock != null && mWakeLock.isHeld()) {
			mWakeLock.release();
			mWakeLock = null;
		}
	}
	
}
