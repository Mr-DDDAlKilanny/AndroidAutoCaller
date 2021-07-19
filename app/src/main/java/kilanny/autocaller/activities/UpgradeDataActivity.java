package kilanny.autocaller.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import kilanny.autocaller.R;
import kilanny.autocaller.services.UpgradeDataService;
import kilanny.autocaller.utils.OsUtils;

public class UpgradeDataActivity extends AppCompatActivity implements ServiceConnection {

    private UpgradeDataService.LocalBinder mBinder;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onBackPressed() {
        // no return back
    }

    @Override
    protected void onDestroy() {
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(mBroadcastReceiver);
                mBroadcastReceiver = null;
                startActivity(new Intent(context, CallListsActivity.class));
                finish();
            }
        }, new IntentFilter(UpgradeDataService.ACTION_FINISH));
        setContentView(R.layout.activity_data_upgrade);
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            private int mCounter = 0;

            @Override
            public void run() {
                if (OsUtils.isServiceRunning(UpgradeDataActivity.this, UpgradeDataService.class)) {
                    t.cancel();
                    bindService(new Intent(UpgradeDataActivity.this, UpgradeDataService.class),
                            UpgradeDataActivity.this, BIND_ABOVE_CLIENT);
                } else if (++mCounter > 50) {
                    t.cancel();
                    startActivity(new Intent(getApplicationContext(), CallListsActivity.class));
                    finish();
                }
            }
        }, 100, 100);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = (UpgradeDataService.LocalBinder) service;
        Log.d("upgradeData", "Activity has bounded to the service");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBinder = null;
    }
}
