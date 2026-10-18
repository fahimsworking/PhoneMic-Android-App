package com.fahim.phonemic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Phone Mic: one button. Tap = microphone ON (your voice plays on the
 * connected Bluetooth speaker). Tap again = OFF.
 */
public class MainActivity extends Activity {

    private static final int REQ_MIC = 1;

    private ImageButton micButton;
    private TextView statusText;

    private volatile boolean running = false;
    private Thread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        micButton = findViewById(R.id.micButton);
        statusText = findViewById(R.id.statusText);
        micButton.setOnClickListener(v -> toggle());
        updateUi();
    }

    private void toggle() {
        if (running) {
            stopMic();
        } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startMic();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MIC) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                startMic();
            } else {
                Toast.makeText(this, "Microphone permission is needed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startMic() {
        if (running) return;
        if (!bluetoothSpeakerConnected()) {
            Toast.makeText(this,
                    "No Bluetooth speaker found – sound will come from the phone itself",
                    Toast.LENGTH_LONG).show();
        }
        running = true;
        audioThread = new Thread(this::audioLoop, "mic-loop");
        audioThread.start();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateUi();
    }

    private void stopMic() {
        running = false;
        Thread t = audioThread;
        audioThread = null;
        if (t != null && t != Thread.currentThread()) {
            try {
                t.join(1000);
            } catch (InterruptedException ignored) {
            }
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        updateUi();
    }

    /** Reads the microphone and plays it straight out to the speaker. */
    private void audioLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        int rate = 48000;
        int minIn = AudioRecord.getMinBufferSize(rate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minIn <= 0) {
            rate = 44100;
            minIn = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }
        int minOut = AudioTrack.getMinBufferSize(rate,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord recorder = null;
        AudioTrack player = null;
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minIn * 2);

            player = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(minOut * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build();

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Microphone is busy or unavailable");
            }

            // Small chunks (~10 ms) keep the delay as low as possible.
            short[] buffer = new short[Math.max(64, Math.min(minIn / 2, rate / 100))];

            recorder.startRecording();
            player.play();

            while (running) {
                int n = recorder.read(buffer, 0, buffer.length);
                if (n > 0) {
                    player.write(buffer, 0, n);
                } else if (n < 0) {
                    throw new IllegalStateException("Microphone read error " + n);
                }
            }
        } catch (Exception e) {
            final String msg = e.getMessage();
            runOnUiThread(() -> {
                Toast.makeText(this, "Mic stopped: " + msg, Toast.LENGTH_LONG).show();
                if (running) stopMic();
            });
        } finally {
            if (recorder != null) {
                try { recorder.stop(); } catch (Exception ignored) { }
                recorder.release();
            }
            if (player != null) {
                try { player.stop(); } catch (Exception ignored) { }
                player.release();
            }
        }
    }

    private boolean bluetoothSpeakerConnected() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = d.getType();
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || type == 26 /* TYPE_BLE_HEADSET */
                    || type == 27 /* TYPE_BLE_SPEAKER */) {
                return true;
            }
        }
        return false;
    }

    private void updateUi() {
        micButton.setBackgroundResource(running ? R.drawable.circle_on : R.drawable.circle_off);
        statusText.setText(running ? "MIC ON\nTap to turn off" : "MIC OFF\nTap to speak");
    }

    @Override
    protected void onDestroy() {
        stopMic();
        super.onDestroy();
    }
}
