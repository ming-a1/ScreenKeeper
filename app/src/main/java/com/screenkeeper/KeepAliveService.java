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
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;

public class KeepAliveService extends Service {

    public static final String ACTION_START = "com.screenkeeper.START";
    public static final String ACTION_STOP = "com.screenkeeper.STOP";
    public static final String ACTION_BRIGHTNESS = "com.screenkeeper.BRIGHTNESS";
    public static final String EXTRA_LOW_BRIGHTNESS = "low_brightness";
    public static final String EXTRA_BRIGHTNESS_VALUE = "brightness_value";

    private static final String CHANNEL_ID = "screen_keeper_channel";
    private static final int NOTIFICATION_ID = 1001;

    private Handler handler;
    private Runnable keepAliveRunnable;
    private boolean isRunning = false;
    private WindowManager windowManager;
    private android.view.View overlayView;
    private WindowManager.LayoutParams overlayParams;

    private boolean lowBrightnessEnabled = false;
    private int brightnessValue = 50;

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
                lowBrightnessEnabled = intent.getBooleanExtra(EXTRA_LOW_BRIGHTNESS, false);
                brightnessValue = intent.getIntExtra(EXTRA_BRIGHTNESS_VALUE, 50);
                startKeepAlive();
            } else if (ACTION_STOP.equals(action)) {
                stopKeepAlive();
            } else if (ACTION_BRIGHTNESS.equals(action)) {
                lowBrightnessEnabled = intent.getBooleanExtra(EXTRA_LOW_BRIGHTNESS, lowBrightnessEnabled);
                brightnessValue = intent.getIntExtra(EXTRA_BRIGHTNESS_VALUE, brightnessValue);
                applyBrightness();
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
            isRunning = false;
            stopSelf();
            return;
        }

        createOverlayWindow();
        applyBrightness();

        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                handler.postDelayed(this, 30000);
            }
        };
        handler.post(keepAliveRunnable);
    }

    private void stopKeepAlive() {
        isRunning = false;
        if (handler != null && keepAliveRunnable != null) {
            handler.removeCallbacks(keepAliveRunnable);
        }
        resetBrightness();
        removeOverlayWindow();
        stopForeground(true);
        stopSelf();
    }

    private void applyBrightness() {
        if (!isRunning) return;

        // 通过悬浮窗设置屏幕亮度（后台生效）
        if (overlayParams != null && lowBrightnessEnabled) {
            overlayParams.screenBrightness = brightnessValue / 100f;
            try {
                if (overlayView != null && windowManager != null) {
                    windowManager.updateViewLayout(overlayView, overlayParams);
                }
            } catch (Exception e) {
                // 忽略
            }
        }

        // 同时写入系统亮度（需要 WRITE_SETTINGS 权限）
        if (lowBrightnessEnabled && Settings.System.canWrite(this)) {
            try {
                int systemBrightness = (int) (brightnessValue * 2.55f);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, systemBrightness);
            } catch (Exception e) {
                // 部分 OEM 设备写入失败，忽略
            }
        }
    }

    private void resetBrightness() {
        if (overlayParams != null) {
            overlayParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            try {
                if (overlayView != null && windowManager != null) {
                    windowManager.updateViewLayout(overlayView, overlayParams);
                }
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    private void createOverlayWindow() {
        if (overlayView != null) return;
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

            overlayView = new android.view.View(this);
            overlayView.setBackgroundColor(Color.TRANSPARENT);

            int layoutType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                layoutType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            overlayParams = new WindowManager.LayoutParams(
                    1, 1,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT);
            overlayParams.gravity = Gravity.TOP | Gravity.START;
            overlayParams.x = 0;
            overlayParams.y = 0;

            windowManager.addView(overlayView, overlayParams);
        } catch (Exception e) {
            overlayView = null;
            overlayParams = null;
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
            overlayParams = null;
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String contentText = lowBrightnessEnabled
                ? "屏幕常亮 · 亮度 " + brightnessValue + "%"
                : "屏幕保持点亮状态，点击查看详情";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("屏幕常亮中")
                .setContentText(contentText)
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
