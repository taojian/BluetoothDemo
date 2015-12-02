package com.cld.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class ComActivity extends AppCompatActivity {

    private static final String TAG = "ComActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_com);
        Intent mainActivity = this.getIntent();
        BluetoothDevice device = mainActivity.getParcelableExtra("DEVICE");
        Log.i(TAG, "---tj----" + device.getName() + "-----" + device.getAddress());
    }

}
