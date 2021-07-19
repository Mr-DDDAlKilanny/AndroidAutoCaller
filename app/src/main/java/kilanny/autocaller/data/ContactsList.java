package kilanny.autocaller.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Yasser on 05/31/2016.
 */
@Deprecated
public class ContactsList extends ArrayList<ContactsListItem>
        implements Serializable {

    private static final String OLD_LIST_FILE_NAME = "contacts.dat";
    static final long serialVersionUID =-4234930416454832853L;

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
