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


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.buletoothn.AES.*;
import com.buletoothn.BleService.BluetoothLeService;
import com.lee.buletoothn.R;

import static java.lang.Math.abs;

public class CalibrationActivity extends Activity {
    private final static String TAG = CalibrationActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private StringBuffer sbValues,disPlay,am;

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;

    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;


    boolean connect_status_bit=false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private Handler mHandler;


    Button  jiaozhun,write,jiance;


    int tx_count = 0;
    int connect_count = 0;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
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

                if( connect_count==0 )
                {
                    connect_count =1;
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } //接收FFE1串口透传数据通道数据
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData( intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA) );
            }
            //接收FFE2功能配置返回的数据
            else if (BluetoothLeService.ACTION_DATA_AVAILABLE1.equals(action)) {
            }
        }
    };

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    return false;
                }
            };


    EditText receiveData;
    Button clear_button;
    Timer timer = new Timer();
    CheckBox checkBox5;
    boolean send_hex = true;
    boolean rx_hex = false;
    byte[] mBytes = null;
    private String[] strArr;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration_device);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        initView();
        setClickListener();
        setChangeListener();
        AES mAes = new AES();

        sbValues = new StringBuffer();
        disPlay = new StringBuffer();

        mHandler = new Handler();
        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {

            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean sg = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        updateConnectionState(R.string.connecting);

    }

    private void setChangeListener() {

        checkBox5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if(isChecked){
                    rx_hex = true;
                }else{
                    rx_hex = false;

                }
            }
        });
    }

    private void setClickListener() {

        write.setOnClickListener(new ButtonClickEvent());
        jiaozhun.setOnClickListener(new ButtonClickEvent());
        jiance.setOnClickListener(new ButtonClickEvent());
        clear_button.setOnClickListener(new ButtonClickEvent());
    }

    private void initView() {
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        clear_button=(Button)findViewById(R.id.clear_button);//send data 1002
        jiance=(Button)findViewById(R.id.jiance);
        jiaozhun=(Button)findViewById(R.id.jiaozhun);
        write=(Button)findViewById(R.id.write);

        receiveData =(EditText)findViewById(R.id.rx_data_id_1);//1002 data

        receiveData.setText("");

        checkBox5 = (CheckBox)findViewById(R.id.checkBox5);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (mBluetoothLeService != null) {
                    if( mConnected==false ) {
                        updateConnectionState(R.string.connecting);
                        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                        Log.d(TAG, "Connect request result=" + result);
                    }
                }
            }super.handleMessage(msg);
        };
    };

    TimerTask task = new TimerTask() {

        @Override
        public void run() {
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
        //unregisterReceiver(mGattUpdateReceiver);
        //mBluetoothLeService.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService.disconnect();
        mBluetoothLeService = null;
        timer.cancel();
        timer=null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
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

    String da="";
    int len_g = 0;
    AES mAes = new AES();

    private void displayData( byte[] data1 ) {

        if (data1 != null && data1.length > 0) {
            if( rx_hex )
            {
                final StringBuilder stringBuilder = new StringBuilder( sbValues.length()  );
                StringBuffer my_StringBuffer = new StringBuffer();
                byte[] WriteBytes = mBluetoothLeService.hex2byte( stringBuilder.toString().getBytes() );

                for(byte byteChar : data1)
                    stringBuilder.append(String.format(" %02X", byteChar));

                String da = stringBuilder.toString();
                sbValues.append( da );
                if(sbValues.indexOf("+ok") != -1)
                {
                    String substr2=sbValues.substring(0, sbValues.length()-3);
                    Log.d(TAG, "displayData: sbValues.substring(0, sbValues.length()-3)");
//                    String deString = mAes.decrypt(sbValues.substring(0, sbValues.length()-3));
//                    receiveData.setText(deString);
                    mBluetoothLeService.txxx("+ok", send_hex);
                }
                receiveData.setText( sbValues.toString() );
            }
            else
            {
                String res = new String( data1 );
                sbValues.append( res ) ;
                if(sbValues.indexOf("+ok") != -1)
                {
                    String substr2=sbValues.substring(0, sbValues.length()-3);
                    Log.d(TAG, substr2);
                    //对接收的密文解密
                    String deString = mAes.decrypt(new String(substr2));
                    //解析分解密文
                    strArr = deString.split(";");

                    Log.d(TAG, deString);
                    disPlay.append(deString);
                    Log.d(TAG, "strArr.length"+String.valueOf(strArr.length));
                    //分析指标数据
                    algorithm(strArr);
                    disPlay.append("\n");
                    disPlay.append(algorithm(strArr));
                    disPlay.append("\n");
                    //成功接收数据，解析完成向设备端发送响应指令
                    mBluetoothLeService.txxx("+ok", send_hex);
                    //清空接收密文的的buff sbValues，防止下次接收数据解密失败
                    sbValues.delete(0,sbValues.length());
                    sbValues.setLength(0);
                    receiveData.setText( disPlay.toString() );
                } else {
                    receiveData.setText( sbValues.toString() );
                }
            }
            len_g += data1.length;
            if( sbValues.length()<= receiveData.getText().length() )
                receiveData.setSelection( sbValues.length() );
            if( sbValues.length()>=5000 )sbValues.delete(0,sbValues.length());
            mDataField.setText( ""+len_g );

        }

    }

    private String isSkin(int length) {
        StringBuffer sb = new StringBuffer();
        if(length>5){
            int[] mis = new int[7];
            for (int i = 0; i< strArr.length; i++) {
                mis[i] = Integer.valueOf(strArr[i]);
            }if(!skinStatues(mis)){
                for (int i = 0; i< mis.length; i++) {
                    mis[i] = 0;
                    sb.append(mis[i]+";");
                }
            }
        }
        return sb.toString();
    }

    private boolean skinStatues(int [] data){
        int Red,Green,Blue,Misortuer;
        Misortuer=data[0];Red=data[4]; Green=data[5]; Blue=data[6];
        Log.d(TAG, "Misortuer"+String.valueOf(Misortuer));
        Log.d(TAG, "Red"+String.valueOf(Red));
        if(((Red > 95 && Green > 40 && Blue > 20) || (Red > 210 && Green > 210 && Blue > 170))&&
                    (Misortuer<=200000 && Misortuer>70000) ){
            Log.d(TAG, "skin true");
            return true;
        }
        Log.d(TAG, "skin false");
        return false;
    }
    private String algorithm(String[] data) {
        double am = 0.00;
        int[] rxDatabuf = {0,0,0,0,0,0,0};
        StringBuffer sb = StringBufferToInt(data, rxDatabuf);
        Log.d(TAG, "rxDatabuf.length" + String.valueOf(rxDatabuf.length));

        if (data.length > 5) {
            if (!skinStatues(rxDatabuf)) {
                for (int i = 0; i < rxDatabuf.length - 3; i++) {
                    rxDatabuf[i] = 0;
                }
            }
        }
        if (rxDatabuf[0] != 0)
        {
            //水分值计算
            if ((rxDatabuf[0]) < 130000 && rxDatabuf[0] > 0) {
                rxDatabuf[0] = 100 - (int) (div(rxDatabuf[0] - 70000, (130000 - 70000), 4) * 25);
            } else if (rxDatabuf[0] > 160000 && rxDatabuf[0] < 200000) {
                rxDatabuf[0] = 120 - (int) (85 + div(rxDatabuf[0] - 160000, 20000, 4) * 15);
            } else if (rxDatabuf[0] >= 130000 && rxDatabuf[0] <= 160000) {
                rxDatabuf[0] = 110 - (int) (Math.pow(div((rxDatabuf[0] - 127159.1), 4.54545455, 4), 0.5));
            }
            Log.d(TAG, String.valueOf(rxDatabuf[0]));
            //黑色素计算
            rxDatabuf[1] = (int) (90 / (Math.pow(600, 3)) * (Math.pow(rxDatabuf[1], 3)) + 20);
            //分泌计算
            rxDatabuf[3] = (int) (100 - (90 / (Math.pow(2800, 2))) * (Math.pow(rxDatabuf[3], 2)));
        }
        for (int i = 0; i < rxDatabuf.length-3; i++) {
            sb.append(rxDatabuf[i] + ";");
        }
        return sb.toString();
    }

    private StringBuffer StringBufferToInt(String[] data, int[] mis) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i< data.length; i++) {
            mis[i] = Integer.valueOf(data[i]);
        }
        return sb;
    }

    public static double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) return;
        if( gattServices.size()>0&&mBluetoothLeService.get_connected_status( gattServices )==2 )//匹配KOI设备
        {
            connect_count = 0;
            if( connect_status_bit )
            {
                mConnected = true;
                mBluetoothLeService.Delay_ms( 100 );
                mBluetoothLeService.enable_JDY_ble( 0 );
                mBluetoothLeService.Delay_ms( 100 );
                mBluetoothLeService.enable_JDY_ble( 1 );
                mBluetoothLeService.Delay_ms( 100 );

                updateConnectionState(R.string.connected);
                mBluetoothLeService.txxx("+ok", send_hex);

            }else{
                //Toast.makeText(this, "Deleted Successfully!", Toast.LENGTH_LONG).show();
                Toast toast = Toast.makeText(CalibrationActivity.this, "提示！此设备不是KOI检测设备", Toast.LENGTH_SHORT);
                toast.show();
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
            if (v == clear_button){
                sbValues.delete(0,sbValues.length());
                len_g =0;
                da = "";
                receiveData.setText( da );
                mDataField.setText( ""+len_g );
                tx_count = 0;
            }
            if(v == write){
                String LedStastr="WH0";
                tx_count+=mBluetoothLeService.txxx( mDeviceAddress,send_hex );
                tx_count+=mBluetoothLeService.txxx( LedStastr,send_hex );
            }
            if(v == jiance){
                String LedStastr="WH2";
                tx_count+=mBluetoothLeService.txxx( LedStastr,send_hex );
            }
            if(v == jiaozhun){
                String LedStastr="WH1";
                tx_count+=mBluetoothLeService.txxx( LedStastr,send_hex );
            }
        }
    }

}
