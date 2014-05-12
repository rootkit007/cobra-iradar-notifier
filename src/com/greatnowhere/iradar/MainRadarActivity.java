/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.greatnowhere.iradar;

import java.util.Random;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.cobra.iradar.protocol.RadarMessageAlert;
import com.cobra.iradar.protocol.RadarMessageAlert.Alert;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.config.SettingsActivity;
import com.greatnowhere.iradar.location.LocationManager;
import com.greatnowhere.iradar.services.CollectorService;

import de.greenrobot.event.EventBus;

/**
 * This is the main Activity that displays the radar status.
 */
public class MainRadarActivity extends Activity {
    // Debugging
    private static final String TAG = "CobraIRadarActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    
    public static final String INTENT_BACKGROUND = "runInBackground";
    
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    private View rootView;
    
    private TextView alert;
    private TextView connState;
    private TextView log;
    private TextView voltage;
    private TextView location;
    private Button btnReconnect;
    private Button btnFakeAlert;
    private Button btnFakeAlertMulti;
    private Button btnQuit;
    private Runnable uiRefreshRunnable;
    
    private EventBus eventBus = EventBus.getDefault();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main_radar_view);
        
        rootView = findViewById(R.id.mainViewLayout);
        
        eventBus.register(this);

        // set defaults for preferences
        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.settings, false);
        Preferences.init(getApplicationContext());
        
        log = (TextView) findViewById(R.id.logScroll);
        log.setMovementMethod(new ScrollingMovementMethod());
        alert = (TextView) findViewById(R.id.radarState);
        connState = (TextView) findViewById(R.id.connStatus);
        voltage = (TextView) findViewById(R.id.voltageText);
        btnReconnect = (Button) findViewById(R.id.btnReconnect);
        location = (TextView) findViewById(R.id.mainViewLocation);
        btnReconnect.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				initialize();
			}
		});
        btnFakeAlert = (Button) findViewById(R.id.btnFakeAlert);
        btnFakeAlert.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				Random r = new Random();
				eventBus.post(new RadarMessageAlert(Alert.Ka, r.nextInt(4) + 1, 35.1f, 3000L));
			}
		});
        btnFakeAlertMulti = (Button) findViewById(R.id.btnFakeAlertMulti);
        btnFakeAlertMulti.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				Random r = new Random();
				eventBus.post(new RadarMessageAlert(Alert.Ka, r.nextInt(4) + 1, 35.1f, 3000L));
				eventBus.post(new RadarMessageAlert(Alert.K, r.nextInt(4) + 1, 35.1f, 3000L));
			}
		});
        btnQuit = (Button) findViewById(R.id.btnQuit);
        btnQuit.setOnClickListener( new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
           	initialize();
           	if ( getIntent().getBooleanExtra(INTENT_BACKGROUND, false)) {
           		goHome();
           	}
        }
    }
    
    public void onResume() {
        // keep screen on
        if ( Preferences.isKeepScreenOnForeground() )
        	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else 
        	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onResume();
    }

    @Override
    public void onBackPressed() {
       Log.d(TAG, "onBackPressed Called");
       goHome();
    }
    
    public void goHome() {
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
    }
    
	
    public void initialize() {

        // Start data collector service
        getApplicationContext().startService(new Intent(getApplicationContext(), CollectorService.class));
        
    	if ( uiRefreshRunnable != null ) {
    		rootView.removeCallbacks(uiRefreshRunnable);
    	}
    	uiRefreshRunnable = new Runnable() {
			public void run() {
				eventBus.post(new UIRefreshEvent());
		    	rootView.postDelayed(uiRefreshRunnable,(1000L / Preferences.getScreenRefreshFrequency()));
			};
    	};
    	rootView.postDelayed(uiRefreshRunnable,(1000L / Preferences.getScreenRefreshFrequency()));
    	
    }
    
    @Override
    public void finish() {
    	stop();
    	super.finish();
    }
    
    public void stop() {
    	if ( uiRefreshRunnable != null ) {
    		rootView.removeCallbacks(uiRefreshRunnable);
    		uiRefreshRunnable = null;
    	}
    	boolean serviceStopped = getApplicationContext().stopService(new Intent(getApplicationContext(), CollectorService.class));
    	Log.d(TAG, "service " + serviceStopped);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch ( item.getItemId() ) {
    	case R.id.itemSettings:
    		Intent settingsIntent = new Intent(getApplicationContext(),SettingsActivity.class);
    		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		getApplicationContext().startActivity(settingsIntent);
    		return true;
    	case R.id.itemQuit:
    		finish();
    		return true;
    	default:
            return false;
    	}
    	
    }
    
    /**
     * Refresh UI thingamajjigs
     * @param event
     */
    public void onEventMainThread(UIRefreshEvent event) {
    	log.setText(CollectorService.getLog());
    	voltage.setText(CollectorService.getBatteryVoltage());
    	alert.setText(CollectorService.getLastAlert());
    	connState.setText(CollectorService.getConnStatus());
		String locInfo = "";
		if ( LocationManager.getCurrentLoc() != null ) {
			Location lastKnownLoc = LocationManager.getCurrentLoc();
			locInfo += ((System.currentTimeMillis() - lastKnownLoc.getTime())/1000L) + "s ";
			locInfo += (lastKnownLoc.getSpeed() * 3.6f) + "kph";
		}
		location.setText(( LocationManager.isReady() ? "Ready " + locInfo : "Not Ready" ) );
    }
    
    public static class UIRefreshEvent {
    }
    
}
