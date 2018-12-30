package kilanny.autocaller.intents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by ibraheem on 5/11/2017.
 */
public class AutoCallPhoneStateListener extends BroadcastReceiver {

    public static final String BROADCAST_ACTION = "AutoCallPhoneStateListener_ACTION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String callState = intent.getStringExtra("state");
        int state;
        if ("RINGING".equals(callState)) {
            state = TelephonyManager.CALL_STATE_RINGING;
        } else if ("OFFHOOK".equals(callState)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        } else if("IDLE".equals(callState)) {
            state = TelephonyManager.CALL_STATE_IDLE;
        } else {
            state = -1;
        }
        Log.d("PhoneStateListener", "State Received: " + callState);
        Intent i = new Intent();
        i.setAction(BROADCAST_ACTION);
        i.putExtra("state", state);
        context.sendBroadcast(i);
    }
}
