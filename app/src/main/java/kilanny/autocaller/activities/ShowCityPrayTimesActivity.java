package kilanny.autocaller.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import kilanny.autocaller.App;
import kilanny.autocaller.R;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.utils.PrayTimes;

public class ShowCityPrayTimesActivity extends AppCompatActivity {

    private int cityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_city_pray_times);
        cityId = getIntent().getIntExtra("cityId", 0);
        City city = AppDb.getInstance(this).cityDao().find(cityId);
        getSupportActionBar().setTitle(city.country + " - " + city.name);

        Date now = new Date();
        int myTimezone = TimeZone.getDefault().getOffset(now.getTime()) / (1000 * 60 * 60);
        int diffTimezone = myTimezone - city.timezone;
        int timeZoneOffset = 1000 * 60 * 60 * city.timezone;
        TimeZone timeZone = TimeZone.getTimeZone(TimeZone.getAvailableIDs(timeZoneOffset)[0]);
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(now);
        PrayTimes prayers = new PrayTimes();
        prayers.setTimeFormat(PrayTimes.TIME_Time24);
        prayers.setCalcMethod(city.prayerCalcMethod);
        prayers.setAsrJuristic(city.asrPrayerCalcMethod);
        prayers.setAdjustHighLats(PrayTimes.ADJ_AngleBased);
        int[] offsets = {0, 0, 0, 0, 0, 0, 0}; // {Fajr,Sunrise,Dhuhr,Asr,Sunset,Maghrib,Isha}
        prayers.tune(offsets);

        ArrayList<String> prayerTimes = prayers.getPrayerTimes(calendar,
                city.lat, city.lng, city.timezone);
        prayerTimes.remove(4);
        int[] prayerTxts = { R.id.txtFajr, R.id.txtSunrise, R.id.txtDhuhr, R.id.txtAsr,
                R.id.txtMagrib, R.id.txtIsha };
        for (int i = 0; i < prayerTimes.size(); ++i) {
            TextView textView = findViewById(prayerTxts[i]);
            String[] s = prayerTimes.get(i).split(":");
            int hour = (Integer.parseInt(s[0]) + diffTimezone) % 24;
            textView.setText(String.format(Locale.ENGLISH, "%02d", hour) + ":" + s[1]);
        }
    }
}
