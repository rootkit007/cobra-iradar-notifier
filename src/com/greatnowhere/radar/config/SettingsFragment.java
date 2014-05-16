package com.greatnowhere.radar.config;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.greatnowhere.radar.R;

public class SettingsFragment extends PreferenceFragment {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        
        // Ensure current alert value does not exceed max alarm volume for this platform 
        // and set preferences slider max volume to appropriate value
        /*
        SeekBarPreference alertLevelPref = (SeekBarPreference) findPreference(getResources().getString(R.string.alert_level) );
        alertLevelPref.setRangeValues(1, SettingsActivity.getMaxAlarmVolume());
        if ( alertLevelPref.getPersistedValue() > SettingsActivity.getMaxAlarmVolume() ) {
        	alertLevelPref.setPersistedValue(SettingsActivity.getMaxAlarmVolume());
        }
        */
    }

}
