package kilanny.autocaller;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Yasser on 06/10/2016.
 */
public class AutoCallLog implements Serializable {

    public static class AutoCallSession extends ArrayList<AutoCall>
            implements Serializable {
        Date date;
    }

    public static class AutoCall implements Serializable {
        public static final int RESULT_UNKNOWN = 0;
        public static final int RESULT_NOT_ANSWERED = 1;
        public static final int RESULT_ANSWERED_OR_REJECTED = 2;


        String name, number;
        Date date;
        int result;
    }
    private static final String AUTO_CALL_LOG_FILENAME = "autolog";
    private static AutoCallLog instance;

    final ArrayList<AutoCallSession> sessions = new ArrayList<>();

    public static AutoCallLog getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(
                        AUTO_CALL_LOG_FILENAME);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (AutoCallLog) is.readObject();
                is.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            if (instance == null)
                instance = new AutoCallLog();
        }
        return instance;
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(
                    AUTO_CALL_LOG_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AutoCallLog() {
    }
}
