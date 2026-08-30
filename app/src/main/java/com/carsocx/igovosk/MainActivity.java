package com.carsocx.igovosk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_MIC = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("iGO Offline Voice");
        title.setTextSize(26);

        TextView info = new TextView(this);
        info.setText(
                "\nVosk Offline Speech Recognition\n\n" +
                "用途：为 iGO 提供离线语音识别服务。\n\n" +
                "1. 允许麦克风权限\n" +
                "2. 设置本程序为语音识别服务\n" +
                "3. 返回 iGO 测试 Voice Test"
        );
        info.setTextSize(17);

        Button micButton = new Button(this);
        micButton.setText("允许麦克风权限");
        micButton.setOnClickListener(v -> requestMic());

        Button settingsButton = new Button(this);
        settingsButton.setText("打开语音输入设置");
        settingsButton.setOnClickListener(v -> openVoiceSettings());

        layout.addView(title);
        layout.addView(info);
        layout.addView(micButton);
        layout.addView(settingsButton);

        setContentView(layout);

        requestMic();
    }

    private void requestMic() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQ_MIC
                );

            } else {
                Toast.makeText(
                        this,
                        "麦克风权限已经开启",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void openVoiceSettings() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_VOICE_INPUT_SETTINGS
            );
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_SETTINGS
                );
                startActivity(intent);
            } catch (Exception ignored) {
                Toast.makeText(
                        this,
                        "无法打开系统设置",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
  }
