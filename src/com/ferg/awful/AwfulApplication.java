package com.ferg.awful;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

import com.ferg.awful.constants.Constants;

@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=d6a53a0d", formKey = Constants.ACRA_FORMKEY) 
public class AwfulApplication extends Application {
	private static String TAG="AwfulApplication";

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }
}
