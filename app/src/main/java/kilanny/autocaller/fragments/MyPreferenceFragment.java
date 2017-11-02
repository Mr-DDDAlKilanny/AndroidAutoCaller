package kilanny.autocaller.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import kilanny.autocaller.R;

public class MyPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
    }
}
