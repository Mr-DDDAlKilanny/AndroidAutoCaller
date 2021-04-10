package kilanny.autocaller.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Locale;

import kilanny.autocaller.data.SerializableInFile;

public final class UpdateCheckUtil {

    public static final byte CONNECTION_STATUS_CONNECTED = 1,
            CONNECTION_STATUS_NOT_CONNECTED = 2,
            CONNECTION_STATUS_UNKNOWN_STATUS = 3;

    public static byte isConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting() ?
                    CONNECTION_STATUS_CONNECTED : CONNECTION_STATUS_NOT_CONNECTED;
        } catch (Exception ignored) {
            return CONNECTION_STATUS_UNKNOWN_STATUS;
        }
    }

    public static String getLastCheckWhatsnew(Context context) {
        final SerializableInFile<String> maqraahResponse = new SerializableInFile<>(
                context, "updates__st", null);
        return maqraahResponse.getData();
    }

    public static boolean shouldCheckForUpdates(Context context) {
        final SerializableInFile<String> maqraahResponse = new SerializableInFile<>(
                context, "updates__st", null);
        Date date = maqraahResponse.getFileLastModifiedDate(context);
        if (date == null) {
            maqraahResponse.setData(null, context);
            return true;
        }
        long diffTime = new Date().getTime() - date.getTime();
        long diffDays = diffTime / (1000 * 60 * 60 * 24);
        return diffDays > 6;
    }

    public static void setHasCheckedForUpdates(Context context, String whatsnew) {
        final SerializableInFile<String> maqraahResponse = new SerializableInFile<>(
                context, "updates__st", null);
        maqraahResponse.setData(whatsnew, context);
    }

    public static String[] getLatestVersion() {
        try {
            URL url = new URL("https://firestore.googleapis.com/v1/projects/alien-bruin-242913/databases/(default)/documents/versions?pageSize=1&orderBy=createdOn%20desc");
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            JSONObject jsonObject = new JSONObject(builder.toString());
            jsonObject = jsonObject.getJSONArray("documents").getJSONObject(0)
                    .getJSONObject("fields");
            String versionName = jsonObject.getJSONObject("name").getString("stringValue");
            String versionCode = String.format(Locale.ENGLISH, "%d",
                    jsonObject.getJSONObject("code").getInt("integerValue"));
            String whatsnew = jsonObject.getJSONObject("whatsnew").getString("stringValue");
            return new String[]{versionCode, versionName, whatsnew};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
