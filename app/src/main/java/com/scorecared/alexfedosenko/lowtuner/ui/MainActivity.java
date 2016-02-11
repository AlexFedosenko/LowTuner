package com.scorecared.alexfedosenko.lowtuner.ui;

import android.content.Context;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.scorecared.alexfedosenko.lowtuner.AudioReceiver;
import com.scorecared.alexfedosenko.lowtuner.R;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getMicrophoneAvailable(this)) {
            AudioReceiver.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioReceiver.stop();
    }

    public static boolean getMicrophoneAvailable(Context context) {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        recorder.setOutputFile(new File(context.getCacheDir(), "MediaUtil#micAvailTestFile").getAbsolutePath());
        boolean available = true;
        try {
            recorder.prepare();
        }
        catch (IOException exception) {
            available = false;
        }
        recorder.release();
        return available;
    }
}
