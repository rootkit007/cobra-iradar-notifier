package com.greatnowhere.iradar;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;

@ReportsCrashes(formKey = "", mode=ReportingInteractionMode.TOAST, mailTo="rootkit007@gmail.com")
public class CobraApplication extends Application {

	 @Override
     public void onCreate() {
         super.onCreate();

         // The following line triggers the initialization of ACRA
         ACRA.init(this);
     }
}
