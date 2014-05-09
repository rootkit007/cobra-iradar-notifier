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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
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

import com.cobra.iradar.RadarManager;
import com.cobra.iradar.IRadarMessageHandler;
import com.cobra.iradar.RadarScanManager;
import com.cobra.iradar.messaging.CobraMessageAllClear;
import com.cobra.iradar.messaging.CobraMessageConnectivityNotification;
import com.cobra.iradar.messaging.CobraMessageNotification;
import com.cobra.iradar.messaging.CobraMessageThreat;
import com.cobra.iradar.messaging.ConnectivityStatus;
import com.cobra.iradar.protocol.RadarMessageAlert;
import com.cobra.iradar.protocol.RadarMessageAlert.Alert;
import com.greatnowhere.iradar.config.Preferences;
import com.greatnowhere.iradar.config.SettingsActivity;
import com.greatnowhere.iradar.location.LocationManager;
import com.greatnowhere.iradar.threats.AlertAudioManager;
import com.greatnowhere.iradar.threats.TTSManager;
import com.greatnowhere.iradar.threats.ThreatManager;

import de.greenrobot.event.EventBus;

/**
 * This is the main Activity that displays the radar status.
 */
public class CobraIRadarActivity extends Activity {
    // Debugging
    private static final String TAG = "CobraIRadarActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;
    
    public static final String INTENT_BACKGROUND = "runInBackground";

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    private NotificationManager mNotificationManager;
    
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
    private Timer timerVoltage;
    
    public static String LOG_FILE = "iradar.log";
    
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
        
        // Initialize alerts audio manager
        AlertAudioManager.init(getApplicationContext());
        
        // initialize threats manager
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ThreatManager.init(getApplicationContext());
        
        // Initialize TTS 
        TTSManager.init(getApplicationContext());
        
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
				System.exit(0);
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

    public void stopService() {
        RadarManager.stop();
        mNotificationManager.cancelAll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        stopService();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
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
    
    // The Handler that gets information back from the IRadar
    // All event handlers are called in async thread, so care must be taken when updating UI
	private IRadarMessageHandler radarMessageHandler = new IRadarMessageHandler() {

		@Override
		public void onRadarMessage(final CobraMessageConnectivityNotification msg) {
			rootView.post(new Runnable() {
				public void run() {
					addLogMessage(msg.message);
					connState.setText(msg.message);
					if ( msg.status == ConnectivityStatus.DISCONNECTED ) {
						voltage.setText("");
					}
				}
			});
		}

		@Override
		public void onRadarMessage(final CobraMessageThreat msg) {
			rootView.post(new Runnable() {
				public void run() {
		        	addLogMessage("Alert " + msg.alertType.getName() + msg.strength + " " + msg.frequency);
		        	alert.setText(msg.alertType.getName() + " " + msg.frequency);
		        	ThreatManager.newThreat(msg);
				}
			});
		}

		@Override
		public void onRadarMessage(final CobraMessageAllClear msg) {
			rootView.post(new Runnable() {
				public void run() {
					alert.setText("");
					ThreatManager.removeThreats();
				}
			});
		}

		@Override
		public void onRadarMessage(final CobraMessageNotification msg) {
			rootView.post(new Runnable() {
				public void run() {
					addLogMessage(msg.message);
				}
			});
		}
	};
	
	public void onEventAsync(final UIRefreshEvent event) {
		rootView.post(new Runnable() {
			public void run() {
				voltage.setText(Double.toString(radarMessageHandler.getBatteryVoltage()) + "V");
				String locInfo = "";
				if ( LocationManager.getCurrentLoc() != null ) {
					Location lastKnownLoc = LocationManager.getCurrentLoc();
					locInfo += ((System.currentTimeMillis() - lastKnownLoc.getTime())/1000L) + "s ";
					locInfo += (lastKnownLoc.getSpeed() * 3.6f) + "kph";
				}
				location.setText(( LocationManager.isReady() ? "Ready " + locInfo : "Not Ready" ) );
			}
		});
	}

    public void addLogMessage(String msg) {
    	// generate timestamp
    	String tsMsg = DateFormat.format("HH:mm:ss", new Date()).toString() + " " + msg + "\n";
    	log.setText(tsMsg + log.getText());
    	addFileLogMessage(tsMsg);
    }
    
    public void addFileLogMessage(String msg) {
    	try {
    		  File logFile = new File(
    				  Environment.getExternalStorageDirectory(), 
    				  LOG_FILE);
    		  OutputStream os = new FileOutputStream(logFile, true);
    		  os.write(msg.getBytes());
    		  os.close();
    	} catch (Exception e) {
    		  e.printStackTrace();
    	}
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                finish();
            }
        }
    }
    
    public void initialize() {
		// ongoing notification?
    	Builder b;
    	Notification scanNotification = null;
    	Notification connectedNotification = null;
    	Intent resumeAppIntent;
    	if ( Preferences.isNotifyOngoingScan() ) {
    		b = new Notification.Builder(getApplicationContext());
    		b.setContentText("iRadar Scanning For Devices");
    		resumeAppIntent = new Intent(getApplicationContext(), this.getClass());
    		resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
    		b.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resumeAppIntent , 0));
    		b.setContentTitle("iRadar Notifier");
    		b.setOngoing(true);
    		b.setSmallIcon(R.drawable.app_icon);
    		scanNotification = b.build();
    	}
    	if ( Preferences.isNotifyOngoingConnected() ) {
			b = new Notification.Builder(getApplicationContext());
			b.setContentText("iRadar Notifier Connected");
			resumeAppIntent = new Intent(getApplicationContext(), this.getClass());
			resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
			b.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resumeAppIntent , 0));
			b.setContentTitle("iRadar Notifier");
    		b.setOngoing(true);
			b.setSmallIcon(R.drawable.app_icon);
			connectedNotification = b.build();
    	}
		boolean success = RadarManager.initialize(getApplicationContext(), Preferences.isNotifyOngoingConnected(),
				connectedNotification, scanNotification, 
				Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly());
		
		if ( !success ) {
			addLogMessage("Error initializing iRadar: " + RadarManager.getLastError());
		} else {
			// set up voltage refresh every second
			if ( timerVoltage == null ) {
				timerVoltage = new Timer();
				timerVoltage.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						eventBus.post(new UIRefreshEvent());
					}
				}, 1000L, 1000L);
			}
		}
    }
    
    public void onEvent(Preferences.PreferenceScanChangedEvent event) {
    	RadarScanManager.scan(Preferences.isScanForDevice(), Preferences.getDeviceScanInterval(), Preferences.isScanForDeviceInCarModeOnly());
    }

    @Override
    public void finish() {
    	stop();
    	super.finish();
    }
    
    public void stop() {
    	RadarManager.stop();
    	ThreatManager.stop();
    	TTSManager.stop();
    	if ( timerVoltage != null )
    		timerVoltage.cancel();
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
    
    protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG, "onSaveInstanceState");
		CharSequence logText = log.getText();
		outState.putCharSequence("log", logText);
    }
    
    protected void onRestoreInstanceState(Bundle savedState) {		
		Log.i(TAG, "onRestoreInstanceState");
		log.setText(savedState.getCharSequence("log"));
    }
    
    public static class UIRefreshEvent {
    	
    }

}
