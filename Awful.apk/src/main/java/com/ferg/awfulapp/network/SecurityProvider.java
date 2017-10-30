package com.ferg.awfulapp.network;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

/**
 * Created by baka kaba on 29/10/2017.
 * <p>
 * A component to handle updating the device's Security Provider, which updates to fix vulnerabilities
 * and routes security API calls through itself.
 * <p>
 * The update might fail because the user needs to install or update Google Play Services manually,
 * or it could hit an unrecoverable error - which means the app might not work at all, especially
 * if it's an old device that's still trying to use SSLv3. Use {@link #getStatus()} to get the most recent status.
 * <p>
 * See: <a href="https://developer.android.com/training/articles/security-gms-provider.html">Updating Your Security Provider to Protect Against SSL Exploits</a>
 */
abstract class SecurityProvider {

    private static String TAG = SecurityProvider.class.getSimpleName();
    private static Status status = Status.UNCHECKED;

    /**
     * Update the system's security provider to fix network vulnerabilities.
     * <p>
     * This fixes older devices trying to connect through SSLv3 (which gets rejected).
     */
    static void update(@NonNull Context context) {
        try {
            // this here blocks if there's an update, allegedly for ~350ms on older devices
            // calling this first when initialising NetworkUtils (and letting it block) means we can update it before the network stuff is set up
            ProviderInstaller.installIfNeeded(context);
            status = Status.UP_TO_DATE;
            Log.i(TAG, "Security Provider is up to date.");
        } catch (GooglePlayServicesRepairableException e) {
            status = Status.PLAY_SERVICES_UPDATE_REQUIRED;
            Log.w(TAG, "Security Provider requires a Google Play Services update.", e);

        } catch (GooglePlayServicesNotAvailableException e) {
            status = Status.PLAY_SERVICES_ERROR;
            Log.w(TAG, "Security Provider can't update!", e);
        }
    }

    @NonNull
    public static Status getStatus() {
        return status;
    }

    enum Status {UNCHECKED, UP_TO_DATE, PLAY_SERVICES_UPDATE_REQUIRED, PLAY_SERVICES_ERROR}
}