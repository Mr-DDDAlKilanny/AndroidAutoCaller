package kilanny.autocaller.utils;

import android.app.ActivityManager;
import android.content.Context;

/**
 * Created by ibraheem on 5/11/2017.
 */
public final class OsUtils {

    /**
     * http://stackoverflow.com/a/5921190/7429464
     */
    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private OsUtils() {}
}
