package kilanny.autocaller;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by ibraheem on 5/8/2017.
 */
public class PrefsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MyPreferenceFragment())
                .commit();
    }
}

