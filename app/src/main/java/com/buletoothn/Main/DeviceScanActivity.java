package com.buletoothn.Main;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.buletoothn.BleFunction.ChoseItemActivity;
import com.buletoothn.BleService.DeviceListAdapter;

import com.lee.buletoothn.R;

public  class DeviceScanActivity extends Activity
{
   // private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 5000;
    
    private DeviceListAdapter mDevListAdapter;
	ListView lv_bleList;
	byte dev_bid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_device);
        
        this.setTitle("BLE无线控制器");
        mHandler = new Handler();
        
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 如果本地蓝牙没有开启，则开启  
        if (!mBluetoothAdapter.isEnabled()) 
        {
            // 我们通过startActivityForResult()方法发起的Intent将会在onActivityResult()回调方法中获取用户的选择，比如用户单击了Yes开启，  
            // 那么将会收到RESULT_OK的结果，  
            // 如果RESULT_CANCELED则代表用户不愿意开启蓝牙  
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  
            startActivityForResult(mIntent, 1);  
            // 用enable()方法来开启，无需询问用户(实惠无声息的开启蓝牙设备),这时就需要用到android.permission.BLUETOOTH_ADMIN权限。  
            // mBluetoothAdapter.enable();  
            // mBluetoothAdapter.disable();//关闭蓝牙  
        }

        lv_bleList = (ListView) findViewById(R.id.lv_bleList);
		mDevListAdapter = new DeviceListAdapter( mBluetoothAdapter,DeviceScanActivity.this );
		dev_bid = (byte)0x88;//88 是厂家VID码
		mDevListAdapter.set_vid( dev_bid );//用于识别自家的VID相同的设备，只有模块的VID与APP的VID相同才会被搜索得到
		lv_bleList.setAdapter( mDevListAdapter.init_adapter( ) );
		lv_bleList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (mDevListAdapter.get_count() > 0) 
				{
					Byte vid_byte =  mDevListAdapter.get_vid( position );//返回136表示是JDY厂家模块
					if( vid_byte==dev_bid )//厂家VID为0X88， 用户的APP不想搜索到其它厂家的JDY-08模块的话，可以设备一下 APP的VID，此时模块也需要设置，
						                      //模块的VID与厂家APP的VID要一样，APP才可以搜索得到模块VID与APP一样的设备
					switch( mDevListAdapter.get_item_type(position) )
					{
						case JDY:////为标准透传模块
						{
							 BluetoothDevice device1 = mDevListAdapter.get_item_dev(position);
						        if (device1 == null) return;

							Intent intent1 = new Intent(DeviceScanActivity.this,ChoseItemActivity.class);;
							intent1.putExtra(ChoseItemActivity.EXTRAS_DEVICE_NAME, device1.getName());
							intent1.putExtra(ChoseItemActivity.EXTRAS_DEVICE_ADDRESS, device1.getAddress());
							// if (mScanning)
							{
								mDevListAdapter.scan_jdy_ble( false );;
								mScanning = false;
							}
							startActivity(intent1);
							break;
						}
						default:
							break;
					}
				}
			}
		});
		
		
      Message message = new Message();  
      message.what = 100;  
      handler.sendMessage(message);  

    }
    
    Handler handler = new Handler() {  
        public void handleMessage(Message msg) {  
        	if (msg.what == 100) {
            }
            super.handleMessage(msg);  
        }
		private void setTitle(String hdf) {
			// TODO 自动生成的方法存根
		};  
    }; 
    
    public static boolean turnOnBluetooth()
        {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null)
            {
                return bluetoothAdapter.enable();
            }
            return false;
        }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        menu.findItem(R.id.scan_menu_set).setVisible(true);
        menu.findItem(R.id.scan_menu_id).setActionView(null);

        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_menu_set1:
            {
            	mDevListAdapter.clear();
            	scanLeDevice( true );
            }
            break;
        }
        return true;
    }

	private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mDevListAdapter.scan_jdy_ble( false );
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mDevListAdapter.scan_jdy_ble( true );
        } else {
            mScanning = false;
            mDevListAdapter.scan_jdy_ble( false );
        }
    }

	@Override
	protected void onResume() {//打开APP时扫描设备
		super.onResume();
	//	scanLeDevice(true);
		//mDevListAdapter.scan_jdy_ble( false );
	}

	@Override
	protected void onPause() {//停止扫描
		super.onPause();
		mDevListAdapter.scan_jdy_ble( false );
	}


}