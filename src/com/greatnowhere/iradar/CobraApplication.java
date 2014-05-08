package com.greatnowhere.iradar;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "", mode=ReportingInteractionMode.DIALOG, mailTo="rootkit007@gmail.com",
		resNotifText=R.string.crashNotificationText, resNotifTitle=R.string.crashNotificationTitle,
		resNotifTickerText=R.string.crashNotificationTitle, resDialogText=R.string.crashNotificationText,
		resDialogCommentPrompt=R.string.crashNotificationText, resDialogTitle=R.string.crashNotificationTitle)
public class CobraApplication extends Application {

	 @Override
     public void onCreate() {
         super.onCreate();

         // The following line triggers the initialization of ACRA
         //ACRA.init(this);
     }
}
