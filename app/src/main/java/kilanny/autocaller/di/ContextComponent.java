package kilanny.autocaller.di;

import android.content.Context;

import dagger.Component;
import kilanny.autocaller.activities.CallListsActivity;
import kilanny.autocaller.activities.CitiesActivity;
import kilanny.autocaller.activities.EditCityActivity;
import kilanny.autocaller.activities.EditGroupsActivity;
import kilanny.autocaller.activities.MainActivity;
import kilanny.autocaller.activities.ShowCityPrayTimesActivity;
import kilanny.autocaller.activities.ShowLogActivity;
import kilanny.autocaller.services.AutoCallService;

/**
 * Created by user on 11/4/2017.
 */
@PerActivityOrService
@Component(dependencies = AppComponent.class, modules = ContextModule.class)
public interface ContextComponent {
    @ActivityOrServiceContext
    Context getContext();

    void inject(ShowLogActivity showLogActivity);
    void inject(AutoCallService autoCallService);
    void inject(CallListsActivity callListsActivity);
    void inject(EditGroupsActivity editGroupsActivity);
    void inject(MainActivity mainActivity);
    void inject(CitiesActivity citiesActivity);
    void inject(EditCityActivity editCityActivity);
    void inject(ShowCityPrayTimesActivity showCityPrayTimesActivity);
}