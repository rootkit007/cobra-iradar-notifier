package com.greatnowhere.radar.threats;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.cobra.iradar.protocol.CobraRadarMessageNotification;
import com.greatnowhere.radar.config.Preferences;
import com.greatnowhere.radar.messaging.ConnectivityStatus;

import de.greenrobot.event.EventBus;

public class TTSManager {
	
	private static final String TAG = TTSManager.class.getCanonicalName(); 

	private static TextToSpeech tts;
	private static AtomicBoolean isReady = new AtomicBoolean(false);
	private static EventBus eventBus;
	private static AtomicInteger connStatus = new AtomicInteger(ConnectivityStatus.UNKNOWN.getCode());
	private static Timer timer;
	private static HashMap<String, String> ttsParams = new HashMap<String, String>();
	private static TelephonyManager tm;
	private static EventListener listener = new EventListener();
	private static TTSUtteranceProgressListener ttsUtteranceListener;
	
	public static void init(Context ctx) {
		stop();
		eventBus = EventBus.getDefault();
		isReady.set(false);
		tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tts = new TextToSpeech(ctx, new TTSInitListener());
		ttsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AlertAudioManager.OUTPUT_STREAM));
		ttsUtteranceListener = new TTSUtteranceProgressListener();
		tts.setOnUtteranceProgressListener(ttsUtteranceListener);
		eventBus.register(listener);
	}
	
	public static void stop() {
    	Log.d(TAG, "stop");
		if ( tts != null ) 
			tts.shutdown();
		if ( timer != null )
			timer.cancel();
		if ( eventBus != null && eventBus.isRegistered(listener))
			eventBus.unregister(listener);
	}
	
	public static class EventListener {
		public void onEvent(CobraRadarMessageNotification msg) {
			if ( msg.type == CobraRadarMessageNotification.TYPE_CONN ) {
				
				connStatus.set(msg.connectionStatus);
				
				switch (ConnectivityStatus.fromCode(msg.connectionStatus)) {
				case CONNECTED:
					speak(Preferences.getNotifyOnConnectText());
					startConnectivityTimer(Preferences.getNotifyWhileConnectedInterval());
					break;
				case DISCONNECTED:
					speak(Preferences.getNotifyOnDisconnectText());
					stopConnectivityTimer();
					break;
				default:
				}
				
			}
		}
	}
	
	private synchronized static void startConnectivityTimer(int seconds) {
		if ( timer != null )
			timer.cancel();
		if ( seconds > 0 ) {
			timer = new Timer(true);
			timer.scheduleAtFixedRate(new RadarActiveSpeaker(), (long) seconds * 1000L, (long) seconds * 1000L);
		}
	}
	
	private synchronized static void stopConnectivityTimer() {
		if ( timer != null )
			timer.cancel();
	}
	
	private static void speak(String text) {
		if ( Preferences.isNotifyConnectivityNotDuringCalls() && tm.getCallState() != TelephonyManager.CALL_STATE_IDLE )
			return;
		if ( isReady.get() && Preferences.isNotifyConnectivity() && text != null && !text.isEmpty() ) {
			AlertAudioManager.setTTSVolume();
	        ttsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,UUID.randomUUID().toString());
			int i = tts.speak(text, TextToSpeech.QUEUE_ADD, ttsParams);
			Log.d(TAG, "speak " + text + " :result " + i);
		}
	}
	
	private static class RadarActiveSpeaker extends TimerTask {
		@Override
		public void run() {
			speak(Preferences.getNotifyWhileConnectedText());
		}
	}
	
	private static class TTSInitListener implements TextToSpeech.OnInitListener {
		public void onInit(int status) {
			if ( status == TextToSpeech.SUCCESS )
				isReady.set(true);
		}
	}
	
	private static class TTSUtteranceProgressListener extends UtteranceProgressListener {
		@Override
		public void onStart(String utteranceId) {
			Log.d(TAG, "TTS started for " + utteranceId);
		}

		@Override
		public void onDone(String utteranceId) {
			Log.d(TAG, "TTS done for " + utteranceId);
			AlertAudioManager.restoreOldAlertVolume();
		}

		@Override
		public void onError(String utteranceId) {
			Log.d(TAG, "TTS error for " + utteranceId);
		}
		
	}
	
}
