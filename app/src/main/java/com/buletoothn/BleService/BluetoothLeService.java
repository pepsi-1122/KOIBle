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

package com.buletoothn.BleService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.lee.buletoothn.R;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */



public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DATA_AVAILABLE1 =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE1";
    
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String EXTRA_DATA1 =
            "com.example.bluetooth.le.EXTRA_DATA1";
    
    public final static String EXTRA_UUID =
            "com.example.bluetooth.le.uuid_DATA";
    public final static String EXTRA_NAME =
            "com.example.bluetooth.le.name_DATA";
    public final static String EXTRA_PASSWORD =
            "com.example.bluetooth.le.password_DATA";
    
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    
    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    
    public static String Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_FUNCTION = "0000ffe2-0000-1000-8000-00805f9b34fb";
	
    
    //int tx_cnt = 1;
    byte tx_cnt = (byte)0x01;
    public enum function_type {

    	name,
    	
    }

    public  String bin2hex(String bin) {
        char[] digital = "0123456789ABCDEF".toCharArray();
        StringBuffer sb = new StringBuffer("");
        byte[] bs = bin.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(digital[bit]);
            bit = bs[i] & 0x0f;
            sb.append(digital[bit]);
        }
        return sb.toString();
    }
    public  byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("���Ȳ���ż��");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }
    
    
    void deley( int ms )
    {
    	try {  
            Thread.currentThread();  
            Thread.sleep( ms );  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }
    }

	public  String getStringByBytes(byte[] bytes) {
		String result = null;
		String hex = null;
		if (bytes != null && bytes.length > 0) {
			final StringBuilder stringBuilder = new StringBuilder(bytes.length);
			for (byte byteChar : bytes) {
				hex = Integer.toHexString(byteChar & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
				stringBuilder.append(hex.toUpperCase());
			}
			result = stringBuilder.toString();
		}
		return result;
	}

	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}

	public static byte[] getBytesByString(String data) //
	{
		byte[] bytes = null;
		if (data != null) {
			data = data.toUpperCase();
			int length = data.length() / 2;
			char[] dataChars = data.toCharArray();
			bytes = new byte[length];
			for (int i = 0; i < length; i++) {
				int pos = i * 2;
				bytes[i] = (byte) (charToByte(dataChars[pos]) << 4 | charToByte(dataChars[pos + 1]));
			}
		}
		return bytes;
	}
    public String bytesToHexString(byte[] src)//����byte[]��0x11,0x22,0x33,0x44,0x55,0x66  ת����String:��112233445566��
    {  
      	 StringBuilder stringBuilder = new StringBuilder(src.length);
           for(byte byteChar : src)
              stringBuilder.append(String.format("%02X", byteChar));
          return stringBuilder.toString();  
      } 
    public String bytesToHexString1(byte[] src)//����byte[]��0x11,0x22,0x33,0x44,0x55,0x66  ת����String:��11 22 33 44 55 66��
    {  
      	 StringBuilder stringBuilder = new StringBuilder(src.length);
           for(byte byteChar : src)
              stringBuilder.append(String.format(" %02X", byteChar));
          return stringBuilder.toString();  
      }
	
    byte[] WriteBytes = new byte[200];
	public int txxx(String g ,boolean string_or_hex_data ){
		int ic=0;
		//g=""+g;
		if( string_or_hex_data )WriteBytes= g.getBytes();//getBytesByString( g );//  hex2byte(g.toString().getBytes());
		else WriteBytes= getBytesByString( g );
		int length = WriteBytes.length;
		int data_len_20 = length/20;
		int data_len_0 = length%20;

		int i=0;
		if( data_len_20>0 )
		{
			for( ;i<data_len_20;i++ )
			{
				byte[] da = new byte[20];
				for( int h=0;h<20;h++ )
				{
					da[h] = WriteBytes[ 20*i+h];
					//Log.d("20*i+h"," len = " + 20*i+h );
				}
				BluetoothGattCharacteristic gg;
				gg=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
				gg.setValue(da);
				mBluetoothGatt.writeCharacteristic(gg);
				deley(23);
				ic +=20;
			}
		}
		if( data_len_0>0 )
		{
			byte[] da = new byte[data_len_0];
			for( int h=0;h<data_len_0;h++ )
			{
				da[h] = WriteBytes[ 20*i+h];
			}
			BluetoothGattCharacteristic gg;
			gg=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
			gg.setValue(da);
			mBluetoothGatt.writeCharacteristic(gg);
			ic +=data_len_0;
			deley(23);
		}

		return ic;
	}
	public void function_data( byte []data  )
	{
    	WriteBytes= data;
    	BluetoothGattCharacteristic gg;
		gg=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
		//byte t[]={51,1,2};
		gg.setValue(WriteBytes);
		mBluetoothGatt.writeCharacteristic(gg);
	}
    public void enable_JDY_ble( int p ){
    	try {
		//if( p )
	    {
			BluetoothGattService service =mBluetoothGatt.getService(UUID.fromString(Service_uuid));
			BluetoothGattCharacteristic ale;// =service.getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
			switch( p )
			{
				case 0://0xFFE1 //͸��
				{
					ale =service.getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
				}break;
				case 1:// 0xFFE2 //iBeacon_UUID
				{
					ale =service.getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
				}break;
				default:
					ale =service.getCharacteristic(UUID.fromString(Characteristic_uuid_TX));
					break;
			} 
			boolean set = mBluetoothGatt.setCharacteristicNotification(ale, true);
			BluetoothGattDescriptor dsc =ale.getDescriptor(UUID.fromString(  "00002902-0000-1000-8000-00805f9b34fb"));
			byte[]bytes = {0x01,0x00};
			dsc.setValue(bytes);
			boolean success =mBluetoothGatt.writeDescriptor(dsc);
			//Log.d(TAG, "writing enabledescriptor:" + success);
	    }

//        	jdy=mBluetoothGatt.getService(UUID.fromString(Service_uuid)).getCharacteristic(UUID.fromString(Characteristic_uuid_FUNCTION));
//        	mBluetoothGatt.setCharacteristicNotification(jdy, p);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
    }
    public void Delay_ms( int ms )
    {
		 try {  
	            Thread.currentThread();  
	            Thread.sleep( ms );  
	        } catch (InterruptedException e) {  
	            e.printStackTrace();  
	        } 
    }

    
    public int get_connected_status( List<BluetoothGattService> gattServices )
    {
    	int jdy_ble_server = 0;
    	int jdy_ble_ffe1 = 0;
    	int jdy_ble_ffe2 = 0;
    	
        final String LIST_NAME1 = "NAME";
        final String LIST_UUID1 = "UUID";
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        //mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        
        int count_char = 0;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME1, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID1, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            //Log.d("server_uuid", uuid );
            if( Service_uuid.equals( uuid ) )
            {
            	//Log.d("server_uuid", "jdy ble" );
            	jdy_ble_server = 1;
            }
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME1, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID1, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                count_char++;
                
                if( jdy_ble_server==1 )
                {
                	//Log.d("Characteristic_uuid", uuid );
                	if( Characteristic_uuid_TX.equals( uuid ) )jdy_ble_ffe1=1;
                	else if( Characteristic_uuid_FUNCTION.equals( uuid ) )jdy_ble_ffe2=1;
                }
            }
            //mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
        if( jdy_ble_ffe1==1&&jdy_ble_ffe2==1 )return 2;//JDY-06,JDY-06
        else if( jdy_ble_ffe1==1&&jdy_ble_ffe2==0 )return 1;//JDY-09,JDY-10
        else return 0;
        //return count_char;
    }
    
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	if( UUID.fromString(Characteristic_uuid_TX).equals(characteristic.getUuid()) )
            	{
            		broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            	}
            	else if( UUID.fromString(Characteristic_uuid_FUNCTION).equals(characteristic.getUuid()) )
            	{
            		broadcastUpdate(ACTION_DATA_AVAILABLE1, characteristic);
            	}
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        	if( UUID.fromString(Characteristic_uuid_TX).equals(characteristic.getUuid()) )
        	{
        		broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        	}
        	else if( UUID.fromString(Characteristic_uuid_FUNCTION).equals(characteristic.getUuid()) )
        	{
        		broadcastUpdate(ACTION_DATA_AVAILABLE1, characteristic);
        	}
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        Log.d("getUuid"," len = " + characteristic.getUuid() );

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
              //  Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
               // Log.d(TAG, "Heart rate format UINT8.");
            }
            //final int heartRate = characteristic.getIntValue(format, 1);
            //Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            //intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }
        else if( UUID.fromString(Characteristic_uuid_TX).equals(characteristic.getUuid()) )
        {
        	//Log.d("Characteristic_uuid_TX", ""+characteristic.getUuid());
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) 
            {
               // final StringBuilder stringBuilder = new StringBuilder(data.length);
               // for(byte byteChar : data)
                  // stringBuilder.append(String.format("%02X", byteChar));
                
                intent.putExtra(EXTRA_DATA, data );// stringBuilder.toString());
            }
        }
        else if( UUID.fromString(Characteristic_uuid_FUNCTION).equals(characteristic.getUuid()) )
        {
        	//Log.i(TAG, "8");
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(data.length);
//                for(byte byteChar : data)
//                   stringBuilder.append(String.format("%02X", byteChar));
//                intent.putExtra(EXTRA_DATA,stringBuilder.toString());
            	intent.putExtra(EXTRA_DATA1, data );
            }
        }
        
        
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        
        mBluetoothGatt.disconnect();
    }
    public boolean isconnect() {
       
        
       return mBluetoothGatt.connect();
    }
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
