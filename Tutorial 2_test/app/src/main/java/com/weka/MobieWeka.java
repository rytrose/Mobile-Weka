package com.weka;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;

public class MobieWeka extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);	
        setContentView(R.layout.main);
    }
    
    public boolean dispatchTouchEvent(MotionEvent ev) {
		Intent intent = new Intent();
		Bundle bundle1 = new Bundle();					
		intent.putExtras(bundle1);
		intent.setClass(MobieWeka.this, MainMenu.class);
		startActivity(intent);
		return super.dispatchTouchEvent(ev);
	}
}