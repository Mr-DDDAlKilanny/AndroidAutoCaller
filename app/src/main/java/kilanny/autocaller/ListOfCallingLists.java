package kilanny.autocaller;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kilanny.autocaller.utils.SerializableInFile;
import kilanny.autocaller.utils.SerializablePair;

/**
 * Created by Yasser on 11/18/2016.
 */

public class ListOfCallingLists implements Serializable {

    private static ListOfCallingLists instance;
    private static final String LIST_FILE_NAME = "ListOfCallingLists.dat";
    static final long serialVersionUID =-7719765106986038527L;
    private static final ExecutorService executorService
            = Executors.newSingleThreadExecutor();

    private /*transient*/ SerializableInFile<Integer> idCounter;
    private ArrayList<SerializablePair<Integer, ContactsList> > list;

    public static ListOfCallingLists getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(LIST_FILE_NAME);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (ListOfCallingLists) is.readObject();
                is.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            if (instance == null) {
                instance = new ListOfCallingLists();
                //try to read the old, save it, and delete the old
                //if you fail, no problem "catch" will handle it
                try {
                    ContactsList list = ContactsList.readOld(context);
                    instance.add(list);
                    instance.save(context);
                    ContactsList.deleteOld(context);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        if (instance.idCounter == null)
            instance.idCounter = new SerializableInFile<>(context,
                    "ListOfCallingListsIdCounter.int", 0);
        return instance;
    }

    private ListOfCallingLists() {
        list = new ArrayList<>();
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

    private static void scheduleSave(final ListOfCallingLists list, final Context context) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    FileOutputStream fos = context.openFileOutput("tmp" + LIST_FILE_NAME,
                            Context.MODE_PRIVATE);
                    ObjectOutputStream os = new ObjectOutputStream(fos);
                    os.writeObject(list);
                    os.close();
                    fos.close();
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

    public void save(Context context) {
        try {
            scheduleSave((ListOfCallingLists) this.clone(), context);
            idCounter.save(context);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void add(ContactsList item) {
        int newId = idCounter.getData();
        idCounter.setData(newId + 1);
        list.add(new SerializablePair<>(newId, item));
    }

    public int size() {
        return list.size();
    }

    public ContactsList get(int index) {
        return list.get(index).getSecond();
    }

    public ContactsList getById(int id) {
        for (SerializablePair<Integer, ContactsList> item : list)
            if (item.getFirst() == id)
                return item.getSecond();
        return null;
    }

    public int idOf(ContactsList item) {
        for (SerializablePair<Integer, ContactsList> i : list)
            if (i.getSecond() == item)
                return i.getFirst();
        return -1;
    }

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

    private static ContactsList parseList(ListOfCallingLists parent, String s) {
        int idx = s.indexOf("\n");
        ContactsList result = new ContactsList(parent, s.substring(0, idx));
        idx = s.indexOf("*items", idx);
        idx = s.indexOf("\n", idx) + 1;
        int idx2 = s.indexOf("\n", idx);
        for (String s1 : s.substring(idx, idx2).split(";")) {
            if (s1.length() > 1) {
                ContactsListItem contactsListItem = new ContactsListItem();
                String[] arr = s1.split(",");
                contactsListItem.name = arr[0];
                contactsListItem.number = arr[1];
                contactsListItem.callCount = Integer.parseInt(arr[2]);
                contactsListItem.index = Integer.parseInt(arr[3]);
                result.add(contactsListItem);
            }
        }
        idx = s.indexOf("*groups", idx2);
        idx = s.indexOf("\n", idx);
        idx2 = s.indexOf("*logs", idx);
        for (String s1 : s.substring(idx, idx2).trim().split("\n")) {
            String tmpArr[] = s1.split(":");
            ContactsListGroup group = new ContactsListGroup();
            group.name = tmpArr[0];
            for (String s2 : tmpArr[1].split(";")) {
                if (s2.length() > 1) {
                    String arr2[] = s2.split(",");
                    group.contacts.put(arr2[0], arr2[1]);
                }
            }
            result.getGroups().add(group);
        }
        idx = s.indexOf("\n", idx2) + 1;
        for (String s1 : s.substring(idx).trim().split("\n")) {
            String arr1[] = s1.split(":");
            AutoCallLog.AutoCallSession autoCallSession = new AutoCallLog.AutoCallSession();
            autoCallSession.date = new Date(Long.parseLong(arr1[0]));
            for (String s2 : arr1[1].split(";")) {
                if (s2.length() > 1) {
                    if (s2.equals("entry"))
                        autoCallSession.add(new AutoCallLog.AutoCallRetry());
                    else {
                        AutoCallLog.AutoCall autoCall = new AutoCallLog.AutoCall();
                        String[] arr = s2.split(",");
                        autoCall.date = new Date(Long.parseLong(arr[0]));
                        autoCall.name = arr[1];
                        autoCall.number = arr[2];
                        autoCall.result = Integer.parseInt(arr[3]);
                        autoCallSession.add(autoCall);
                    }
                }
            }
            result.getLog().sessions.add(autoCallSession);
        }
        return result;
    }

    public static ListOfCallingLists parse(String s) {
        s = s.substring(s.indexOf("#"));
        ListOfCallingLists result = new ListOfCallingLists();
        for (String s1 : s.split("#")) {
            if (s1.length() > 1) {
                result.add(parseList(result, s1));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("ListOfCallingLists").append("\n\n");
        for (SerializablePair<Integer, ContactsList> item : list) {
            b.append("#" + item.getSecond().getName()).append("\n")
                    .append("*items:\n");
            for (ContactsListItem listItem : item.getSecond()) {
                b.append(listItem.name).append(",")
                        .append(listItem.number).append(",")
                        .append(listItem.callCount).append(",")
                        .append(listItem.index).append(";");
            }
            b.append("\n*groups:\n");
            for (ContactsListGroup group : item.getSecond().getGroups()) {
                b.append(group.name).append(":");
                for (Map.Entry<String, String> number : group.contacts.entrySet()) {
                    b.append(number.getKey()).append(",")
                            .append(number.getValue()).append(";");
                }
                b.append("\n");
            }
            b.append("*logs:\n");
            for (AutoCallLog.AutoCallSession session : item.getSecond().getLog().sessions) {
                b.append(session.date.getTime()).append(":");
                for (AutoCallLog.AutoCallItem logItem : session) {
                    if (logItem instanceof AutoCallLog.AutoCallRetry)
                        b.append("entry;");
                    else {
                        AutoCallLog.AutoCall i = (AutoCallLog.AutoCall) logItem;
                        b.append(i.date.getTime()).append(",")
                                .append(i.name).append(",")
                                .append(i.number).append(",")
                                .append(i.result).append(";");
                    }
                }
                b.append("\n");
            }
        }
        return b.toString();
    }
}
