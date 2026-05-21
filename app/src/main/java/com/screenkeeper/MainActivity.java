package com.screenkeeper;

import android.app.Activity;
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
import androidx.cardview.widget.CardView;

import com.google.android.material.materialswitch.MaterialSwitch;

public class MainActivity extends AppCompatActivity {

    private static final int OVERLAY_PERMISSION_REQUEST = 1001;
    private static final int WRITE_SETTINGS_REQUEST = 1002;

    private MaterialSwitch switchKeepScreen;
    private MaterialSwitch switchLowBrightness;
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
        setContentView(R.layout.activity_main);

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
            if (isChecked && isActive) {
                applyLowBrightness();
            } else if (!isChecked && isActive) {
                resetBrightness();
            }
            saveState();
        });

        seekBarBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentBrightness = Math.max(1, progress); // 最低1%亮度
                textBrightnessValue.setText(currentBrightness + "%");
                if (isActive && switchLowBrightness.isChecked()) {
                    applyLowBrightness();
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
        // 检查悬浮窗权限（用于低亮度覆盖）
        if (switchLowBrightness.isChecked() && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            Toast.makeText(this, "请授予悬浮窗权限以使用低亮度功能", Toast.LENGTH_LONG).show();
            switchKeepScreen.setChecked(false);
            return;
        }

        isActive = true;

        // 启动前台服务
        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        serviceIntent.setAction(KeepAliveService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // 设置当前 Activity 的屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (switchLowBrightness.isChecked()) {
            applyLowBrightness();
        }

        Toast.makeText(this, "✅ 屏幕常亮已开启", Toast.LENGTH_SHORT).show();
    }

    private void stopKeepAlive() {
        isActive = false;

        Intent serviceIntent = new Intent(this, KeepAliveService.class);
        serviceIntent.setAction(KeepAliveService.ACTION_STOP);
        startService(serviceIntent);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        resetBrightness();

        Toast.makeText(this, "❌ 屏幕常亮已关闭", Toast.LENGTH_SHORT).show();
    }

    private void applyLowBrightness() {
        // 设置系统亮度
        try {
            if (Settings.System.canWrite(this)) {
                int systemBrightness = (int) (currentBrightness * 2.55f); // 0-255
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, systemBrightness);
            }
        } catch (Exception e) {
            // 忽略权限错误
        }

        // 通过 WindowManager 设置当前窗口亮度
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = currentBrightness / 100f;
        getWindow().setAttributes(layoutParams);
    }

    private void resetBrightness() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(layoutParams);
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
                switchKeepScreen.setChecked(true); // 重新触发
            } else {
                Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
