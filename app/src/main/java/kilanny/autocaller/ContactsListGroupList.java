package kilanny.autocaller;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Yasser on 08/12/2016.
 */
public class ContactsListGroupList extends ArrayList<ContactsListGroup>
    implements Serializable {
    private static ContactsListGroupList instance;
    public static final String CONTACTS_LIST_SAVE_FILE_NAME = "contact_groups.dat";

    public static ContactsListGroupList getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(CONTACTS_LIST_SAVE_FILE_NAME);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (ContactsListGroupList) is.readObject();
                is.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            if (instance == null)
                instance = new ContactsListGroupList();
        }
        return instance;
    }

    private ContactsListGroupList() {
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(CONTACTS_LIST_SAVE_FILE_NAME,
                    Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(this);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
