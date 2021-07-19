package kilanny.autocaller.data;

import android.content.Context;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Yasser on 08/12/2016.
 */
@Deprecated
public class ContactsListGroupList extends ArrayList<ContactsListGroup>
    implements Serializable {

    static final long serialVersionUID=-4297779932874148360L;

    public static ContactsListGroupList readOld(Context context) throws IOException, ClassNotFoundException {
        FileInputStream fis = context.openFileInput("contact_groups.dat");
        ObjectInputStream is = new ObjectInputStream(fis);
        ContactsListGroupList instance = (ContactsListGroupList) is.readObject();
        is.close();
        fis.close();
        return instance;
    }
}
