package kilanny.autocaller.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.R;
import kilanny.autocaller.adapters.ExpandableListAdapter_Cities;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.databinding.ActivityCitiesBinding;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;

public class CitiesActivity extends AppCompatActivity {

    private ActivityCitiesBinding binding;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private boolean isFabMenuOpen = false;

    @Inject
    CityList cities;
    @Inject
    ListOfCallingLists listOfCallingLists;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ContextComponent contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
        bindList();
    }

    private void bindList() {
        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expList_cities);

        // preparing list data
        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();
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
                listDataHeader, listDataChild, cities);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        final int groupPosition, final int childPosition, long id) {
                new AlertDialog.Builder(CitiesActivity.this)
                        .setItems(R.array.city_item_options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String country = listDataHeader.get(groupPosition);
                                String city = listDataChild.get(country)
                                        .get(childPosition);
                                switch (which) {
                                    case 0:
                                        startEditActivity(cities.findByName(country, city));
                                        break;
                                    case 1:
                                        delete(country, city);
                                        break;
                                    case 2: {
                                        Intent i = new Intent(
                                                CitiesActivity.this, ShowCityPrayTimesActivity.class);
                                        i.putExtra("cityId", cities.findByName(country, city).id);
                                        startActivity(i);
                                    }
                                        break;
                                }
                            }
                        })
                        .show();
                return true;
            }
        });
    }

    private Map<String, String> getCityContacts(int cityId) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < listOfCallingLists.size(); ++i) {
            ContactsList list = listOfCallingLists.get(i);
            for (int j = 0; j < list.size(); ++j) {
                ContactsListItem item = list.get(j);
                if (item.cityId != null && item.cityId == cityId
                        && !result.containsKey(item.number))
                    result.put(item.number, item.name);
            }
        }
        return result;
    }

    private void delete(final String country, final String city) {
        new AlertDialog.Builder(CitiesActivity.this)
                .setTitle(R.string.dlg_deleteCity_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.dlg_deleteCity_msg)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final City c = cities.findByName(country, city);
                        Map<String, String> cityContacts = getCityContacts(c.id);
                        if (cityContacts.size() == 0){
                            cities.remove(c);
                            cities.save(CitiesActivity.this);
                            bindList();
                        } else {
                            StringBuilder numbers = new StringBuilder("\n");
                            for (String number : cityContacts.keySet()) {
                                numbers.append(number)
                                        .append(" ")
                                        .append(cityContacts.get(number))
                                        .append("\n");
                            }
                            new AlertDialog.Builder(CitiesActivity.this)
                                    .setTitle(R.string.dlg_deleteCity_title)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setMessage(getString(R.string.dlg_deleteCity_failed_msg) + numbers)
                                    .show();
                        }
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
            cities.findById(city.id).setFrom(city);
            cities.save(this);
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
