package kilanny.autocaller.utils;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Locale;

/**
 * Logs anonymous analytics logs. Personal information like phone number and location are NOT gathered
 */
public final class AnalyticsTrackers {

    private static AnalyticsTrackers instance;

    public static AnalyticsTrackers getInstance(Context context) {
        if (instance == null)
            instance = new AnalyticsTrackers(context);
        return instance;
    }

    private FirebaseAnalytics mFirebaseAnalytics;

    public boolean canMakeAnalytics() {
        return mFirebaseAnalytics != null;
    }

    public void logAddList() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("AddList", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditList() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditList", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logDeleteList() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DeleteList", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logClearLog() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("ClearLog", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logDeleteProfile(boolean succeeded) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("succeeded", succeeded);
            mFirebaseAnalytics.logEvent("DeleteProfile", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logAddCity() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("AddCity", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditCity() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditCity", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logPickedLocation() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("PickedLocation", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logDeleteCity(boolean succeeded) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("succeeded", succeeded);
            mFirebaseAnalytics.logEvent("DeleteCity", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditProfile(boolean isEdit) {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            bundle.putBoolean("isEdit", isEdit);
            mFirebaseAnalytics.logEvent("EditProfile", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logAddContact() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("AddContact", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditContactCity() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditContactCity", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditContactProfile() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditContactProfile", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditContactOrder() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditContactOrder", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logEditContactCount() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("EditContactCount", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logDeleteContact() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("DeleteContact", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logContinueLastSession() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("ContinueLastSession", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingInit() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingInit", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingStarted() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingStarted", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingIgnoredContactBeforeFajrAfterSunrise() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingIgnoredContactBeforeFajrAfterSunrise", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingStop() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingStop", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingRepeatList() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingRepeatList", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingMaxCountList() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingMaxCountList", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingContactAnsweredRejected() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingContactAnsweredRejected", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingContactNoAnswer() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingContactNoAnswer", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingContact() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingContact", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logRingCancelBeforeAfterTime() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("RingCancelBeforeAfterTime", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logIgnoredSomeNumbers() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("IgnoredSomeNumbers", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logShareApp() {
        if (!canMakeAnalytics()) return;
        try {
            Bundle bundle = new Bundle();
            mFirebaseAnalytics.logEvent("ShareApp", bundle);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void logException(Throwable throwable) {
        if (!canMakeAnalytics()) return;
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isGooglePlayServicesAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            try {
                int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
                switch (errorCode) {
                    case ConnectionResult.SUCCESS:
                        Log.d("isGmsAvailable", "SUCCESS");
                        // Google Play Services installed and up to date
                        return true;
                    case ConnectionResult.SERVICE_MISSING:
                        Log.d("isGmsAvailable", "MISSING");
                        // Google Play services is missing on this device.
                        break;
                    case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                        Log.d("isGmsAvailable", "VERSION_UPDATE_REQUIRED");
                        // The installed version of Google Play services is out of date.
                        break;
                    case ConnectionResult.SERVICE_DISABLED:
                        Log.d("isGmsAvailable", "DISABLED");
                        // The installed version of Google Play services has been disabled on this device.
                        break;
                    case ConnectionResult.SERVICE_INVALID:
                        Log.d("isGmsAvailable", "INVALID");
                        // The version of the Google Play services installed on this device is not authentic.
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private AnalyticsTrackers(Context context) {
        if (isGooglePlayServicesAvailable(context)) {
            try {
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
                mFirebaseAnalytics.setUserProperty("locale", Locale.getDefault().getDisplayName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
