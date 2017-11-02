package kilanny.autocaller.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import kilanny.autocaller.fragments.MyPreferenceFragment;

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

