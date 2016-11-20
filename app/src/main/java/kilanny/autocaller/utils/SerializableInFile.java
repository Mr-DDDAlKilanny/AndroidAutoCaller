package kilanny.autocaller.utils;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by Yasser on 11/18/2016.
 */
public class SerializableInFile<T extends Serializable> implements Serializable {

    private T data;
    private final String fileName;
    private boolean saved;

    public SerializableInFile(@NonNull Context context, @NonNull String fileName) {
        this.fileName = fileName;
        saved = false;
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            setData((T) is.readObject());
            is.close();
            fis.close();
            saved = true;
        } catch (IOException | ClassNotFoundException ex) {
        }
    }

    public SerializableInFile(@NonNull Context context, @NonNull String fileName,
                              T initDataIfNotExists) {
        this(context, fileName);
        if (!isSaved())
            setData(initDataIfNotExists);
    }

    public boolean isSaved() {
        return saved;
    }

    public String getFileName() {
        return fileName;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setData(T data, Context context) {
        setData(data);
        save(context);
    }

    public boolean save(@NonNull Context context) {
        if (isSaved())
            return true;
        try {
            FileOutputStream fos = context.openFileOutput(fileName,
                    Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(data);
            os.close();
            fos.close();
            saved = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
