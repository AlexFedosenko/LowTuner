package com.scorecared.alexfedosenko.lowtuner;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.math3.complex.Complex;

public class AudioReceiver {

    private static final String TAG = "AudioReceiver";
    private static final AsyncTask<Void, Double, Void> mAudioTask = new AsyncTask<Void, Double, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            AudioRecord record = findAudioRecord();
            record.startRecording();
            byte[][] buffers = new byte[256][8192];
            int ix = 0;

            do {
                byte[] buffer = buffers[ix++ % buffers.length];
                record.read(buffer, 0, buffer.length);
                calculateFFT(buffer);
            } while (!isCancelled());
            record.stop();
            record.release();
            return null;
        }

        private int[] mSampleRates = new int[] { 44100, 22050, 11025, 8000 };
        public AudioRecord findAudioRecord() {
            for (int rate : mSampleRates) {
                for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                    for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                        try {
                            Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                    + channelConfig);
                            int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                            if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                                // check if we can instantiate and have a success
                                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                                if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                    return recorder;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, rate + "Exception, keep trying.",e);
                        }
                    }
                }
            }
            return null;
        }

        public double[] calculateFFT(byte[] signal)
        {
            final int mNumberOfFFTPoints = 4096;
            double mMaxFFTSample;
            int mPeakPos;

            double temp;
            double[] y;
            Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
            double[] absSignal = new double[mNumberOfFFTPoints / 2];

            for(int i = 0; i < mNumberOfFFTPoints; i++){
                temp = (double)((signal[2*i] & 0xFF) | (signal[2*i+1] << 8)) / 32768.0F;
                complexSignal[i] = new Complex(temp,0.0);
            }

            y = gaus(fft(complexSignal, true));
//            y = FFT.fft(complexSignal); // --> Here I use FFT class

            mMaxFFTSample = 0.0;
            mPeakPos = 0;
            try {
                for (int i = 0; i < (mNumberOfFFTPoints / 2); i++) {
                    absSignal[i] = Math.sqrt(Math.pow(y[i * 2], 2) + Math.pow(y[i * 2 + 1], 2));
                    if (absSignal[i] > mMaxFFTSample) {
                        mMaxFFTSample = absSignal[i];
                        mPeakPos = i;
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "End of the array", e);
            }

            double result = mPeakPos * 44100.0 / 2048;
            if (result <= 5000) {
                Log.w(TAG, "Frequency = " + mPeakPos * 44100.0 / mNumberOfFFTPoints);
            }

            return y;

        }

        public double gausse(double n, double frameSize)
        {
            double a = (frameSize - 1)/2;
            double t = (n - a)/(0.5*a);
            t = t*t;
            return Math.exp(-t/2);
        }

        private double[] gaus(double[] n) {
            double[] result = new double[n.length];
            for (int i = 0; i < n.length; i++) {
                result[i] = gausse(n[i], 8192);
            }
            return result;
        }
        /**
         * The Fast Fourier Transform (generic version, with NO optimizations).
         *
         * @param inputComplex
         *            an array of length n of complex values
         * @param DIRECT
         *            TRUE = direct transform, FALSE = inverse transform
         * @return a new array of length 2n
         */
        public double[] fft(final Complex[] inputComplex,
                            boolean DIRECT) {
            // - n is the dimension of the problem
            // - nu is its logarithm in base e
            int n = inputComplex.length;

            // If n is a power of 2, then ld is an integer (_without_ decimals)
            double ld = Math.log(n) / Math.log(2.0);

            // Here I check if n is a power of 2. If exist decimals in ld, I quit
            // from the function returning null.
            if ((n & (n - 1)) != 0) {
                System.out.println("The number of elements is not a power of 2.");
                return null;
            }
            if (((int) ld) - ld != 0) {
                System.out.println("The number of elements is not a power of 2.");
                return null;
            }

            // Declaration and initialization of the variables
            // ld should be an integer, actually, so I don't lose any information in
            // the cast
            int nu = (int) ld;
            int n2 = n / 2;
            int nu1 = nu - 1;
            double[] xReal = new double[n];
            double[] xImag = new double[n];
            double tReal, tImag, p, arg, c, s;

            // Here I check if I'm going to do the direct transform or the inverse
            // transform.
            double constant;
            if (DIRECT)
                constant = -2 * Math.PI;
            else
                constant = 2 * Math.PI;

            // I don't want to overwrite the input arrays, so here I copy them. This
            // choice adds \Theta(2n) to the complexity.
            for (int i = 0; i < n; i++) {
                xReal[i] = inputComplex[i].getReal();
                xImag[i] = inputComplex[i].getImaginary();
            }

            // First phase - calculation
            int k = 0;
            for (int l = 1; l <= nu; l++) {
                while (k < n) {
                    for (int i = 1; i <= n2; i++) {
                        p = bitreverseReference(k >> nu1, nu);
                        // direct FFT or inverse FFT
                        arg = constant * p / n;
                        c = Math.cos(arg);
                        s = Math.sin(arg);
                        tReal = xReal[k + n2] * c + xImag[k + n2] * s;
                        tImag = xImag[k + n2] * c - xReal[k + n2] * s;
                        xReal[k + n2] = xReal[k] - tReal;
                        xImag[k + n2] = xImag[k] - tImag;
                        xReal[k] += tReal;
                        xImag[k] += tImag;
                        k++;
                    }
                    k += n2;
                }
                k = 0;
                nu1--;
                n2 /= 2;
            }

            // Second phase - recombination
            k = 0;
            int r;
            while (k < n) {
                r = bitreverseReference(k, nu);
                if (r > k) {
                    tReal = xReal[k];
                    tImag = xImag[k];
                    xReal[k] = xReal[r];
                    xImag[k] = xImag[r];
                    xReal[r] = tReal;
                    xImag[r] = tImag;
                }
                k++;
            }

            // Here I have to mix xReal and xImag to have an array (yes, it should
            // be possible to do this stuff in the earlier parts of the code, but
            // it's here to readibility).
            double[] newArray = new double[xReal.length * 2];
            double radice = 1 / Math.sqrt(n);
            for (int i = 0; i < newArray.length; i += 2) {
                int i2 = i / 2;
                // I used Stephen Wolfram's Mathematica as a reference so I'm going
                // to normalize the output while I'm copying the elements.
                newArray[i] = xReal[i2] * radice;
                newArray[i + 1] = xImag[i2] * radice;
            }
            return newArray;
        }

        /**
         * The reference bitreverse function.
         */
        private int bitreverseReference(int j, int nu) {
            int j2;
            int j1 = j;
            int k = 0;
            for (int i = 1; i <= nu; i++) {
                j2 = j1 / 2;
                k = 2 * k + j1 - 2 * j2;
                j1 = j2;
            }
            return k;
        }
    };

    public static void start() {
        mAudioTask.execute();
    }

    public static void stop() {
        mAudioTask.cancel(true);
    }
}
