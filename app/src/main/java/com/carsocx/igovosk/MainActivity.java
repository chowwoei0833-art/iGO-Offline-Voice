package com.carsocx.igovosk;

import android.Manifest;
import android.app.Activity;
import android.content.res.AssetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Map;

public class MainActivity extends Activity implements RecognitionListener {

    private static final int REQ_MIC = 1001;
    private static final String MODEL_NAME = "vosk-model-small-cn-0.22";

    private TextView statusView;
    private TextView resultView;
    private Button prepareButton;
    private Button listenButton;
    private Button stopButton;

    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private volatile boolean preparingModel = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("iGO Offline Voice - Vosk Test");
        title.setTextSize(26);

        TextView info = new TextView(this);
        info.setText(
                "\n独立测试车机麦克风 + Vosk 中文离线识别。\n" +
                "首次启动会自动准备 APK 内置中文模型。\n" +
                "测试词：你好 / 导航 / 取消导航 / YES\n"
        );
        info.setTextSize(17);

        statusView = new TextView(this);
        statusView.setTextSize(17);
        statusView.setText("状态：检查中...");

        resultView = new TextView(this);
        resultView.setTextSize(22);
        resultView.setText("\n识别结果：\n");

        Button micButton = new Button(this);
        micButton.setText("1. 允许麦克风权限");
        micButton.setOnClickListener(v -> requestMic());

        prepareButton = new Button(this);
        prepareButton.setText("2. 重新准备中文模型");
        prepareButton.setOnClickListener(v -> prepareModel());

        listenButton = new Button(this);
        listenButton.setText("3. 开始监听");
        listenButton.setEnabled(false);
        listenButton.setOnClickListener(v -> startListeningTest());

        stopButton = new Button(this);
        stopButton.setText("停止监听");
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> stopListeningTest());

        Button settingsButton = new Button(this);
        settingsButton.setText("打开语音输入设置");
        settingsButton.setOnClickListener(v -> openVoiceSettings());

        layout.addView(title);
        layout.addView(info);
        layout.addView(statusView);
        layout.addView(micButton);
        layout.addView(prepareButton);
        layout.addView(listenButton);
        layout.addView(stopButton);
        layout.addView(settingsButton);
        layout.addView(resultView);

        setContentView(scroll);
        requestMic();
        updateModelStatus();
        if (!isModelReady()) {
            prepareModel();
        }
    }

    private File getModelDir() {
        return new File(getFilesDir(), MODEL_NAME);
    }

    private boolean isModelReady() {
        File dir = getModelDir();
        return dir.isDirectory()
                && new File(dir, "am/final.mdl").isFile()
                && new File(dir, "conf/model.conf").isFile();
    }

    private void updateModelStatus() {
        if (isModelReady()) {
            statusView.setText("状态：中文模型已准备，可以开始监听。");
            listenButton.setEnabled(true);
            prepareButton.setText("中文模型已准备");
        } else {
            statusView.setText("状态：中文模型未准备。请按“准备中文模型”。");
            listenButton.setEnabled(false);
        }
    }

    private void prepareModel() {
        if (preparingModel) return;
        if (isModelReady()) {
            updateModelStatus();
            return;
        }

        preparingModel = true;
        prepareButton.setEnabled(false);
        listenButton.setEnabled(false);
        statusView.setText("状态：正在从 APK 解压中文模型，请稍候...");

        new Thread(() -> {
            try {
                File dst = getModelDir();
                if (dst.exists()) deleteRecursive(dst);
                copyAssetTree(getAssets(), MODEL_NAME, dst);
                if (!isModelReady()) {
                    throw new IllegalStateException("模型文件不完整");
                }

                runOnUiThread(() -> {
                    preparingModel = false;
                    prepareButton.setEnabled(true);
                    updateModelStatus();
                    Toast.makeText(this, "中文模型准备完成", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    preparingModel = false;
                    prepareButton.setEnabled(true);
                    statusView.setText("状态：模型准备失败：" + e.getMessage());
                });
            }
        }).start();
    }

    private void copyAssetTree(AssetManager assets, String assetPath, File dest) throws Exception {
        String[] children = assets.list(assetPath);
        if (children == null) throw new IllegalStateException("APK 内没有模型：" + assetPath);

        if (children.length == 0) {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("无法创建目录：" + parent);
            }
            try (InputStream in = assets.open(assetPath);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            }
            return;
        }

        if (!dest.exists() && !dest.mkdirs()) {
            throw new IllegalStateException("无法创建目录：" + dest);
        }
        for (String child : children) {
            copyAssetTree(assets, assetPath + "/" + child, new File(dest, child));
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private void startListeningTest() {
        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestMic();
            return;
        }
        if (!isModelReady()) {
            statusView.setText("状态：请先准备中文模型。");
            return;
        }

        stopListeningTest();
        resultView.setText("识别结果：\n");
        statusView.setText("状态：正在加载模型...");
        listenButton.setEnabled(false);

        new Thread(() -> {
            try {
                if (model == null) model = new Model(getModelDir().getAbsolutePath());
                recognizer = new Recognizer(model, 16000.0f);
                speechService = new SpeechService(recognizer, 16000.0f);
                runOnUiThread(() -> {
                    statusView.setText("状态：正在监听，请说话...");
                    stopButton.setEnabled(true);
                    speechService.startListening(this);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusView.setText("状态：启动失败：" + e.getMessage());
                    listenButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        }).start();
    }

    private void stopListeningTest() {
        if (speechService != null) {
            try { speechService.stop(); } catch (Exception ignored) {}
            try { speechService.shutdown(); } catch (Exception ignored) {}
            speechService = null;
        }
        if (recognizer != null) {
            try { recognizer.close(); } catch (Exception ignored) {}
            recognizer = null;
        }
        if (statusView != null && isModelReady()) statusView.setText("状态：已停止。可以再次开始监听。");
        if (listenButton != null) listenButton.setEnabled(isModelReady());
        if (stopButton != null) stopButton.setEnabled(false);
    }

    private void requestMic() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            } else {
                Toast.makeText(this, "麦克风权限已经开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openVoiceSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String parse(String json, String key) {
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> map = new Gson().fromJson(json, type);
            Object value = map.get(key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String text = parse(hypothesis, "partial");
        runOnUiThread(() -> {
            statusView.setText("状态：正在监听...");
            resultView.setText("识别结果（实时）：\n" + text);
        });
    }

    @Override
    public void onResult(String hypothesis) {
        String text = parse(hypothesis, "text");
        runOnUiThread(() -> resultView.setText("识别结果：\n" + text));
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String text = parse(hypothesis, "text");
        runOnUiThread(() -> {
            resultView.setText("最终识别：\n" + text);
            statusView.setText("状态：识别完成。可再次开始监听。");
            listenButton.setEnabled(true);
            stopButton.setEnabled(false);
        });
    }

    @Override
    public void onError(Exception e) {
        runOnUiThread(() -> {
            statusView.setText("状态：识别错误：" + e.getMessage());
            listenButton.setEnabled(true);
            stopButton.setEnabled(false);
        });
    }

    @Override
    public void onTimeout() {
        runOnUiThread(() -> {
            statusView.setText("状态：超时，没有听到有效语音。");
            listenButton.setEnabled(true);
            stopButton.setEnabled(false);
        });
    }

    @Override
    protected void onDestroy() {
        stopListeningTest();
        if (model != null) {
            try { model.close(); } catch (Exception ignored) {}
            model = null;
        }
        super.onDestroy();
    }
}
