package com.carsocx.igovosk;

import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class IgoVoskRecognitionService extends RecognitionService implements RecognitionListener {

    private static final String TAG = "IgoVoskService";
    private String getModelPath() {
    return new File(getFilesDir(), "vosk-model-small-cn-0.22").getAbsolutePath();
    }

    private Callback callback;
    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback callback) {
        this.callback = callback;

        try {
            File modelDir = new File(getModelPath());

            if (!modelDir.exists()) {
                callback.error(SpeechRecognizer.ERROR_CLIENT);
                return;
            }

            if (model == null) {
               model = new Model(getModelPath());
            }

            if (recognizer == null) {
                recognizer = new Recognizer(model, 16000.0f);
            }

            if (speechService != null) {
                speechService.cancel();
                speechService.shutdown();
            }

            speechService = new SpeechService(recognizer, 16000.0f);

            callback.readyForSpeech(new Bundle());
            callback.beginningOfSpeech();

            speechService.startListening(this);

        } catch (Exception e) {
            Log.e(TAG, "start failed", e);

            try {
                callback.error(SpeechRecognizer.ERROR_CLIENT);
            } catch (RemoteException ignored) {
            }
        }
    }

    @Override
    protected void onStopListening(Callback callback) {
        if (speechService != null) {
            speechService.stop();
        }
    }

    @Override
    protected void onCancel(Callback callback) {
        if (speechService != null) {
            speechService.cancel();
        }
    }

    private Bundle resultBundle(String text) {
        ArrayList<String> list = new ArrayList<>();
        list.add(text == null ? "" : text);

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION,
                list
        );

        return bundle;
    }

    private String parse(String json, String key) {
        try {
            Type type = new TypeToken<Map<String, String>>() {}.getType();

            Map<String, String> map =
                    new Gson().fromJson(json, type);

            String value = map.get(key);

            return value == null ? "" : value;

        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onResult(String hypothesis) {
        try {
            callback.results(
                    resultBundle(
                            parse(hypothesis, "text")
                    )
            );

            if (speechService != null) {
                speechService.cancel();
            }

        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        try {
            callback.results(
                    resultBundle(
                            parse(hypothesis, "text")
                    )
            );
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        try {
            callback.partialResults(
                    resultBundle(
                            parse(hypothesis, "partial")
                    )
            );
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onError(Exception e) {
        try {
            callback.error(
                    SpeechRecognizer.ERROR_CLIENT
            );
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onTimeout() {
        try {
            if (callback != null) {
                callback.results(
                        resultBundle("")
                );
            }
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public void onDestroy() {

        if (speechService != null) {
            speechService.cancel();
            speechService.shutdown();
        }

        if (recognizer != null) {
            recognizer.close();
        }

        if (model != null) {
            model.close();
        }

        super.onDestroy();
    }
}
