package com.screenkeeper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
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
    private WindowManager windowManager;
    private android.view.View overlayView;

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

        try {
            Notification notification = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            // 部分 OEM 设备前台服务启动失败，降级处理
            isRunning = false;
            stopSelf();
            return;
        }

        // 创建悬浮窗，保持屏幕常亮（后台也生效）
        createOverlayWindow();

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
        removeOverlayWindow();
        stopForeground(true);
        stopSelf();
    }

    private void createOverlayWindow() {
        if (overlayView != null) return;
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            // 创建一个 1x1 像素的透明悬浮窗，带 FLAG_KEEP_SCREEN_ON
            overlayView = new android.view.View(this);
            overlayView.setBackgroundColor(Color.TRANSPARENT);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    1, 1,  // 1x1 像素，几乎不可见
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;

            windowManager.addView(overlayView, params);
        } catch (Exception e) {
            // 悬浮窗创建失败（权限不足或 OEM 限制），不影响服务运行
            overlayView = null;
        }
    }

    private void removeOverlayWindow() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception e) {
                // 忽略
            }
            overlayView = null;
        }
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
        removeOverlayWindow();
        super.onDestroy();
    }
}
