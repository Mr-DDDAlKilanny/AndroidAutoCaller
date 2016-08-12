package kilanny.autocaller;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ScheduleService extends Service {

    /**
     * Class for clients to access
     */
    public class ServiceBinder extends Binder {
        ScheduleService getService() {
            return ScheduleService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ScheduleService", "Received start id " + startId + ": " + intent);

        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients. See
    private final IBinder mBinder = new ServiceBinder();

    /**
     * Show an alarm for a certain date when the alarm is called it will pop up a notification
     */
    public void setAlarm(Calendar c) {
        // This starts a new thread to set the alarm
        // You want to push off your tasks onto a new thread to free up the UI to carry on responding
        // Request to start are service when the alarm date is upon us
        // We don't start an activity as we just want to pop up a notification into the system bar not a full activity
        Intent intent = new Intent(this, NotifyService.class);
        intent.putExtra(NotifyService.INTENT_NOTIFY, true);
        PendingIntent pendingIntent = PendingIntent
                .getService(this, 0, intent, 0);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Sets an alarm - note this alarm will be lost if the phone
        // is turned off and on again
        am.set(AlarmManager.RTC, c.getTimeInMillis(), pendingIntent);
    }
}