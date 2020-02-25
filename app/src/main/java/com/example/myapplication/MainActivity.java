package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 8000;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;

    private Button recordButton;
    private AudioIn audioIn;
    private AudioOut audioOut;
    private WSClient wsClient;

    private Queue<byte[]> voiceQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recordButton = findViewById(R.id.recordButton);
        recordButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    startRecord();
                } else if (action == MotionEvent.ACTION_UP) {
                    stopRecord();
                }
                return false;
            }
        });

        audioIn = new AudioIn();
        audioOut = new AudioOut();
        initRemoteConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initRemoteConnection() {
        wsClient = new WSClient(URI.create("ws://139.199.17.99:9998/ws/im"), new WSClient.MessageListener() {
            @Override
            public void onMessage(ByteBuffer byteBuffer) {
                System.out.println("received bytebuffer");
                byte[] buffer = byteBuffer.array();
                if (buffer != null) {
                    System.out.println("received byte length is " + buffer.length);
                    audioOut.write(buffer);
                }
            }
        });

        try {
            wsClient.connectBlocking();
        } catch (InterruptedException e) {
            //报错异常
            e.printStackTrace();
        }
    }

    protected void startRecord() {
        System.out.println("starting record");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
        }

        audioIn.startRecording();
    }

    protected void stopRecord() {
        if (audioIn != null) {
            audioIn.stopRecording();
        }

//        byte[] buffer;
//        while ((buffer = voiceQueue.poll()) != null) {
//            audioOut.write(buffer);
//        }
    }

    protected void processPCM(byte[] buffer) {
        wsClient.send(buffer);
//        voiceQueue.add(buffer);
        System.out.println("put buffer successfully, bufferSize is: " + buffer.length + " voiceQueue length is " + voiceQueue.size());
    }

    class AudioIn extends Thread {
        private boolean start = false;

        public AudioIn() {
            start();
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            AudioRecord recorder = null;
            try {
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 5;

                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize);

                recorder.startRecording();

                while (true) {
                    byte[] buffer = new byte[bufferSize];

                    bufferSize = recorder.read(buffer, 0, buffer.length);
                    //process is what you will do with the data...not defined here

                    if (start) {
                        processPCM(buffer);
                    }
                }
            } catch (Throwable x) {
                x.printStackTrace();
            }
        }

        void stopRecording() {
            start = false;
        }

        void startRecording() {
            start = true;
        }
    }

    private class AudioOut extends Thread {
        AudioTrack audioTrack;

        private BlockingQueue<byte[]> blockingQueue = new LinkedBlockingQueue<>();

        public AudioOut() {
            start();
        }

        @Override
        public void run() {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AUDIO_FORMAT) * 5;
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,
                    AUDIO_FORMAT, bufferSize, AudioTrack.MODE_STREAM);

            audioTrack.play();

            while (true) {
                byte[] buffer = null;
                try {
                    buffer = blockingQueue.poll(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (buffer != null) {
                    audioTrack.write(buffer, 0, buffer.length);
                }
            }
        }

        public void write(byte[] buffer) {
            blockingQueue.add(buffer);
        }
    }
}
