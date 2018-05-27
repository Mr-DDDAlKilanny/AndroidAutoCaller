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
        static final long serialVersionUID=4100471386052040774L;

        public Date date;
    }

    public static abstract class AutoCallItem implements Serializable {
        static final long serialVersionUID=-6126351805320809037L;
    }
    public static class AutoCallRetry extends AutoCallItem {
        static final long serialVersionUID=-2390267599795357233L;
    }
    public static class AutoCall extends AutoCallItem {
        static final long serialVersionUID=5675691872497743035L;

        public static final int RESULT_UNKNOWN = 0;
        public static final int RESULT_NOT_ANSWERED = 1;
        public static final int RESULT_ANSWERED_OR_REJECTED = 2;

        public String name, number;
        public Date date;
        public int result;
    }
    public static class AutoCallIgnored extends AutoCallItem {
        static final long serialVersionUID=1L;
        public static final byte RESULT_NOT_IGNORED = 0;
        public static final byte RESULT_IGNORED_BEFORE_FAJR = 1;
        public static final byte RESULT_IGNORED_AFTER_SUNRISE = 2;

        public String name, number;
        public Date date;
        public byte result;
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
