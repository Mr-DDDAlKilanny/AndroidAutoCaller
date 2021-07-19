package kilanny.autocaller.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import kilanny.autocaller.R;
import kilanny.autocaller.serializers.SerializerFactory;

@Deprecated
public class AutoCallProfileList extends ArrayList<AutoCallProfile>
        implements Serializable {
    static final long serialVersionUID=1L;
    private static final String LIST_FILE_NAME = "ProfileList.dat";

    private int idCounter = 1;

    public static final int DEFAULT_PROFILE_ID = 1;

    public AutoCallProfileList(Context context) {
        try {
            FileInputStream fis = context.openFileInput(LIST_FILE_NAME);
            AutoCallProfileList instance = SerializerFactory.getSerializer()
                    .deserializeAutoCallProfiles(fis);
            addAll(instance);
            idCounter = instance.idCounter;
            fis.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            addInitialProfiles(context);
        }
    }

    public void save(Context context) {
        try {
            FileOutputStream fos = context.openFileOutput(LIST_FILE_NAME,
                    Context.MODE_PRIVATE);
            byte[] bytes = SerializerFactory.getSerializer()
                    .serialize(this);
            fos.write(bytes, 0, bytes.length);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AutoCallProfile createNewProfile() {
        return new AutoCallProfile(idCounter++);
    }

    public AutoCallProfile addNewProfile() {
        AutoCallProfile autoCallProfile = createNewProfile();
        add(autoCallProfile);
        return autoCallProfile;
    }

    private void addInitialProfiles(Context context) {
        AutoCallProfile callProfile = addNewProfile();
        callProfile.name = context.getString(R.string.default_profile);
        callProfile.noReplyTimeoutSeconds = 30 + 5;
        callProfile.killCallAfterSeconds = callProfile.noReplyTimeoutSeconds + 35;

        callProfile = addNewProfile();
        callProfile.name = context.getString(R.string.voice_mail_profile);
        callProfile.noReplyTimeoutSeconds = 30 + 5;
        callProfile.killCallAfterSeconds = callProfile.noReplyTimeoutSeconds + 5;
    }

    public AutoCallProfile findByName(@NonNull String name) {
        for (AutoCallProfile callProfile : this) {
            if (name.equals(callProfile.name)) {
                return callProfile;
            }
        }
        return null;
    }

    public AutoCallProfile findById(int id) {
        for (AutoCallProfile callProfile : this) {
            if (callProfile.id == id) {
                return callProfile;
            }
        }
        return null;
    }
}
