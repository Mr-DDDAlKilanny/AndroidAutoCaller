package kilanny.autocaller.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListItem;

@Database(entities = {
        City.class,
        AutoCallProfile.class,
        ContactsListItem.class,
        ContactList.class,
        ContactsListGroup.class,
        ContactInList.class,
        ContactInGroup.class,
        CallSession.class,
        CallSessionItem.class
}, version = 1, exportSchema = false)
@TypeConverters({ RoomConverters.class })
public abstract class AppDb extends RoomDatabase {
    public static final String DB_NAME = "app-db";
    private static AppDb instance;

    public static AppDb getInstance(Context context) {
        if (instance == null) {
            Context appContext = context.getApplicationContext();
            instance = Room.databaseBuilder(appContext, AppDb.class, DB_NAME)
                    //.addMigrations(MIGRATION_1_2(), MIGRATION_2_3(), MIGRATION_3_4())
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }

    public static boolean exists(Context context) {
        return context.getDatabasePath(DB_NAME).exists();
    }

//    private static Migration MIGRATION_3_4() {
//        return new Migration(3, 4) {
//            @Override
//            public void migrate(@NonNull SupportSQLiteDatabase database) {
//                database.execSQL("ALTER TABLE `video` ADD `order_in_list` INTEGER");
//            }
//        };
//    }

    public abstract CityDao cityDao();
    public abstract CallProfileDao callProfileDao();
    public abstract ContactDao contactDao();
    public abstract ContactListDao contactListDao();
    public abstract ContactGroupDao contactGroupDao();
    public abstract ContactInListDao contactInListDao();
    public abstract ContactInGroupDao contactInGroupDao();
    public abstract CallSessionDao callSessionDao();
    public abstract CallSessionItemDao callSessionItemDao();
}
