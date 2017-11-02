package kilanny.autocaller.data;

import android.content.Context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Yasser on 06/10/2016.
 */
public class AutoCallLog implements Serializable {
    public static final long serialVersionUID =-5262431042354907616L;

    public static class AutoCallSession extends ArrayList<AutoCallItem>
            implements Serializable {
        public Date date;
    }

    public static abstract class AutoCallItem implements Serializable {

    }
    public static class AutoCallRetry extends AutoCallItem {

    }
    public static class AutoCall extends AutoCallItem {
        public static final int RESULT_UNKNOWN = 0;
        public static final int RESULT_NOT_ANSWERED = 1;
        public static final int RESULT_ANSWERED_OR_REJECTED = 2;


        public String name, number;
        public Date date;
        public int result;
    }
    private static AutoCallLog instance;

    public final ArrayList<AutoCallSession> sessions = new ArrayList<>();

    public static AutoCallLog readOld(Context context) throws IOException, ClassNotFoundException {
        FileInputStream fis = context.openFileInput("autolog");
        ObjectInputStream is = new ObjectInputStream(fis);
        instance = (AutoCallLog) is.readObject();
        is.close();
        fis.close();
        return instance;
    }
}
