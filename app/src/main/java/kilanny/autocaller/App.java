package kilanny.autocaller;

import android.content.Context;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Date;

import kilanny.autocaller.utils.AnalyticsTrackers;

/**
 * Created by user on 11/4/2017.
 */
public class App extends MultiDexApplication {

    public Date lastOutgoingCallStartRinging;
    public String lastCallNumber, lastCallName;
    public int lastCallCurrentCount, lastCallTotalCount, lastNumberCallProfileId;
    public boolean verifiedByOutgoingReceiver;

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && AnalyticsTrackers.isGooglePlayServicesAvailable(getApplicationContext())) {
            FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        }
    }
}
