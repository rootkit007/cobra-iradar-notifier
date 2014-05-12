package com.greatnowhere.iradar.threats;

import android.app.Activity;
import android.os.Bundle;

public class ThreatActivity extends Activity {

	public static String INTENT_KEY = "threatActivityKey";
	
	public static int INTENT_SHOW_HUD = 1;
	public static int INTENT_ADD_THREAT = 2;
	public static int INTENT_REMOVE_HUD = 3;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    }
    
    public void onResume(Bundle savedInstanceState) {
    	switch (getIntent().getIntExtra(INTENT_KEY, 0)) {
    	
    	}
    }
	
}
