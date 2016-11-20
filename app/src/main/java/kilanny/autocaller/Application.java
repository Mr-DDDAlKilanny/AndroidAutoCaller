package kilanny.autocaller;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by Yasser on 06/11/2016.
 */
public class Application implements Serializable {

    private static final String FILE_NAME = "app";
    private static Application instance;

    @NonNull
    public static Application getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(FILE_NAME);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (Application) is.readObject();
                is.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            if (instance == null)
                instance = new Application();
        }
        return instance;
    }

    public Date lastOutgoingCallStartRinging;
    public String lastCallNumber, lastCallName;
    public int lastCallCurrentCount, lastCallTotalCount;

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(
                    FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Application() {
    }
}
