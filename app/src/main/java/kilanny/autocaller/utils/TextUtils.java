package kilanny.autocaller.utils;

import android.content.Context;

import kilanny.autocaller.R;

/**
 * Created by ibraheem on 5/11/2017.
 */
public final class TextUtils {

    public static String getCurrentCalleeProgressMessage(Context context,
                                                         String lastCallName,
                                                         int lastCallCurrentCount,
                                                         int lastCallTotalCount,
                                                         boolean includePrefix) {
        String prefix = includePrefix ?
                context.getString(R.string.toast_display_call_callTo) + " "
                : "";
        return prefix + lastCallName + " (" + lastCallCurrentCount
                + " " + context.getString(R.string.toast_display_call_of)
                + " " + lastCallTotalCount + ")";
    }

    public static String fixPhoneNumber(String n) {
        return n.replace("(", "")
                .replace(")", "")
                .replace("-", "")
                .replace(" ", "")
                .trim();
    }

    private TextUtils() {}
}
