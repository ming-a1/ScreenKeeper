package com.screenkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("screen_keeper", Context.MODE_PRIVATE);
            boolean wasActive = prefs.getBoolean("is_active", false);

            if (wasActive) {
                Intent serviceIntent = new Intent(context, KeepAliveService.class);
                serviceIntent.setAction(KeepAliveService.ACTION_START);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
