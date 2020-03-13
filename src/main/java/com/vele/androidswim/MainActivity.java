package com.vele.androidswim;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class MainActivity extends AppCompatActivity {

    MediaRecorder mediaRecorder;
    AudioRecord audioRecord1;
    AudioRecord audioRecord2;
    AudioTrack audioTrack;
    short[] buffer1;
    short[] buffer2;

    float[] medians;
    int runs;

    int reqFreq = 44100;

    int recBufferSize;
    int writeBufferSize;

    Height height;

    View dot;

    Thread thread1;
    Thread thread2;
    Thread thread3;

    int offset;
    float[] cos;
    short[] samples;

    final CyclicBarrier gate = new CyclicBarrier(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mediaRecorder = new MediaRecorder();
        recBufferSize = AudioRecord.getMinBufferSize(reqFreq, MediaRecorder.AudioSource.MIC,
                AudioFormat.ENCODING_PCM_16BIT);

        recBufferSize = 100*441;
        writeBufferSize = 100*441;

        audioRecord1 = new AudioRecord(MediaRecorder.AudioSource.MIC, reqFreq,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                recBufferSize);
//        audioRecord2 = new AudioRecord(MediaRecorder.AudioSource.MIC, reqFreq,
//                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
//                bufferSize);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, reqFreq, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, writeBufferSize, AudioTrack.MODE_STREAM);

        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        dot = (View)findViewById(R.id.CircleView);

        height = new Height();
        height.set(0);

        medians = new float[1000];
        runs = 0;

        height.setOnHeightChangeListener(new OnHeightChangeListener() {
            @Override
            public void onFloatChanged(float newValue) {
                dot.setY(newValue);
            }
        });

        cos = new float[writeBufferSize];
        samples = new short[writeBufferSize];
        int freq = 4410;
        int i;
        for (i = 0; i < writeBufferSize; i++){
            cos[i] = (float)Math.cos(2*Math.PI*freq*i/reqFreq);
            samples[i] = (short)(cos[i] * 10000);
        }

        offset = 0;

        startRecording();

        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    gate.await();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(true){
                    audioTrack.write(samples, 0, writeBufferSize);
//                    audioRecord1.read(buffer1, 0, recBufferSize);
//
//                    offset = (offset + buffer1.length) % reqFreq;
//
//                    //buffer3 = smoothArray(buffer3, 1000);
//
//                    // Using Average
////                    float total = 0;
////                    for (i = 0; i < buffer3.length; i++) {
////                        total += buffer3[i];
////                    }
////                    float average = total/buffer3.length;
////
////                    height.set(average/10000-100);
//
////                    medians[runs] = median;
////                    runs++;
////                    if (runs == 1000){
////                        System.out.println(runs);
////                    }
//                    //height.set((float)(median/Math.sqrt(buffer3.length)));
                }
            }
        });

        thread2 = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    gate.await();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while(true) {
                    short[] buffer1Cp = buffer1.clone();
                    int i;
                    float total = 0;
                    float[] buffer3 = new float[recBufferSize];
                    for (i = 0; i < buffer1Cp.length; i++) {
                        float temp1 = (float) buffer1Cp[i];
                        float temp2 = (float) samples[i];
                        buffer3[i] = temp1 * temp2;
                        total += buffer3[i];
                    }

                    float avg = total/buffer1Cp.length/30000;

                    height.set((short)avg);

//                    // Using Median
//                    Arrays.sort(buffer3);
//                    float median;
//                    if (buffer3.length % 2 == 0)
//                        median = (buffer3[buffer3.length / 2] + buffer3[buffer3.length / 2 - 1]) / 2;
//                    else
//                        median = buffer3[buffer3.length / 2];
//                    height.set(median);
                }
            }
        });

        thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    gate.await();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(true){
                    audioRecord1.read(buffer1, 0, recBufferSize);
                }
            }
        });

        thread1.start();
        thread2.start();
        thread3.start();

        try {
            gate.await();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    float[] smoothArray( float[] values, float smoothing ){
        float value = values[0]; // start with the first input
        for (int i = 1; i < values.length; i++){
            float currentValue = values[i];
            value += (currentValue - value) / smoothing;
            values[i] = value;
        }
        return values;
    }

    void startRecording(){
        buffer1 = new short[recBufferSize];
        //buffer2 = new short[bufferSize];
        while(audioRecord1.getState() == AudioRecord.STATE_UNINITIALIZED){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        while(audioRecord2.getState() == AudioRecord.STATE_UNINITIALIZED){
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }

        while(audioTrack.getPlayState() == AudioTrack.STATE_UNINITIALIZED){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        audioRecord1.startRecording();
        audioTrack.play();
    }





}
