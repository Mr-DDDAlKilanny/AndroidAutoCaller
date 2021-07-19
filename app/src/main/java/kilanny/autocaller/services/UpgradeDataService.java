package kilanny.autocaller.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import kilanny.autocaller.R;
import kilanny.autocaller.activities.UpgradeDataActivity;
import kilanny.autocaller.data.AutoCallLog;
import kilanny.autocaller.data.AutoCallProfile;
import kilanny.autocaller.data.AutoCallProfileList;
import kilanny.autocaller.data.City;
import kilanny.autocaller.data.CityList;
import kilanny.autocaller.data.ContactsList;
import kilanny.autocaller.data.ContactsListGroup;
import kilanny.autocaller.data.ContactsListItem;
import kilanny.autocaller.data.ListOfCallingLists;
import kilanny.autocaller.db.AppDb;
import kilanny.autocaller.db.CallSession;
import kilanny.autocaller.db.CallSessionItem;
import kilanny.autocaller.db.ContactInGroup;
import kilanny.autocaller.db.ContactInList;
import kilanny.autocaller.db.ContactList;

public class UpgradeDataService extends Service {

    private static final int NOTIFICATION_ID = 611;
    private static final String CHANNEL_ID = "kilanny.autocaller.services.UpgradeDataService";
    public static final String ACTION_FINISH = "kilanny.autocaller.services.UpgradeDataService/finish";

    private final BroadcastReceiver mDelNotifBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CHANNEL_ID.equals(intent.getAction())) {
                try {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, initNotification());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    };

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelName = "Upgrade data Service";
        NotificationChannel channel = new NotificationChannel(UpgradeDataService.CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.BLUE);
        channel.enableLights(true);
        channel.setDescription(channelName);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(channel);
        return UpgradeDataService.CHANNEL_ID;
    }

    private Intent getStartActivityIntent() {
        Intent intent = new Intent(this, UpgradeDataActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        return intent;
    }

    private Notification initNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                1,
                getStartActivityIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this,
                    createNotificationChannel());
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_upgrade_data);

        return notificationBuilder.setContentIntent(pendingIntent)
                .setContent(remoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setChannelId(CHANNEL_ID)
                .setUsesChronometer(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setFullScreenIntent(pendingIntent, true)
                //Hawawii devices can still cancel ongoing notification!!? so listen for that
                .setDeleteIntent(PendingIntent.getBroadcast(this, 3,
                        new Intent(CHANNEL_ID),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, initNotification());
        registerReceiver(mDelNotifBroadcastReceiver, new IntentFilter(CHANNEL_ID));

        new Thread(this::start).start();

        return START_REDELIVER_INTENT;
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private void start() {
        ListOfCallingLists listOfCallingLists = ListOfCallingLists.getInstance(this);
        CityList cities = new CityList(this);
        AutoCallProfileList profiles = new AutoCallProfileList(this);
//        cities.save(this);
//        profiles.save(this);
        AppDb db = AppDb.getInstance(this);
        Map<Integer, Long> cityId = new HashMap<>();
        Map<Integer, Long> profileId = new HashMap<>();
        db.runInTransaction(() -> {
            for (City city : cities) {
                long id = db.cityDao().insert(city);
                cityId.put(city.id, id);
            }
            for (AutoCallProfile profile : profiles) {
                long id = db.callProfileDao().insert(profile);
                profileId.put(profile.id, id);
            }
            Map<String, Long> numbers = new HashMap<>();
            for (int i = 0; i < listOfCallingLists.size(); ++i) {
                ContactsList list = listOfCallingLists.get(i);
                ContactList contactList = new ContactList();
                contactList.name = list.getName();
                contactList.id = db.contactListDao().insert(contactList);
                Collections.sort(list, (o1, o2) -> o1.index - o2.index);
                for (int j = 0; j < list.size(); ++j) {
                    ContactsListItem contactsListItem = list.get(j);
                    if (numbers.get(contactsListItem.number) == null) {
                        if (contactsListItem.cityId != null) {
                            Long l = cityId.get(contactsListItem.cityId);
                            if (l != null) {
                                contactsListItem.cityId = (int) l.longValue();
                            } else {
                                contactsListItem.cityId = null;
                            }
                        }
                        if (contactsListItem.callProfileId != null) {
                            Long l = profileId.get(contactsListItem.callProfileId);
                            if (l != null) {
                                contactsListItem.callProfileId = (int) l.longValue();
                            } else {
                                contactsListItem.callProfileId = null;
                            }
                        }
                        contactsListItem.id = db.contactDao().insert(contactsListItem);
                        numbers.put(contactsListItem.number, contactsListItem.id);
                    }
                    ContactInList contactInList = new ContactInList();
                    contactInList.callCount = contactsListItem.callCount;
                    contactInList.index = j;
                    contactInList.contactId = numbers.get(contactsListItem.number);
                    contactInList.listId = contactList.id;
                    db.contactInListDao().insert(contactInList);
                }
                for (ContactsListGroup group : list.getGroups()) {
                    group.contactListId = contactList.id;
                    group.id = db.contactGroupDao().insert(group);
                    for (Map.Entry<String, String> entry : group.contacts.entrySet()) {
                        Long contactId = numbers.get(entry.getKey());
                        if (contactId != null) {
                            ContactInGroup contactInGroup = new ContactInGroup();
                            contactInGroup.contactId = contactId;
                            contactInGroup.groupId = group.id;
                            db.contactInGroupDao().insert(contactInGroup);
                        }
                    }
                }
                for (AutoCallLog.AutoCallSession session : list.getLog().sessions) {
                    CallSession callSession = new CallSession();
                    callSession.date = session.date.getTime();
                    callSession.listId = contactList.id;
                    callSession.id = db.callSessionDao().insert(callSession);
                    for (AutoCallLog.AutoCallItem item : session) {
                        CallSessionItem callSessionItem = new CallSessionItem();
                        callSessionItem.callSessionId = callSession.id;
                        String number = null, name = null;
                        //noinspection StatementWithEmptyBody
                        if (item instanceof AutoCallLog.AutoCallRetry) {
                        } else if (item instanceof AutoCallLog.AutoCall) {
                            callSessionItem.date = ((AutoCallLog.AutoCall) item).date.getTime();
                            number = ((AutoCallLog.AutoCall) item).number;
                            name = ((AutoCallLog.AutoCall) item).name;
                            callSessionItem.contactId = numbers.get(((AutoCallLog.AutoCall) item).number);
                            callSessionItem.result = ((AutoCallLog.AutoCall) item).result;
                        } else if (item instanceof AutoCallLog.AutoCallIgnored) {
                            callSessionItem.date = ((AutoCallLog.AutoCallIgnored) item).date.getTime();
                            number = ((AutoCallLog.AutoCallIgnored) item).number;
                            name = ((AutoCallLog.AutoCallIgnored) item).name;
                            callSessionItem.contactId = numbers.get(((AutoCallLog.AutoCallIgnored) item).number);
                            callSessionItem.result = (int) ((AutoCallLog.AutoCallIgnored) item).result;
                            if (callSessionItem.result > 0)
                                callSessionItem.result += 2;
                        }

                        // special: deleted contact item history
                        if (callSessionItem.contactId == null && callSessionItem.result != null) {
                            ContactsListItem contactsListItem = new ContactsListItem();
                            contactsListItem.number = number;
                            contactsListItem.name = name;
                            contactsListItem.id = db.contactDao().insert(contactsListItem);
                            numbers.put(contactsListItem.number, contactsListItem.id);
                            callSessionItem.contactId = numbers.get(number);
                        }
                        db.callSessionItemDao().insert(callSessionItem);
                    }
                }
            }
        });

        sendBroadcast(new Intent(ACTION_FINISH));
        myStopSelf();
    }

    private void myStopSelf() {
        try {
            unregisterReceiver(mDelNotifBroadcastReceiver);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public class LocalBinder extends Binder {
        public UpgradeDataService getService() {
            return UpgradeDataService.this;
        }
    }
}
