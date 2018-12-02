package kilanny.autocaller.data;

import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * Created by Yasser on 05/31/2016.
 */
public class ContactsListItem implements Serializable {
    static final long serialVersionUID=6222460388325562750L;

    public String name, number;
    public int callCount, index;

    @Nullable
    public Integer cityId;

    @Nullable
    public Integer callProfileId;
}
