package com.greatnowhere.iradar.config;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;

public class SettingsActivity extends Activity {

	private AudioManager am;
	private static int maxAlarmVolume;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        maxAlarmVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public static int getMaxAlarmVolume() {
    	return maxAlarmVolume;
    }
}
