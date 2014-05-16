package com.greatnowhere.radar.threats;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.Parcelable;
import android.util.Log;

import com.greatnowhere.radar.MainRadarApplication;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.messaging.RadarMessageThreat;
import com.greatnowhere.radar.threats.ThreatManager.ThreatCredibility;

public class ThreatLogger extends SQLiteOpenHelper {

	private static final String DB_NAME = MainRadarApplication.class.getCanonicalName();
	private static final int DB_VERSION = 4;
	
	private static final String TAG = ThreatLogger.class.getCanonicalName();
	
	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "long";
	private static final String COSLAT = "coslat";
	private static final String SINLAT = "sinlat";
	private static final String COSLNG = "coslong";
	private static final String SINLNG = "sinlong";
	private static final double EARTH_R = 6371d;
	
	private static ThreatLogger instance;
	private static Context ctx;
	private static AlarmManager alarmManager;
	private static PendingIntent logCleanupIntent;
	
	public static synchronized void init(Context ctx) {
		instance = new ThreatLogger(ctx, DB_NAME, null, DB_VERSION);
		alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
		ThreatLogger.ctx = ctx;
		
		if ( Preferences.isLogThreats() && Preferences.isLogThreatLimitNumeric() ) {
			Intent i = new Intent(ctx, DBPruneService.class);
			logCleanupIntent = PendingIntent.getService(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3600000L, AlarmManager.INTERVAL_DAY, logCleanupIntent);
		} 
	}
	
	private ThreatLogger(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	public static void logThreat(Threat threat) {
		Intent i = new Intent(ctx, ThreatLoggerService.class);
		i.putExtra(ThreatLoggerService.KEY_BUNDLE_ALERT, threat.alert);
		if ( threat.locations != null )
			i.putParcelableArrayListExtra(ThreatLoggerService.KEY_BUNDLE_LOCATIONS, new ArrayList<Parcelable>(threat.locations));
		i.putExtra(ThreatLoggerService.KEY_BUNDLE_START_TIME, threat.startTimeMillis);
		i.putExtra(ThreatLoggerService.KEY_BUNDLE_END_TIME, threat.endTimeMillis);
		i.putExtra(ThreatLoggerService.KEY_BUNDLE_CREDIBILITY, threat.credibility);
		ctx.startService(i);
	}
	
	public static int countSimilarThreatOccurences(RadarMessageThreat threat, Location loc, float radius) {
		Threat t = new Threat();
		t.locations = new LinkedHashSet<Location>();
		t.locations.add(loc);
		t.alert = threat;
		return countSimilarThreatOccurences(t, radius);
	}
	
	/**
	 * Counts threats in database with the same alert type, frequency and within "radius" of the location
	 * @param threat
	 * @param radius in km
	 * @return
	 */
	public static int countSimilarThreatOccurences(Threat threat, float radius) {
		SQLiteDatabase db = instance.getReadableDatabase();
		Set<Integer> threat_ids = new LinkedHashSet<Integer>();
		String partialDistance = Double.toString(convertKmToPartialDistance(radius));
		Log.d(TAG, "radius " + radius + " part dist " + partialDistance);
		if ( threat.locations != null ) {
			for ( Location l : threat.locations ) {
				String sql = "select distinct id from threats where type=? and abs(?-freq)<0.05 and exists (select * from threats_locations "
						+ "where threat_id=threats.id and " + buildDistanceQuery(l.getLatitude(),l.getLongitude()) + " > ?)";
				Cursor c = db.rawQuery(sql, new String[] { Integer.toString(threat.alert.alertType.getCode()),
						Float.toString(threat.alert.frequency), partialDistance
						} );
				if ( c.getCount() > 0 ) {
					while (c.moveToNext() ) {
						threat_ids.add(c.getInt(0));
					}
				}
			}
		}
		return threat_ids.size();
	}
	
	/**
	 * Leaves no more than maxRecordCount records on threats database
	 * @param maxRecordCount
	 */
	public static void purgeOldLogRecords(int maxRecordCount) {
		SQLiteDatabase db = instance.getWritableDatabase(); 
		db.execSQL("delete from threats where id in (select id from threats order by timestamp desc limit " + maxRecordCount + ")");
	}
	
	/**
	 * Purges log records prior to specified cutoff datetime
	 * @param cutoffDateTime
	 */
	public static void purgeOldLogRecords(Date cutoffDateTime) {
		SQLiteDatabase db = instance.getWritableDatabase();
		db.execSQL("delete from threats where timestamp>" + cutoffDateTime.getTime());
	}
	
	@Override
	public void onConfigure(SQLiteDatabase db) {
		super.onOpen(db);
		db.setForeignKeyConstraintsEnabled(true);
	}
	
	public static void injectLocationValues(ContentValues values, double latitude, double longitude) {
	    values.put(LATITUDE, latitude);
	    values.put(LONGITUDE, longitude);
	    values.put(COSLAT, Math.cos(deg2rad(latitude)));
	    values.put(SINLAT, Math.sin(deg2rad(latitude)));
	    values.put(COSLNG, Math.cos(deg2rad(longitude)));
	    values.put(SINLNG, Math.sin(deg2rad(longitude)));
	}

	public static double deg2rad(double deg) {
	    return (deg * Math.PI / 180.0);
	}
	
	public static double convertPartialDistanceToKm(double result) {
	    return Math.acos(result) * EARTH_R;
	}
	
	public static double convertKmToPartialDistance(double km) {
	    return Math.cos(km/EARTH_R);
	}
	
	/**
	 * Build query based on distance using spherical law of cosinus
	 * 
	 * d = acos(sin(lat1).sin(lat2)+cos(lat1).cos(lat2).cos(long2−long1)).R
	 * where R=6371 and latitudes and longitudes expressed in radians
	 * 
	 * In Sqlite we do not have access to acos() sin() and lat() functions.
	 * Knowing that cos(A-B) = cos(A).cos(B) + sin(A).sin(B)
	 * We can determine a distance stub as:
	 * d = sin(lat1).sin(lat2)+cos(lat1).cos(lat2).(cos(long2).cos(long1)+sin(long2).sin(long1))
	 * 
	 * First comparison point being fixed, sin(lat1) cos(lat1) sin(long1) and cos(long1)
	 * can be replaced by constants.
	 * 
	 * Location aware table must therefore have the following columns to build the equation:
	 * sinlat => sin(radians(lat))
	 * coslat => cos(radians(lat))
	 * coslng => cos(radians(lng))
	 * sinlng => sin(radians(lng))
	 *  
	 * Function will return a real between -1 and 1 which can be used to order the query.
	 * Distance in km is after expressed from R.acos(result) 
	 *  
	 * @param latitude, latitude of search
	 * @param longitude, longitude of search
	 * @return selection query to compute the distance
	 */
	public static String buildDistanceQuery(double latitude, double longitude) {
	    final double coslat = Math.cos(deg2rad(latitude));
	    final double sinlat = Math.sin(deg2rad(latitude));
	    final double coslng = Math.cos(deg2rad(longitude));
	    final double sinlng = Math.sin(deg2rad(longitude));
	    //@formatter:off
	    return "(" + coslat + "*" + COSLAT
	            + "*(" + COSLNG + "*" + coslng
	            + "+" + SINLNG + "*" + sinlng
	            + ")+" + sinlat + "*" + SINLAT 
	            + ")";
	    //@formatter:on
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("create table threats(id integer primary key autoincrement, type integer, "
				+ "freq real, timestamp integer,end_timestamp integer,fake integer);");
		db.execSQL("create table threats_locations(id integer primary key autoincrement, threat_id integer,"
				+ " lat real, long real,ts integer,"
				+ COSLAT + " real," + SINLAT + " real," + COSLNG + " real," + SINLNG + " real,speed real,bearing real," 
				+ "foreign key(threat_id) references threats(id) on delete cascade);");
		db.execSQL("create index threat_ind1 on threats(type,freq);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if ( oldVersion == 1 && newVersion == 2 ) {
			db.execSQL("alter table threats_loc add column " + COSLAT + " real;");
			db.execSQL("alter table threats_loc add column " + SINLAT + " real;");
			db.execSQL("alter table threats_loc add column " + COSLNG + " real;");
			db.execSQL("alter table threats_loc add column " + SINLNG + " real;");
			oldVersion = 2;
		}
		if ( oldVersion == 2 && newVersion == 3 ) {
			db.execSQL("create table threats_locations(id integer primary key autoincrement, threat_id integer, lat real, long real, ts integer,"
					+ "foreign key(threat_id) references threats(id) on delete cascade);");
			db.execSQL("insert into threats_locations select * from threats_loc;");
			db.execSQL("drop table threats_loc;");
			db.execSQL("alter table threats_locations add column speed real;");
			db.execSQL("alter table threats_locations add column bearing real;");
			oldVersion = 3;
		}
		if ( oldVersion == 3 && newVersion == 4 ) {
			db.execSQL("alter table threats add column end_timestamp integer;");
			db.execSQL("alter table threats add column fake integer;");
			db.execSQL("create index threat_ind1 on threats(type,freq);");
			oldVersion = 4;
		}
	}
	
	/**
	 * Removes old records in DB as per preferences settings
	 * @author pzeltins
	 *
	 */
	public static class DBPruneService extends IntentService {

		public DBPruneService() {
			super(DBPruneService.class.getCanonicalName());
		}

		@Override
		protected void onHandleIntent(Intent intent) {
			// Keep X last records
			if ( Preferences.isLogThreatLimitNumeric() ) {
				purgeOldLogRecords(Preferences.getLogThreatLimitNumeric());
			}
		}
		
	}
	
	/**
	 * Threat logger service, performs db operations in background
	 * @author pzeltins
	 *
	 */
	public static class ThreatLoggerService extends IntentService {

		public ThreatLoggerService() {
			super(ThreatLoggerService.class.getCanonicalName());
		}

		protected static final String KEY_BUNDLE_ALERT = "alertObj";
		protected static final String KEY_BUNDLE_LOCATIONS = "setOfLocations";
		protected static final String KEY_BUNDLE_START_TIME = "threatStartTime";
		protected static final String KEY_BUNDLE_END_TIME = "threatEndTime";
		protected static final String KEY_BUNDLE_CREDIBILITY = "threatCredibility";
		
		@Override
		protected void onHandleIntent(Intent intent) {
			RadarMessageThreat t = (RadarMessageThreat) intent.getSerializableExtra(KEY_BUNDLE_ALERT);
			if ( t == null )
				return;
			
			ArrayList<Location> locations = intent.getParcelableArrayListExtra(KEY_BUNDLE_LOCATIONS);
			Long startTime = intent.getLongExtra(KEY_BUNDLE_START_TIME, 0);
			Long endTime = intent.getLongExtra(KEY_BUNDLE_END_TIME, 0);
			ThreatManager.ThreatCredibility cred = (ThreatCredibility) intent.getSerializableExtra(KEY_BUNDLE_CREDIBILITY);
			
			SQLiteDatabase db = instance.getWritableDatabase(); 
			db.beginTransaction();
			ContentValues v = new ContentValues();
			v.put("type", t.alertType.getCode());
			v.put("freq", t.frequency);
			v.put("timestamp", startTime);
			v.put("end_timestamp", endTime);
			v.put("fake", cred.getCode());
			long threatId = db.insert("threats", null, v);
			if ( locations != null ) {
				for ( Location l : locations ) {
					v = new ContentValues();
					v.put("threat_id", threatId);
					injectLocationValues(v, l.getLatitude(), l.getLongitude());
					v.put("ts", l.getTime());
					v.put("speed", l.getSpeed());
					v.put("bearing", l.getBearing());
					db.insert("threats_locations", null, v);
				}
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		
		
	}
	
}
