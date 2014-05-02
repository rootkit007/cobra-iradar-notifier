package com.greatnowhere.iradar;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

	private static SharedPreferences prefs;
	
	public static void init(Context ctx) {
		prefs = ctx.getSharedPreferences(Preferences.class.getCanonicalName(), Context.MODE_PRIVATE);
	}
	
	
}
