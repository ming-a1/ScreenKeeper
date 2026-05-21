package com.screenkeeper;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;

    private SwitchCompat switchKeepScreen;
    private SwitchCompat switchLowBrightness;
    private SeekBar seekBarBrightness;
    private TextView textStatus;
    private TextView textBrightnessValue;
    private CardView cardMain;
    private View indicatorDot;

    private boolean isActive = false;
    private int currentBrightness = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();
        loadState();
        setupListeners();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void initViews() {
        switchKeepScreen = findViewById(R.id.switch_keep_screen);
        switchLowBrightness = findViewById(R.id.switch_low_brightness);
        seekBarBrightness = findViewById(R.id.seekbar_brightness);
        textStatus = findViewById(R.id.text_status);
        textBrightnessValue = findViewById(R.id.text_brightness_value);
        cardMain = findViewById(R.id.card_main);
        indicatorDot = findViewById(R.id.indicator_dot);
    }

    private void loadState() {
        isActive = getSharedPreferences("screen_keeper", MODE_PRIVATE)
                .getBoolean("is_active", false);
        currentBrightness = getSharedPreferences("screen_keeper", MODE_PRIVATE)
                .getInt("brightness", 50);
        switchKeepScreen.setChecked(isActive);
        switchLowBrightness.setChecked(getSharedPreferences("screen_keeper", MODE_PRIVATE)
                .getBoolean("low_brightness", false));
        seekBarBrightness.setProgress(currentBrightness);
        textBrightnessValue.setText(currentBrightness + "%");
    }

    private void saveState() {
        getSharedPreferences("screen_keeper", MODE_PRIVATE).edit()
                .putBoolean("is_active", isActive)
                .putBoolean("low_brightness", switchLowBrightness.isChecked())
                .putInt("brightness", currentBrightness)
                .apply();
    }

    private void setupListeners() {
        switchKeepScreen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startKeepAlive();
            } else {
                stopKeepAlive();
            }
            saveState();
            updateUI();
        });

        switchLowBrightness.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isActive) {
                sendBrightnessToService();
            }
            saveState();
        });

        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBrightness = Math.max(1, progress);
                textBrightnessValue.setText(currentBrightness + "%");
                if (isActive && switchLowBrightness.isChecked()) {
                    sendBrightnessToService();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveState();
            }
        });
    }

    private void startKeepAlive() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            Toast.makeText(this, "请授予悬浮窗权限以使用常亮功能", Toast.LENGTH_LONG).show();
            switchKeepScreen.setChecked(false);
            return;
        }

        isActive = true;

        try {
            Intent serviceIntent = new Intent(this, KeepAliveService.class);
            serviceIntent.setAction(KeepAliveService.ACTION_START);
            serviceIntent.putExtra(KeepAliveService.EXTRA_LOW_BRIGHTNESS, switchLowBrightness.isChecked());
            serviceIntent.putExtra(KeepAliveService.EXTRA_BRIGHTNESS_VALUE, currentBrightness);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "启动服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            isActive = false;
            switchKeepScreen.setChecked(false);
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        saveState();
        Toast.makeText(this, "✅ 屏幕常亮已开启", Toast.LENGTH_SHORT).show();
    }

    private void stopKeepAlive() {
        isActive = false;

        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        serviceIntent.setAction(KeepAliveService.ACTION_STOP);
        startService(serviceIntent);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Toast.makeText(this, "❌ 屏幕常亮已关闭", Toast.LENGTH_SHORT).show();
    }

    private void sendBrightnessToService() {
        Intent intent = new Intent(this, KeepAliveService.class);
        intent.setAction(KeepAliveService.ACTION_BRIGHTNESS);
        intent.putExtra(KeepAliveService.EXTRA_LOW_BRIGHTNESS, switchLowBrightness.isChecked());
        intent.putExtra(KeepAliveService.EXTRA_BRIGHTNESS_VALUE, currentBrightness);
        startService(intent);
    }

    private void updateUI() {
        if (isActive) {
            textStatus.setText("运行中 · 屏幕保持常亮");
            textStatus.setTextColor(getResources().getColor(R.color.green_active));
            indicatorDot.setBackgroundResource(R.drawable.dot_active);
            cardMain.setCardBackgroundColor(getResources().getColor(R.color.card_active));
        } else {
            textStatus.setText("已停止 · 点击上方开关开启");
            textStatus.setTextColor(getResources().getColor(R.color.text_secondary));
            indicatorDot.setBackgroundResource(R.drawable.dot_inactive);
            cardMain.setCardBackgroundColor(getResources().getColor(R.color.card_default));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST) {
            if (Settings.canDrawOverlays(this)) {
                switchKeepScreen.setChecked(true);
            } else {
                Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
