package com.buletoothn.BleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

//import com.example.jdy_type.Get_type;
import com.buletoothn.DeviceType.JDY_type;
import com.lee.buletoothn.R;

public class DeviceListAdapter extends Activity
{
	int list_select_index = 0;
	
//	Get_type mGet_type;
  private	DeviceListAdapter1 list_cell_0;
	BluetoothAdapter apter;
	Context context;
	
	int scan_int = 0;

		Timer timer = new Timer();  
		boolean stop_timer = true;
		
		byte dev_VID = (byte)0x88;
		
		public JDY_type dv_type( byte[] p )
		{
			//Log.d( "out_3=","scan_byte_len:"+ p.length);
			if( p.length!=62 )return null;
			//if( p.length!=0 )return null;
			String str;

			byte m1 = (byte)((p[18+2]+1)^0x11);
			str = String.format( "%02x", m1 );
			//Log.d( "out_1","="+ str);
			
			byte m2 = (byte)((p[17+2]+1)^0x22);
			str = String.format( "%02x", m2 );
			//Log.d( "out_2","="+ str);

			int ib1_major=0;
			int ib1_minor=0;
			if( p[52]==(byte)0xff )
			{
				if( p[53]==(byte)0xff )ib1_major=1;
			}
			if( p[54]==(byte)0xff )
			{
				if( p[55]==(byte)0xff )ib1_minor=1;
			}
			if( p[5]==(byte)0xe0 && p[6]==(byte)0xff &&p[11]==m1&&p[12]==m2 &&(dev_VID==p[19-6])  )//JDY
			{
				 byte[] WriteBytes = new byte[4];
				 WriteBytes[0]=p[19-6];
				 WriteBytes[1]=p[20-6];
				Log.d( "out_1","TC"+list_cell_0.bytesToHexString1( WriteBytes ) );
				if( p[20-6]==(byte)0xa0 )return JDY_type.JDY;
				return JDY_type.JDY;
			}
			else {
				return JDY_type.UNKW;
			}
		}
	
	
	public DeviceListAdapter( BluetoothAdapter adapter,Context context1 )
	{
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
		apter = adapter;
		context = context1;
        list_cell_0 = new DeviceListAdapter1();
        timer.schedule(task, 1000, 1000);
	}
	  Handler handler = new Handler() {  
	        public void handleMessage(Message msg) {  
	            if (msg.what == 1&&stop_timer) 
	            {  
	                //tvShow.setText(Integer.toString(i++));  
	            	loop_list();
//	            	Log.d( "out_1","time run" );
	            }  
	            super.handleMessage(msg);  
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
	

	public DeviceListAdapter1 init_adapter( )
	{
		
		return list_cell_0;
	}
	public BluetoothDevice get_item_dev( int pos )
	{
		return list_cell_0.dev_ble.get( pos );
	}
	
	public JDY_type get_item_type( int pos )
	{
		return list_cell_0.dev_type.get( pos );
	}
	public int get_count( )
	{
		return list_cell_0.getCount();
	}

	public byte get_vid( int pos )
	{
		return (byte) list_cell_0.get_vid(pos);
	}
	public void set_vid( byte vid )
	{
		dev_VID = vid;
	}

	public void loop_list(  )
	{
		list_cell_0.loop();
	}
	
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) 
        {
        	scan_int++;
        	if( scan_int>1 )
        	{
        		scan_int = 0;
	            if (Looper.myLooper() == Looper.getMainLooper()) 
	            {
	            	JDY_type m_tyep = dv_type( scanRecord  );
	            	if( m_tyep!=JDY_type.UNKW && m_tyep!=null )
	            	{
	            		list_cell_0.addDevice(device,scanRecord,rssi,m_tyep );
	            		//mDevListAdapter.notifyDataSetChanged();
	            		list_cell_0.notifyDataSetChanged();
	            	}
	            }
	            else 
	            {
	                runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                    	JDY_type m_tyep = dv_type( scanRecord  );
	    	            	if( m_tyep!=JDY_type.UNKW && m_tyep!=null )
	    	            	{
	    	            		list_cell_0.addDevice(device,scanRecord,rssi,m_tyep );
	    	            		//mDevListAdapter.notifyDataSetChanged();
	    	            		list_cell_0.notifyDataSetChanged();
	    	            	}
	                    }
	                });
	            }
        	}
        }
    };// public void addDevice(BluetoothDevice device,byte[] scanRecord,Integer  RSSI,JDY_type type ) 

    public void stop_flash( )
    {
    	stop_timer = false;
    }
    public void start_flash( )
    {
    	stop_timer = true;
    }
    
	public void clear()
	{
		list_cell_0.clear();
	}
	public void scan_jdy_ble( Boolean p )//ɨ��BLE����
	{
		if( p )
		{
			list_cell_0.notifyDataSetChanged();
			apter.startLeScan( mLeScanCallback );
			start_flash();
		}
		else 
		{
			apter.stopLeScan( mLeScanCallback );
			stop_flash();
		}
	}

	class DeviceListAdapter1 extends BaseAdapter 
	{
		private List<BluetoothDevice> dev_ble;
		private List<JDY_type>dev_type;
		private List<byte[]> dev_scan_data;
		private List<Integer> dev_rssi;
		private List<Integer> remove;
		
		private ViewHolder viewHolder;
		int count = 0;
		int ip = 0;
		
		public DeviceListAdapter1() {
			dev_ble = new ArrayList<BluetoothDevice>();
			dev_scan_data = new ArrayList<byte[]>();
			dev_rssi = new ArrayList<Integer>();
			dev_type = new ArrayList<JDY_type>();
			remove = new ArrayList<Integer>();
		}
		
		public void loop()
		{
			if( remove!=null&&remove.size()>0&&ip==0 )
			{
				
				if( count>=remove.size() )
				{
					count = 0;
				}
				Integer it = remove.get( count );
				if( it>=3 )
				{
					dev_ble.remove(count);
					dev_scan_data.remove(count);
					dev_rssi.remove(count);
					dev_type.remove(count);
					remove.remove(count);
					notifyDataSetChanged();
				}
				else
				{
					it++;
					remove.add(count+1, it);
					remove.remove(count);
				}
				count++;
				
			}
		}
		public void addDevice(BluetoothDevice device,byte[] scanRecord,Integer  RSSI,JDY_type type ) 
		{
			ip = 1;
			if (!dev_ble.contains(device)) 
			{
				dev_ble.add(device);
				dev_scan_data.add(scanRecord);
				dev_rssi.add(RSSI);
				dev_type.add(type);
				Integer it =0;
				remove.add(it);
			}
			else
			{
				for(int i=0;i<dev_ble.size();i++)
				{
					String btAddress = dev_ble.get(i).getAddress();
					if(btAddress.equals(device.getAddress()))
					{
						dev_ble.add(i+1, device);
						dev_ble.remove(i);
						
						dev_scan_data.add(i+1, scanRecord);
						dev_scan_data.remove(i);
						
						dev_rssi.add(i+1, RSSI);
						dev_rssi.remove(i);
						
						dev_type.add(i+1, type);
						dev_type.remove(i);
						
						Integer it =0;// remove.get( i );
						remove.add(i+1, it);
						remove.remove(i);

					}
				}
			}
			notifyDataSetChanged();
			ip = 0;
		}

		public void clear(){
			dev_ble.clear();
			dev_scan_data.clear();
			dev_rssi.clear();
			dev_type.clear();
			remove.clear();
		}

		@Override
		public int getCount() {
			return dev_ble.size();
		}

		@Override
		public BluetoothDevice getItem(int position) {
			return dev_ble.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
			}
			if( position<=dev_ble.size()  ) {
				JDY_type type_0 = dev_type.get( position );
				if( type_0==JDY_type.JDY ) {

						convertView = LayoutInflater.from( context ).inflate(R.layout.listitem_device, null);
						viewHolder = new ViewHolder();
						viewHolder.tv_devName = (TextView) convertView.findViewById(R.id.device_name);
						viewHolder.tv_devAddress = (TextView) convertView.findViewById(R.id.device_address);
						viewHolder.device_rssi = (TextView) convertView.findViewById(R.id.device_rssi);
						viewHolder.scan_data = (TextView) convertView.findViewById(R.id.scan_data);
						viewHolder.type0 = (TextView) convertView.findViewById(R.id.type0);
						convertView.setTag(viewHolder);

					list_select_index=1;

					BluetoothDevice device = dev_ble.get(position);
					String devName = device.getName();
					devName = "Name:"+devName;
					if( viewHolder.tv_devName!=null )
						viewHolder.tv_devName.setText(devName);

					String mac = device.getAddress();
					mac = "MAC:"+mac;
					if( viewHolder.tv_devAddress!=null )
						viewHolder.tv_devAddress.setText( mac );
					
					String rssi_00 = ""+dev_rssi.get( position );
					rssi_00 = "RSSI:-"+rssi_00;
					if( viewHolder.device_rssi!=null )
						viewHolder.device_rssi.setText( rssi_00 );
					
					String tp = null;
					tp = "Type:" + "Standard model";
					if( viewHolder.type0!=null )
						viewHolder.type0.setText( tp );
					
					if( viewHolder.scan_data!=null )
						viewHolder.scan_data.setText( "scanRecord:"+bytesToHexString1(dev_scan_data.get( position )) );
					
				}else{
				}
				return convertView;
			}return null;
			
		}

		public int get_vid( int pos )
		{
			String vid=null;
			byte[] byte1000=(byte[])dev_scan_data.get( pos );
			byte[] result = new byte[4];  
			result[0]=0X00;
			result[1]=0X00;
			result[2]=0X00;
			JDY_type tp = dev_type.get( pos );
			if( tp==JDY_type.JDY)
			{
				result[3]=byte1000[19-6];
			}
			else 
			{
				result[3]=byte1000[56];
			}
			
			int ii100 = byteArrayToInt1(result);
			//vid=String.valueOf(ii100);
			return ii100;
		}

		  public int byteArrayToInt1(byte[] bytes) {
              int value= 0;
              for (int i = 0; i < 4; i++) {
                  int shift= (4 - 1 - i) * 8;
                  value +=(bytes[i] & 0x000000FF) << shift;//����λ��
              }
              return value;
        }
		  
		    private String bytesToHexString(byte[] src){  
		    	 StringBuilder stringBuilder = new StringBuilder(src.length);
	             for(byte byteChar : src)
	                stringBuilder.append(String.format("%02X", byteChar));
		        return stringBuilder.toString();  
		    }  
		
	    private String bytesToHexString1(byte[] src){  
	    	 StringBuilder stringBuilder = new StringBuilder(src.length);
             for(byte byteChar : src)
                stringBuilder.append(String.format(" %02X", byteChar));
	        return stringBuilder.toString();  
	    }
	}

	class ViewHolder {
		TextView tv_devName, tv_devAddress,device_rssi,type0,scan_data;
	}

}

