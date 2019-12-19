package com.buletoothn.DeviceType;

import android.util.Log;

public class Get_type {

	
	public JDY_type dv_type( byte[] p )
	{
		
		Log.d( "scan_byte_len",""+ p.length);
		String str;
		
		str = String.format( "%02x", p[5] );
		Log.d( "scan_byte_bit_0=",""+ str);
		str = String.format( "%02x", p[6] );
		Log.d( "scan_byte_bit_0=",""+ str);
		
		str = String.format( "%02x", p[11] );
		Log.d( "scan_byte_bit_0=",""+ str);
		str = String.format( "%02x", p[12] );
		Log.d( "scan_byte_bit_0=",""+ str);
		byte m1 = (byte)((p[18]+1)^0x11);
		str = String.format( "%02x", m1 );
		Log.d( "scan_byte_bit_0=",""+ str);
		
		byte m2 = (byte)((p[17]+1)^0x22);
		str = String.format( "%02x", m2 );
		Log.d( "scan_byte_bit_0=",""+ str);
		
		
		if( p[5]==(byte)0xe0 && p[6]==(byte)0xff &&p[11]==m1&&p[12]==m2 )
		{
			return JDY_type.JDY;
		}
		else  return JDY_type.UNKW;
	}
	
}
