package kilanny.autocaller.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.sucho.placepicker.AddressData;
import com.sucho.placepicker.Constants;
import com.sucho.placepicker.PlacePicker;

import java.util.List;

import javax.inject.Inject;

import kilanny.autocaller.App;
import kilanny.autocaller.R;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.di.ContextComponent;
import kilanny.autocaller.di.ContextModule;
import kilanny.autocaller.di.DaggerContextComponent;
import kilanny.autocaller.utils.AnalyticsTrackers;
import kilanny.autocaller.utils.PrayTimes;

/**
 * Created by user on 12/11/2017.
 */

public class EditCityActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 2;

    private AppCompatAutoCompleteTextView countryAutoCompleteTextView;
    private AppCompatEditText timezoneEditText, minMinutesBeforeSunriseEditText,
            minMinuteAfterFajrEditText, cityEditText;
    private AppCompatTextView selectedPlaceTextView;
    private AppCompatButton btnPickLocation;
    private AppCompatSpinner prayerCalcMethod, asrPrayerCalcMethod;
    private AddressData selectedPlace;
    private City editCity;

    @Inject
    CityList cities;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dlg_edit_city);

        prayerCalcMethod = findViewById(R.id.spinPrayerCalcMethod);
        asrPrayerCalcMethod = findViewById(R.id.spinAsrPrayerCalcMethod);
        countryAutoCompleteTextView = findViewById(R.id.txtCountryName);
        cityEditText = findViewById(R.id.txtCityName);
        timezoneEditText = findViewById(R.id.txtTimezone);
        minMinuteAfterFajrEditText = findViewById(R.id.txtMinMinuteAfterFajr);
        minMinutesBeforeSunriseEditText = findViewById(R.id.txtMinMinutesBeforeSunrise);
        btnPickLocation = findViewById(R.id.btnPickLocation);
        selectedPlaceTextView = findViewById(R.id.selectedPlace);

        prayerCalcMethod.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[] { getString(R.string.jafari),
                        getString(R.string.karachi),
                        getString(R.string.ISNA),
                        getString(R.string.MWL),
                        getString(R.string.Makkah),
                        getString(R.string.Egypt),
                        getString(R.string.Tehran)
        }));
        asrPrayerCalcMethod.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[] { getString(R.string.shafei),
                        getString(R.string.hanafi)
                }));

        Intent i = getIntent();
        editCity = i.hasExtra("lng") ?
                City.getCityFromIntent(i) : null;
        if (editCity != null) {
            prayerCalcMethod.setSelection(editCity.prayerCalcMethod);
            asrPrayerCalcMethod.setSelection(editCity.asrPrayerCalcMethod);
            countryAutoCompleteTextView.setText(editCity.country);
            cityEditText.setText(editCity.name);
            timezoneEditText.setText("" + editCity.timezone);
            minMinuteAfterFajrEditText.setText("" + editCity.minMinuteAfterFajr);
            minMinutesBeforeSunriseEditText.setText("" + editCity.minMinutesBeforeSunrise);
            selectedPlaceTextView.setText(editCity.plateName);
        } else {
            prayerCalcMethod.setSelection(PrayTimes.CALC_Makkah);
        }

        String[] savedCountries = i.getStringArrayExtra("savedCountries");
        countryAutoCompleteTextView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.select_dialog_item, savedCountries));

        findViewById(R.id.btnEditCityOk).setOnClickListener(btnOkClick);
        btnPickLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(EditCityActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(EditCityActivity.this,
                            R.string.mission_permissions_msg,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                if (editCity != null)
                    builder.setLatLong(editCity.lat, editCity.lng);
                else
                    builder.setLatLong(21.4224779,39.8251832);
                startActivityForResult(builder.build(EditCityActivity.this),
                        Constants.PLACE_PICKER_REQUEST);
            }
        });
        ContextComponent contextComponent = DaggerContextComponent.builder()
                .appComponent(App.get(this).getComponent())
                .contextModule(new ContextModule(this))
                .build();
        contextComponent.inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this,
                            R.string.dialog_title_systemalert_permission,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private View.OnClickListener btnOkClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int prayerTimeCalcMethod = prayerCalcMethod.getSelectedItemPosition();
            int asrPrayerTimeCalcMethod = asrPrayerCalcMethod.getSelectedItemPosition();
            String country = countryAutoCompleteTextView.getText().toString();
            String city = cityEditText.getText().toString();
            String timezone = timezoneEditText.getText().toString();
            String minMinuteAfterFajr = minMinuteAfterFajrEditText.getText().toString();
            String minMinutesBeforeSunrise = minMinutesBeforeSunriseEditText.getText().toString();
            boolean canSave = true;
            if (prayerTimeCalcMethod == AppCompatSpinner.INVALID_POSITION) {

                canSave = false;
            }
            if (asrPrayerTimeCalcMethod == AppCompatSpinner.INVALID_POSITION) {

                canSave = false;
            }
            if (country.trim().length() < 1) {
                countryAutoCompleteTextView.setError(null, null);
                canSave = false;
            }
            if (city.trim().length() < 1) {
                cityEditText.setError(null, null);
                canSave = false;
            }
            if (editCity == null && cities.findByName(country, city) != null) {
                Toast.makeText(EditCityActivity.this,
                        R.string.city_already_exists,
                        Toast.LENGTH_LONG).show();
                canSave = false;
            }
            if (timezone.trim().length() < 1) {
                timezoneEditText.setError(null, null);
                canSave = false;
            }
            if (minMinuteAfterFajr.trim().length() < 1 ||
                    Integer.parseInt(minMinuteAfterFajr.trim()) > 120) {
                minMinuteAfterFajrEditText.setError(null, null);
                canSave = false;
            }
            if (minMinutesBeforeSunrise.trim().length() < 1 ||
                    Integer.parseInt(minMinutesBeforeSunrise.trim()) > 120) {
                minMinutesBeforeSunriseEditText.setError(null, null);
                canSave = false;
            }
            if (selectedPlace == null && editCity == null) {
                btnPickLocation.setError(null, null);
                canSave = false;
            }
            if (!canSave) {
                Toast.makeText(EditCityActivity.this, R.string.all_fields_required, Toast.LENGTH_LONG).show();
                return;
            }
            Intent result = new Intent();
            if (editCity != null) {
                editCity.prayerCalcMethod = prayerTimeCalcMethod;
                editCity.asrPrayerCalcMethod = asrPrayerTimeCalcMethod;
                editCity.country = country;
                editCity.name = city;
                editCity.timezone = Integer.parseInt(timezone);
                editCity.minMinuteAfterFajr = Integer.parseInt(minMinuteAfterFajr);
                editCity.minMinutesBeforeSunrise = Integer.parseInt(minMinutesBeforeSunrise);
                if (selectedPlace != null) {
                    editCity.plateName = tryGetName(selectedPlace);
                    editCity.lng = selectedPlace.getLongitude();
                    editCity.lat = selectedPlace.getLatitude();
                }
                editCity.putExtraInIntent(result);
                AnalyticsTrackers.getInstance(EditCityActivity.this).logEditCity();
            } else {
                City c = cities.addNewCity();
                c.prayerCalcMethod = prayerTimeCalcMethod;
                c.asrPrayerCalcMethod = asrPrayerTimeCalcMethod;
                c.country = country;
                c.name = city;
                c.timezone = Integer.parseInt(timezone);
                c.minMinuteAfterFajr = Integer.parseInt(minMinuteAfterFajr);
                c.minMinutesBeforeSunrise = Integer.parseInt(minMinutesBeforeSunrise);
                c.plateName = tryGetName(selectedPlace);
                c.lng = selectedPlace.getLongitude();
                c.lat = selectedPlace.getLatitude();
                c.putExtraInIntent(result);
                AnalyticsTrackers.getInstance(EditCityActivity.this).logAddCity();
            }
            setResult(RESULT_OK, result);
            finish();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                selectedPlace = data.getParcelableExtra(Constants.ADDRESS_INTENT);
                if (selectedPlace != null) {
                    selectedPlaceTextView.setText(tryGetName(selectedPlace));
                    AnalyticsTrackers.getInstance(EditCityActivity.this).logPickedLocation();
                }
            }
        }
    }

    private String tryGetName(@NonNull AddressData data) {
        List<Address> addressList = data.getAddressList();
        String address = null;
        if (addressList != null && addressList.size() > 0) {
            Address a = addressList.get(0);
            address = a.getThoroughfare();
//            if (address == null)
//                address = a.getSubLocality();
//            if (address == null)
//                address = a.getSubAdminArea();
            if (address == null)
                address = a.getLocality();
            if (address == null)
                address = a.getAdminArea();
            if (address == null)
                address = a.getCountryName();
        }
        if (address != null)
            return address;
        return getString(R.string.city);
    }
}
