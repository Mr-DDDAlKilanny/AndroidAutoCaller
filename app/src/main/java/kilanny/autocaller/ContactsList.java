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
 * Created by Yasser on 05/31/2016.
 */
public class ContactsList extends ArrayList<ContactsListItem>
        implements Serializable {
    private static ContactsList instance;
    public static final String CONTACTS_LIST_SAVE_FILE_NAME = "contacts.dat";

    public static ContactsList getInstance(Context context) {
        if (instance == null) {
            try {
                FileInputStream fis = context.openFileInput(CONTACTS_LIST_SAVE_FILE_NAME);
                ObjectInputStream is = new ObjectInputStream(fis);
                instance = (ContactsList) is.readObject();
                is.close();
                fis.close();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
            if (instance == null)
                instance = new ContactsList();
        }
        return instance;
    }

    private ContactsList() {
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
