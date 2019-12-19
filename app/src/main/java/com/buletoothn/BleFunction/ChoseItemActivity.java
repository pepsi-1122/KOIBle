/**
 * 
 */

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.buletoothn.BleFunction;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.buletoothn.BleService.BluetoothLeService;
import com.lee.buletoothn.R;

public class ChoseItemActivity extends Activity {

    private final static String TAG = CalibrationActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    boolean connect_status_bit=false;


    Button Test;
    Button calibration;
    EditText switch_new_name_value;

    private Handler mHandler;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // 成功启动初始化后自动连接到设备
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connect_status_bit=true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                connect_status_bit=false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }
            //接收FFE1串口透传数据通道数据
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            }
            //接收FFE2功能配置返回的数据
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE1.equals(action)) {
            }
        }
    };

    // GATT characteristic被选中  读取”和“通知”功能

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    return false;
                }
    };

    Timer timer = new Timer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.choseitemfor_device);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        setTitle( mDeviceName );
        
        initView();
        setClickListener();




        mHandler = new Handler();
        getActionBar().setTitle("I-Futuer");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        updateConnectionState(R.string.waittoconnect);

         //timer.schedule(task, 1000, 1000);  // 1s后执行task,经过1s再次执行
        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);

    }

    private void setClickListener() {
        Test.setOnClickListener(new ButtonClickEvent());
        calibration.setOnClickListener(new ButtonClickEvent());
    }

    private void initView() {
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        Test = (Button)findViewById(R.id.Test);
        calibration= (Button)findViewById(R.id.calibration);
        switch_new_name_value = (EditText)findViewById(R.id.switch_new_pass_value);
        switch_new_name_value.setText( mDeviceName );
    }


    Handler handler = new Handler() {
        public void handleMessage(Message msg) {  
        	if (msg.what == 1) {
            	if (mBluetoothLeService != null) {
                	if( mConnected==false ) {
                		updateConnectionState(R.string.connecting);
                		final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                		//Log.d(TAG, "Connect request result=" + result);
                	}
                }
            }super.handleMessage(msg);
        };  
    };  
    TimerTask task = new TimerTask() {  
    	  
        @Override  
        public void run() {
            // 需要做的事:发送消息
            Message message = new Message();  
            message.what = 1;  
            handler.sendMessage(message);  
        }  
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService = null;
        timer.cancel();
        timer=null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // 遍历受支持的GATT服务/特性Services/Characteristics.

    private void displayGattServices(List<BluetoothGattService> gattServices) {
    	if (gattServices == null) return;
        if( gattServices.size()>0&&mBluetoothLeService.get_connected_status( gattServices )==2 )
        {
	        if( connect_status_bit ) {
	        	 mConnected = true;
				 mBluetoothLeService.enable_JDY_ble( 1 );
				 mBluetoothLeService.Delay_ms( 100 );
				 updateConnectionState(R.string.connected);
			}
        }
    }
 
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE1);
        return intentFilter;
    }

    private class ButtonClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == Test){
                Intent intent1 = new Intent(ChoseItemActivity.this,CheckActivity.class);;
                intent1.putExtra(CheckActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent1.putExtra(CheckActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(intent1);
            }
            if (v == calibration){
                Intent intent1 = new Intent(ChoseItemActivity.this,CalibrationActivity.class);;
                intent1.putExtra(CalibrationActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent1.putExtra(CalibrationActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(intent1);
            }

        }
    }
}
