package kilanny.autocaller.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import kilanny.autocaller.App;
import kilanny.autocaller.serializers.BinarySerializer;
import kilanny.autocaller.serializers.Serializer;

/**
 * Created by user on 11/4/2017.
 */
@Module
public class AppModule {

    private final App app;

    public AppModule(App app) {
        this.app = app;
    }

    @Provides
    Serializer provideSerializer() {
        return new BinarySerializer();
    }

    @Provides
    @ApplicationContext
    Context provideContext() {
        return app;
    }

    @Provides
    App provideApp() {
        return app;
    }
}
