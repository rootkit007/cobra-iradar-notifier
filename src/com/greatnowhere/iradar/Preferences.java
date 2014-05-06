package com.greatnowhere.iradar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class Preferences {

	private static SharedPreferences prefs;
	private static Resources res;
	
	public static void init(Context ctx) {
		prefs = ctx.getSharedPreferences(Preferences.class.getCanonicalName(), Context.MODE_PRIVATE);
		res = ctx.getResources();
	}
	
	public static boolean isSetAlertLevel() {
		return prefs.getBoolean(res.getString(R.string.alert_level_set_flag),true); 
	}
	
	public static int getAlertLevel() {
		return prefs.getInt(res.getString(R.string.alert_level), 1);
	}
	
}
