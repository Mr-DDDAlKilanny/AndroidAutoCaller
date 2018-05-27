package kilanny.autocaller.data;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

public final class AutoCallSession implements Serializable {
    static final long serialVersionUID=1L;
    private static final String FILE_NAME = "__AUTO_CALL_SESSION.dat";

    public final int contactsListId;
    public final Date date;

    private transient Context context;
    private transient boolean started;

    private int listCurrentCallItemIdx = 1;
    private int listCurrentCallItemCount = 0;
    private int listAutoRecallCount = 0;
    private final HashSet<String> ansOrRejectedNumbers = new HashSet<>();

    public AutoCallSession(int contactsListId, Date date, Context context) {
        this.contactsListId = contactsListId;
        this.date = date;
        this.context = context;
    }

    @Nullable
    public static AutoCallSession getLastSession(Context context) {
        try {
            FileInputStream fis = context.openFileInput(FILE_NAME);
            ObjectInputStream is = new ObjectInputStream(fis);
            AutoCallSession instance = (AutoCallSession) is.readObject();
            is.close();
            fis.close();
            instance.context = context;
            return instance;
        } catch (IOException | ClassNotFoundException ex) {
            return null;
        }
    }

    public static void clear(Context context) {
        AutoCallSession lastSession = getLastSession(context);
        if (lastSession != null) {
            context.deleteFile(FILE_NAME);
        }
    }

    private void save() {
        try {
            FileOutputStream fos = context.openFileOutput(FILE_NAME,
                    Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getListCurrentCallItemIdx() {
        return listCurrentCallItemIdx;
    }

    public void setListCurrentCallItemIdx(int listCurrentCallItemIdx) {
        this.listCurrentCallItemIdx = listCurrentCallItemIdx;
        save();
    }

    public int getListCurrentCallItemCount() {
        return listCurrentCallItemCount;
    }

    public void setListCurrentCallItemCount(int listCurrentCallItemCount) {
        this.listCurrentCallItemCount = listCurrentCallItemCount;
        save();
    }

    public int getListAutoRecallCount() {
        return listAutoRecallCount;
    }

    public void setListAutoRecallCount(int listAutoRecallCount) {
        this.listAutoRecallCount = listAutoRecallCount;
        save();
    }

    public void addNumberToRejectersList(String number) {
        ansOrRejectedNumbers.add(number);
        save();
    }

    public boolean containsNumberInRejectersList(String number) {
        return ansOrRejectedNumbers.contains(number);
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }
}
