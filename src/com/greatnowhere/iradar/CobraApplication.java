package com.greatnowhere.iradar;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "", mode=ReportingInteractionMode.NOTIFICATION, mailTo="rootkit007@gmail.com",
		resNotifText=R.string.crashNotificationText, resNotifTitle=R.string.crashNotificationTitle,
		resNotifTickerText=R.string.crashNotificationTitle, resDialogText=R.string.crashNotificationText)
public class CobraApplication extends Application {

	 @Override
     public void onCreate() {
         super.onCreate();

         // The following line triggers the initialization of ACRA
         ACRA.init(this);
     }
}
