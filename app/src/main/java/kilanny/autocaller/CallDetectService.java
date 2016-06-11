/*
package kilanny.autocaller;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

*/
/**
 * Call detect service.
 * This service is needed, because MainActivity can lost it's focus,
 * and calls will not be detected.
 *
 * @author Moskvichev Andrey V.
 *
 *//*

public class CallDetectService extends Service {
    */
/**
     * Broadcast receiver to detect the outgoing calls.
     *//*

    public class OutgoingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) == null) {

            } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    .equals(TelephonyManager.EXTRA_STATE_RINGING)) {

                // Phone number
                String incomingNumber = intent
                        .getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                Toast.makeText(context,
                        "Outgoing: "+incomingNumber,
                        Toast.LENGTH_LONG).show();
                // Ringing state
                // This code will execute when the phone has an incoming call
            } else if (intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    .equals(TelephonyManager.EXTRA_STATE_IDLE)
                    || intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    .equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Toast.makeText(context,
                        "Outgoing idle/offhook",
                        Toast.LENGTH_LONG).show();
                // This code will execute when the call is answered or disconnected
            }
        }
    }

    private OutgoingReceiver outgoingReceiver;

    public CallDetectService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        outgoingReceiver = new OutgoingReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL);
        registerReceiver(outgoingReceiver, intentFilter);
        return res;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(outgoingReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supporting binding
        return null;
    }
}
*/
