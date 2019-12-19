package com.buletoothn.BleFunction;

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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.buletoothn.AES.AES;
import com.buletoothn.BleService.BluetoothLeService;
import com.lee.buletoothn.R;

public class CheckActivity extends Activity{
    private final static String TAG = CalibrationActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private StringBuffer sbValues;

    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    boolean connect_status_bit = false;

    private Handler mHandler;

    TextView sjj_connect_text_id;

    Switch switch1, switch2;


    int tx_count = 0;
    int connect_count = 0;
    // Code to manage Service lifecycle.
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

    // 处理服务触发的各种事件
    // ACTION_GATT_CONNECTED: 连接到GATT服务。.
    // ACTION_GATT_DISCONNECTED: 从一个GATT服务断开
    // ACTION_GATT_SERVICES_DISCOVERED: 发现GATT 服务.
    // ACTION_DATA_AVAILABLE: 从设备接收数据。这可能是读取的结果或通知操作
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connect_status_bit = true;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                connect_status_bit = false;
                invalidateOptionsMenu();
                if (connect_count == 0) {
                    connect_count = 1;
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //为用户显示所有支持的服务和特征值characteristics。
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
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

    Timer timer = new Timer();

    boolean send_hex = true;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.check_device);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


        sbValues = new StringBuffer();
        mHandler = new Handler();


        initView();
        setClickListener();


        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);


        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {

            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        boolean sg;
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        sg = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        updateConnectionState(R.string.connecting);

    }

    private void setClickListener() {

        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    if (mConnected) {
                        String LedStastr = "Sen1";
                        tx_count += mBluetoothLeService.txxx(LedStastr, send_hex);
                    }
                } else {
                    if (mConnected) {
                        String LedStastr = "Sen0";
                        tx_count += mBluetoothLeService.txxx(LedStastr, send_hex);
                    }
                }
            }
        });

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                // TODO Auto-generated method stub
                if (isChecked) {
                    if (mConnected) {
                        String LedStastr = "on";
                        tx_count += mBluetoothLeService.txxx(LedStastr, send_hex);
                    }
                } else {
                    if (mConnected) {
                        String LedStastr = "off";
                        tx_count += mBluetoothLeService.txxx(LedStastr, send_hex);
                    }
                }
            }
        });
    }

    private void initView() {
        sjj_connect_text_id = (TextView) findViewById(R.id.sjj_connect_text_id);
        switch1 = (Switch) findViewById(R.id.switch1);
        switch2 = (Switch) findViewById(R.id.switch2);
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (mBluetoothLeService != null) {
                    if (mConnected == false) {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService.disconnect();
        mBluetoothLeService = null;
        timer.cancel();
        timer = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
                sjj_connect_text_id.setText(resourceId);
            }
        });
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) return;

        if (gattServices.size() > 0 && mBluetoothLeService.get_connected_status(gattServices) == 2) {
            connect_count = 0;
            if (connect_status_bit) {
                mConnected = true;
                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(0);
                mBluetoothLeService.Delay_ms(100);
                mBluetoothLeService.enable_JDY_ble(1);
                mBluetoothLeService.Delay_ms(100);

                updateConnectionState(R.string.connected);
                mBluetoothLeService.txxx("+ok", send_hex);
            } else {
                //Toast.makeText(this, "Deleted Successfully!", Toast.LENGTH_LONG).show();
                Toast toast = Toast.makeText(CheckActivity.this, "提示！此设备不是KOI检测设备", Toast.LENGTH_SHORT);
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

}
