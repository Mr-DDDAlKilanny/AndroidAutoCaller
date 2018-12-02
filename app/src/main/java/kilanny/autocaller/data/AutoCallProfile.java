package kilanny.autocaller.data;

import java.io.Serializable;

public class AutoCallProfile implements Serializable {
    static final long serialVersionUID=1L;

    public final int id;

    public String name;
    public int noReplyTimeoutSeconds, killCallAfterSeconds;

    AutoCallProfile(int id) {
        this.id = id;
    }

}
