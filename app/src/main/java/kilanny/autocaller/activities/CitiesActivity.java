package kilanny.autocaller.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ExpandableListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kilanny.autocaller.R;
import kilanny.autocaller.adapters.ExpandableListAdapter_Cities;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.databinding.ActivityCitiesBinding;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.utils.AnalyticsTrackers;

public class CitiesActivity extends AppCompatActivity {

    private ActivityCitiesBinding binding;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;

    ExpandableListAdapter_Cities listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cities);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_cities);
        binding.setFabHandler(new CitiesActivityFabHandler());

        getAnimations();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bindList();
    }

    private void bindList() {
        // get the listview
        expListView = findViewById(R.id.expList_cities);

        // preparing list data
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
        City[] cities = AppDb.getInstance(this).cityDao().getAll();
        for (City city : cities) {
            if (!listDataHeader.contains(city.country)) {
                listDataHeader.add(city.country);
                listDataChild.put(city.country, new ArrayList<String>());
            }
        }
        for (City city : cities) {
            listDataChild.get(city.country).add(city.name);
        }

        listAdapter = new ExpandableListAdapter_Cities(this,
                listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            new androidx.appcompat.app.AlertDialog.Builder(CitiesActivity.this)
                    .setItems(R.array.city_item_options, (dialog, which) -> {
                        String country = listDataHeader.get(groupPosition);
                        String city = listDataChild.get(country)
                                .get(childPosition);
                        switch (which) {
                            case 0:
                                startEditActivity(AppDb.getInstance(this).cityDao()
                                        .findByName(country, city));
                                break;
                            case 1:
                                delete(country, city);
                                break;
                            case 2: {
                                Intent i = new Intent(
                                        CitiesActivity.this, ShowCityPrayTimesActivity.class);
                                i.putExtra("cityId", AppDb.getInstance(this).cityDao()
                                        .findByName(country, city).id);
                                startActivity(i);
                            }
                                break;
                        }
                    })
                    .show();
            return true;
        });
    }

    private void delete(final String country, final String city) {
        new androidx.appcompat.app.AlertDialog.Builder(CitiesActivity.this)
                .setTitle(R.string.dlg_deleteCity_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.dlg_deleteCity_msg)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    final City c = AppDb.getInstance(this).cityDao().findByName(country, city);
                    ContactsListItem[] cityContacts = AppDb.getInstance(this).contactDao()
                            .getByCityId(c.id);
                    if (cityContacts.length == 0) {
                        AppDb.getInstance(this).cityDao().delete(c);
                        AnalyticsTrackers.getInstance(CitiesActivity.this)
                                .logDeleteCity(true);
                        bindList();
                    } else {
                        StringBuilder numbers = new StringBuilder("\n");
                        for (ContactsListItem item : cityContacts) {
                            numbers.append(item.number)
                                    .append(" ")
                                    .append(item.name)
                                    .append("\n");
                        }
                        new androidx.appcompat.app.AlertDialog.Builder(CitiesActivity.this)
                                .setTitle(R.string.dlg_deleteCity_title)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setMessage(getString(R.string.dlg_deleteCity_failed_msg) + numbers)
                                .show();
                        AnalyticsTrackers.getInstance(CitiesActivity.this)
                                .logDeleteCity(false);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void getAnimations() {
        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);

        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);
    }

    private void expandFabMenu() {
//        ViewCompat.animate(binding.baseFloatingActionButton)
//                .rotation(45.0F)
//                .withLayer()
//                .setDuration(300)
//                .setInterpolator(new OvershootInterpolator(10.0F))
//                .start();
        //binding.createLayout.startAnimation(fabOpenAnimation);
        //binding.shareLayout.startAnimation(fabOpenAnimation);
        //binding.createFab.setClickable(true);
        //binding.shareFab.setClickable(true);
        isFabMenuOpen = true;
    }

    private void collapseFabMenu() {
//        ViewCompat.animate(binding.baseFloatingActionButton)
//                .rotation(0.0F)
//                .withLayer()
//                .setDuration(300)
//                .setInterpolator(new OvershootInterpolator(10.0F))
//                .start();
        //binding.createLayout.startAnimation(fabCloseAnimation);
        //binding.shareLayout.startAnimation(fabCloseAnimation);
        //binding.createFab.setClickable(false);
        //binding.shareFab.setClickable(false);
        isFabMenuOpen = false;
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen)
            collapseFabMenu();
        else
            super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            City city = City.getCityFromIntent(data);
            City db = AppDb.getInstance(this).cityDao().find(city.id);
            if (db != null) {
                db.setFrom(city);
                AppDb.getInstance(this).cityDao().update(db);
            } else
                city.id = (int) AppDb.getInstance(this).cityDao().insert(city);
            bindList();
        }
    }

    private void startEditActivity(@Nullable City editCity) {
        Intent i = new Intent(CitiesActivity.this, EditCityActivity.class);
        i.putExtra("savedCountries", listDataHeader.toArray(new String[0]));
        if (editCity != null) {
            editCity.putExtraInIntent(i);
        }
        startActivityForResult(i, 0);
    }

    public class CitiesActivityFabHandler {

        public void onBaseFabClick(View view) {
//            if (isFabMenuOpen)
//                collapseFabMenu();
//            else
//                expandFabMenu();
            startEditActivity(null);
        }

        public void onCreateFabClick(View view) {
            //Snackbar.make(binding.coordinatorLayout, "Create FAB tapped", Snackbar.LENGTH_SHORT).show();
        }

        public void onShareFabClick(View view) {
            //Snackbar.make(binding.coordinatorLayout, "Share FAB tapped", Snackbar.LENGTH_SHORT).show();
        }
    }
}
