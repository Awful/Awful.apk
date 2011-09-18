package com.ferg.awful;

import android.app.Application;

import org.acra.*;
import org.acra.annotation.*;

import com.ferg.awful.constants.Constants;

/**
 * Allows application-wide access to the global image cache
 *
 * @author brosmike
 */
@ReportsCrashes(formUri = "http://www.bugsense.com/api/acra?api_key=aa9457c2", formKey = "")//Constants.ACRA_FORMKEY) 
public class AwfulApplication extends Application {
	private static String TAG="AwfulApplication";

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }
}
