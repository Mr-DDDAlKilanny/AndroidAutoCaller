package kilanny.autocaller.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import kilanny.autocaller.data.ListOfCallingLists;

/**
 * Created by user on 11/4/2017.
 */
@Module
public class ContextModule {
    private Context context;

    public ContextModule(Context context) {
        this.context = context;
    }

    @Provides
    @ActivityOrServiceContext
    Context provideContext() {
        return context;
    }
}
