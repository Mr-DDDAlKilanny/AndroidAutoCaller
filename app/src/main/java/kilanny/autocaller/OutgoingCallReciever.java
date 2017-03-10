package kilanny.autocaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Yasser on 06/11/2016.
 */
public class OutgoingCallReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        if (phoneNumber == null)
            return; //potential bug
        Log.d("OutgoingCallReciever", intent.toString() + ", call to: " + phoneNumber);
        phoneNumber = phoneNumber.replace(" ", "").trim();
        Application app = Application.getInstance(context);
        if (app.lastCallNumber != null) {
            String other = app.lastCallNumber.replace(" ", "").trim();
            boolean equals = other.equals(phoneNumber)
                    || other.endsWith(phoneNumber)
                    || phoneNumber.endsWith(other);
            Log.d("OutgoingCallReciever",
                    String.format("Numbers: %s, %s compare result = %B",
                            phoneNumber, other, equals));
            if (equals) {
                app.verifiedByOutgoingReceiver = true;
                app.save(context);
                try {
                    Toast.makeText(context,
                            context.getString(R.string.toast_display_call_callTo)
                                    + " " + app.lastCallName + " (" + app.lastCallCurrentCount
                                    + " " + context.getString(R.string.toast_display_call_of)
                                    + " " + app.lastCallTotalCount + ")",
                            Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
