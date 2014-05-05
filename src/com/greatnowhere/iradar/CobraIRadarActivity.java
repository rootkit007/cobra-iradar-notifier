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
import android.os.Bundle;
import android.os.Environment;
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

import com.cobra.iradar.IRadarManager;
import com.cobra.iradar.IRadarMessageHandler;
import com.cobra.iradar.messaging.CobraMessageAllClear;
import com.cobra.iradar.messaging.CobraMessageConnectivityNotification;
import com.cobra.iradar.messaging.CobraMessageNotification;
import com.cobra.iradar.messaging.CobraMessageThreat;
import com.cobra.iradar.protocol.RadarMessageAlert.Alert;

/**
 * This is the main Activity that displays the radar status.
 */
public class CobraIRadarActivity extends Activity {
    // Debugging
    private static final String TAG = "CobraIRadarActivity";
    private static final boolean D = true;

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    private NotificationManager mNotificationManager;
    
    private TextView alert;
    private TextView connState;
    private TextView log;
    private Button btnReconnect;
    private Button btnFakeAlert;
    private Button btnQuit;
    
    public static String LOG_FILE = "iradar.log";
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main_radar_view);
        
        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        log = (TextView) findViewById(R.id.logScroll);
        log.setMovementMethod(new ScrollingMovementMethod());
        alert = (TextView) findViewById(R.id.radarState);
        connState = (TextView) findViewById(R.id.connStatus);
        btnReconnect = (Button) findViewById(R.id.btnReconnect);
        btnReconnect.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				initialize();
			}
		});
        btnFakeAlert = (Button) findViewById(R.id.btnFakeAlert);
        btnFakeAlert.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				radarMessageHandler.onRadarMessage(new CobraMessageThreat(Alert.Ka, 2, 35.1f));
			}
		});
        btnQuit = (Button) findViewById(R.id.btnQuit);
        btnQuit.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

        
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Threat.init(getApplicationContext());

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
           	initialize();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

    }


    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }
    
    public void stopService() {
        IRadarManager.stop();
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
       Intent setIntent = new Intent(Intent.ACTION_MAIN);
       setIntent.addCategory(Intent.CATEGORY_HOME);
       setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       startActivity(setIntent);
    }
    
    // The Handler that gets information back from the IRadar
    private IRadarMessageHandler radarMessageHandler = new IRadarMessageHandler() {

		@Override
		public void onRadarMessage(CobraMessageConnectivityNotification msg) {
			addLogMessage(msg.message);
			connState.setText(msg.message);
		}

		@Override
		public void onRadarMessage(CobraMessageThreat msg) {
        	addLogMessage("Alert " + msg.alertType.getName() + msg.strength + " " + msg.frequency);
        	alert.setText(msg.alertType.getName() + msg.strength + "\n" + msg.frequency + " " + msg.alertType.getAdditionalName());
        	Timer t = new Timer(true);
        	t.schedule(new TimerTask() {
				@Override
				public void run() {
					Threat.removeThreats();
				}
			}, 1000L);
        	Threat.newThreat(msg);
		}

		@Override
		public void onRadarMessage(CobraMessageAllClear msg) {
			Threat.removeThreats();
		}

		@Override
		public void onRadarMessage(CobraMessageNotification msg) {
			addLogMessage(msg.message);
		}
	};

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
		boolean success = IRadarManager.initialize(getApplicationContext(), true, CobraIRadarActivity.class, true, 60);
		// start ongoing notification
		/*
		Builder b = new Notification.Builder(getApplicationContext());
		b.setContentText("iRadar Notifier Running");
		Intent resumeAppIntent = new Intent(getApplicationContext(), this.getClass());
		resumeAppIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP );
		b.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resumeAppIntent , 0));
		b.setContentTitle("iRadar Notifier");
		b.setSmallIcon(R.drawable.app_icon);
		mNotificationManager.notify(null, 1, b.build());
		*/
    }

    @Override
    public void finish() {
    	IRadarManager.stop();
    	super.finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
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

}
