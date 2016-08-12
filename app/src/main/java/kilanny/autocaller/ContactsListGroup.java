package kilanny.autocaller;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by Yasser on 08/12/2016.
 */
public class ContactsListGroup implements Serializable {
    public String name;
    //<number, name>
    public HashMap<String, String> contacts = new HashMap<>();
}
