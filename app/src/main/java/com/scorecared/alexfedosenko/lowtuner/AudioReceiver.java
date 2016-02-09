package com.scorecared.alexfedosenko.lowtuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioReceiver {

    private static final Thread mPollingAudioThread = new Thread() {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            int n = AudioRecord.getMinBufferSize(10000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, 10000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 100000);
            record.startRecording();
            short[][] buffers = new short[256][160];
            int ix = 0;

            do {
                short[] buffer = buffers[ix++ % buffers.length];
                n = record.read(buffer, 0, buffer.length);
            } while (!interrupted());
            record.stop();
            record.release();
        }


    };

    public static void start() {
        mPollingAudioThread.start();
    }

    public static void stop() {
        mPollingAudioThread.interrupt();
    }
}
