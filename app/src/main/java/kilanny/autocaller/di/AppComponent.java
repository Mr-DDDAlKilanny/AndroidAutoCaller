package kilanny.autocaller.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import kilanny.autocaller.App;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.serializers.Serializer;

/**
 * Created by user on 11/4/2017.
 */

@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(App app);

    ListOfCallingLists getListOfCallingLists();

    CityList getCityList();

    @ApplicationContext
    Context getContext();

    App getApp();
}
