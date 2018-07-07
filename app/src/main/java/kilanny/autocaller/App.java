package kilanny.autocaller;

import android.app.Application;
import android.content.Context;

import java.util.Date;

import kilanny.autocaller.di.AppComponent;
import kilanny.autocaller.di.AppModule;

/**
 * Created by user on 11/4/2017.
 */
public class App extends Application {

    private AppComponent appComponent;

    public Date lastOutgoingCallStartRinging;
    public String lastCallNumber, lastCallName;
    public int lastCallCurrentCount, lastCallTotalCount;
    public boolean verifiedByOutgoingReceiver;

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (appComponent == null)
            appComponent = kilanny.autocaller.di.DaggerAppComponent
                    .builder()
                    .appModule(new AppModule(this))
                    .build();
        appComponent.inject(this);
    }

    public AppComponent getComponent() {
        return appComponent;
    }
}