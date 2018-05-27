package kilanny.autocaller.data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Yasser on 08/12/2016.
 */
public class ContactsListGroup implements Serializable {
    static final long serialVersionUID =1L;

    public String name;
    //<number, name>
    public HashMap<String, String> contacts = new HashMap<>();
}
