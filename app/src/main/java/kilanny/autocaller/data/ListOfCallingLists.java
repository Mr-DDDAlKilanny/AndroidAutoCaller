package kilanny.autocaller.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kilanny.autocaller.serializers.SerializerFactory;

/**
 * Created by Yasser on 11/18/2016.
 */
@Deprecated
public class ListOfCallingLists implements Serializable {

    private static final String LIST_FILE_NAME = "ListOfCallingLists.dat";
    static final long serialVersionUID =-7719765106986038527L;
    private transient ExecutorService executorService;

    private /*transient*/ SerializableInFile<Integer> idCounter;
    private ArrayList<SerializablePair<Integer, ContactsList>> list;

    private static ListOfCallingLists instance;

    @Deprecated
    public static ListOfCallingLists getInstance(Context context) {
        if (instance == null)
            instance = new ListOfCallingLists(context);
        return instance;
    }

    private ListOfCallingLists(Context context) {
        try {
//            if (!new java.io.File(context.getFilesDir(), LIST_FILE_NAME).exists()) {
//                java.io.InputStream stream = context.getAssets().open("list_20200210.dat");
//                FileOutputStream tmp = context.openFileOutput(LIST_FILE_NAME, 0);
//                byte[] buf = new byte[1024];
//                int count;
//                while ((count = stream.read(buf)) > 0) {
//                    tmp.write(buf, 0, count);
//                }
//                stream.close();
//                tmp.close();
//            }
            FileInputStream fis = context.openFileInput(LIST_FILE_NAME);
            ListOfCallingLists instance = SerializerFactory.getSerializer()
                    .deserializeListOfCallingLists(fis);
            list = instance.list;
            idCounter = instance.idCounter;
            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            idCounter = new SerializableInFile<>(context, "ListOfCallingListsIdCounter.int", 0);
            list = new ArrayList<>();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream
                    = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
            byte[] bytes = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            ByteArrayInputStream byteArrayInputStream
                    = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream
                    = new ObjectInputStream(byteArrayInputStream);
            ListOfCallingLists list = (ListOfCallingLists)
                    objectInputStream.readObject();
            return list;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return super.clone();
        }
    }

    @Deprecated
    private void scheduleSave(final ListOfCallingLists list, final Context context) {
        if (executorService == null)
            executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = context.openFileOutput("tmp" + LIST_FILE_NAME,
                            Context.MODE_PRIVATE);
                    byte[] bytes = SerializerFactory.getSerializer().serialize(list);
                    fos.write(bytes, 0, bytes.length);
                    fos.close();
                    //ObjectOutputStream os = new ObjectOutputStream(fos);
                    //os.writeObject(list);
                    //os.close();
                    //fos.close();
                    // if succeeded, copy tmp file to our primary file
                    FileInputStream fis = context.openFileInput("tmp" + LIST_FILE_NAME);
                    FileOutputStream fos2 = context.openFileOutput(LIST_FILE_NAME,
                            Context.MODE_PRIVATE);
                    byte[] b = new byte[1024];
                    int count;
                    while ((count = fis.read(b, 0, b.length)) >= 0) {
                        fos2.write(b, 0, count);
                    }
                    fis.close();
                    fos2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Deprecated
    public void save(Context context) {
        try {
            scheduleSave((ListOfCallingLists) this.clone(), context);
            idCounter.save(context);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public void add(ContactsList item) {
        int newId = idCounter.getData();
        idCounter.setData(newId + 1);
        list.add(new SerializablePair<>(newId, item));
    }

    @Deprecated
    public int size() {
        return list.size();
    }

    @Deprecated
    public ContactsList get(int index) {
        return list.get(index).getSecond();
    }

    @Deprecated
    public ContactsList getById(int id) {
        for (SerializablePair<Integer, ContactsList> item : list)
            if (item.getFirst() == id)
                return item.getSecond();
        return null;
    }

    @Deprecated
    public int idOf(ContactsList item) {
        for (SerializablePair<Integer, ContactsList> i : list)
            if (i.getSecond() == item)
                return i.getFirst();
        return -1;
    }

    @Deprecated
    public boolean remove(ContactsList item) {
        for (int idx = 0; idx < list.size(); ++idx) {
            if (list.get(idx).getSecond() == item) {
                remove(idx);
                return true;
            }
        }
        return false;
    }

    public void remove(int index) {
        list.remove(index);
    }

    @NonNull
    public List<ContactsList> toList() {
        ArrayList<ContactsList> res = new ArrayList<>(size());
        for (SerializablePair<Integer, ContactsList> item : list)
            res.add(item.getSecond());
        return res;
    }
}
