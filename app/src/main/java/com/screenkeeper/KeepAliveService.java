package com.screenkeeper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

public class KeepAliveService extends Service {

    public static final String ACTION_START = "com.screenkeeper.START";
    public static final String ACTION_STOP = "com.screenkeeper.STOP";
    private static final String CHANNEL_ID = "screen_keeper_channel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler handler;
    private Runnable keepAliveRunnable;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startKeepAlive();
            } else if (ACTION_STOP.equals(action)) {
                stopKeepAlive();
            }
        }
        return START_STICKY;
    }

    private void startKeepAlive() {
        if (isRunning) return;
        isRunning = true;

        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        // 定期唤醒，防止系统休眠
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                // 保持服务活跃
                handler.postDelayed(this, 30000); // 每30秒唤醒一次
            }
        };
        handler.post(keepAliveRunnable);
    }

    private void stopKeepAlive() {
        isRunning = false;
        if (handler != null && keepAliveRunnable != null) {
            handler.removeCallbacks(keepAliveRunnable);
        }
        stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("屏幕常亮中")
                .setContentText("屏幕保持点亮状态，点击查看详情")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "屏幕常亮服务",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持屏幕常亮的后台服务通知");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (handler != null && keepAliveRunnable != null) {
            handler.removeCallbacks(keepAliveRunnable);
        }
        super.onDestroy();
    }
}
