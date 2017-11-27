package kilanny.autocaller.intents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import kilanny.autocaller.App;
import kilanny.autocaller.utils.TextUtils;

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
        phoneNumber = TextUtils.fixPhoneNumber(phoneNumber);
        App app = App.get(context);
        if (app.lastCallNumber != null) {
            String other = TextUtils.fixPhoneNumber(app.lastCallNumber);
            boolean equals = other.equals(phoneNumber)
                    || other.endsWith(phoneNumber)
                    || phoneNumber.endsWith(other);
            Log.d("OutgoingCallReciever",
                    String.format("Numbers: %s, %s compare result = %B",
                            phoneNumber, other, equals));
            if (equals) {
                app.verifiedByOutgoingReceiver = true;
                try {
                    Toast.makeText(context,
                            TextUtils.getCurrentCalleeProgressMessage(context,
                                    app.lastCallName, app.lastCallCurrentCount,
                                    app.lastCallTotalCount, true),
                            Toast.LENGTH_LONG).show();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
