package com.buletoothn.set;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.lee.buletoothn.R;



public class set extends Activity
{

  TextView textView11;
  String resultStr = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.set );
        setTitle("说明");
        ActionBar actionBar = getActionBar();  
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    Handler handler = new Handler() {  
        public void handleMessage(Message msg) {  
        	if (msg.what == 1) 
        	{
            }
            super.handleMessage(msg);  
        };  
    };
	public String getSharedPreference(String key) 
	{
		SharedPreferences sharedPreferences= getSharedPreferences("test", 
		Activity.MODE_PRIVATE); 

		String name =sharedPreferences.getString(key, ""); 
		return name;
	}

	public void setSharedPreference(String key, String values) 
	{

		SharedPreferences mySharedPreferences= getSharedPreferences("test", 
		Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = mySharedPreferences.edit();
		editor.putString(key, values );
		editor.commit(); 

	}
    
    
	@Override
	protected void onResume() {
		super.onResume();

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.set_menu, menu);
    	menu.findItem(R.id.set_menu).setVisible(true);
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
    

}
