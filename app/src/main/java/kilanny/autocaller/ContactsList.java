package kilanny.autocaller;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/**
 * Created by Yasser on 05/31/2016.
 */
public class ContactsList extends ArrayList<ContactsListItem>
        implements Serializable {

    private static final String OLD_LIST_FILE_NAME = "contacts.dat";
    static final long serialVersionUID =-4234930416454832853L;

    static ContactsList readOld(Context context)
            throws IOException, ClassNotFoundException {
        ContactsList instance;
        FileInputStream fis = context.openFileInput(OLD_LIST_FILE_NAME);
        ObjectInputStream is = new ObjectInputStream(fis);
        instance = (ContactsList) is.readObject();
        is.close();
        fis.close();
        instance.groups = ContactsListGroupList.readOld(context);
        instance.log = AutoCallLog.readOld(context);
        instance.setName("Menu1");
        return instance;
    }

    static boolean deleteOld(Context context) {
        return context.deleteFile(OLD_LIST_FILE_NAME);
    }

    private String name;
    private ContactsListGroupList groups;
    private AutoCallLog log;
    public final ListOfCallingLists myList;

    public ContactsList(@NonNull ListOfCallingLists myList) {
        this.myList = myList;
        groups = new ContactsListGroupList();
        log = new AutoCallLog();
    }

    public ContactsList(@NonNull ListOfCallingLists myList, @NonNull String name) {
        this(myList);
        setName(name);
    }

    public ContactsListGroupList getGroups() {
        return groups;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public AutoCallLog getLog() {
        return log;
    }

    public void save(@NonNull Context context) {
        myList.save(context);
    }
}
