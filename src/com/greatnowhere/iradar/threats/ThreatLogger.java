package com.greatnowhere.iradar.threats;

import com.greatnowhere.iradar.CobraApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

public class ThreatLogger extends SQLiteOpenHelper {

	private static final String DB_NAME = CobraApplication.class.getCanonicalName();
	private static final int DB_VERSION = 1;
	
	private static ThreatLogger instance;
	
	public static void init(Context ctx) {
		instance = new ThreatLogger(ctx, DB_NAME, null, DB_VERSION);
	}
	
	private ThreatLogger(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	public static void logThreat(ThreatManager.Threat threat) {
		SQLiteDatabase db = instance.getWritableDatabase(); 
		db.beginTransaction();
		ContentValues v = new ContentValues();
		v.put("type", threat.alert.alertType.getCode());
		v.put("freq", threat.alert.frequency);
		v.put("timestamp", threat.startTimeMillis );
		long threatId = db.insert("threats", null, v);
		if ( threat.locations != null ) {
			for ( Location l : threat.locations ) {
				v = new ContentValues();
				v.put("threat_id", threatId);
				v.put("lat", l.getLatitude());
				v.put("long", l.getLongitude());
				v.put("ts", l.getTime());
				db.insert("threats_loc", null, v);
			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}
	
	public static int countSimilarThreatOccurences(ThreatManager.Threat threat) {
		return 0;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table threats(id integer primary key autoincrement, type integer, freq real, timestamp integer);");
		db.execSQL("create table threats_loc(id integer primary key autoincrement, threat_id integer, lat real, long real, ts integer,"
				+ "foreign key(threat_id) references threats(id));");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
}
