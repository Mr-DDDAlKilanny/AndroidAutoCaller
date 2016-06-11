/*
package kilanny.autocaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

*/
/**
 * @author Tony Gao
 *
 *//*

public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";

    // private static final long INCOMING_CALL_DELAY = 1600;// milliseconds

    @Override
    public void onReceive(final Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        Log.d(TAG, "onreceive, action: " + intent.getAction()
                + "; callState: " + tm.getCallState());


        // Note: An outgoing call fires this receiver twice for two actions:
        // once for action "android.intent.action.NEW_OUTGOING_CALL" with call state 0,
        // and then once for action "android.intent.action.PHONE_STATE" with call state 2;
        // AND: There won't be any thing happening that can be caught here when this
        // outgoing call is answered!
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(TAG, "outgoing call broadcast received, number:"
                    + phoneNumber + "; callState: " + tm.getCallState());

            Intent i = new Intent(context, CallAnswerService.class);
            i.putExtra(Intent.EXTRA_PHONE_NUMBER, phoneNumber);
            i.putExtra(CallAnswerService.EXTRA_CALL_STATE, CallAnswerService.CUSTOM_CALL_STATE_CALLING);
            context.startService(i);
        } else {
            int callState = tm.getCallState();
            Log.d(TAG, "broadcast received, call state: " + callState);

            Intent i = new Intent(context, CallAnswerService.class);
            i.putExtra(CallAnswerService.EXTRA_CALL_STATE, callState);
            switch (callState) {
                case TelephonyManager.CALL_STATE_RINGING:
                    String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    i.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                case TelephonyManager.CALL_STATE_OFFHOOK:

                    break;
                default:
                    return; // return to not start the service
            }
            context.startService(i);
        }
    }
}*/
