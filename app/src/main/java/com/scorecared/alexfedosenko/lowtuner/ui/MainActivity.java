package com.scorecared.alexfedosenko.lowtuner.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.scorecared.alexfedosenko.lowtuner.AudioReceiver;
import com.scorecared.alexfedosenko.lowtuner.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AudioReceiver.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioReceiver.stop();
    }
}
