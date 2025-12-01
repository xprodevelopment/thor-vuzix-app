package com.thor.vuzix;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import android.util.Log;
import android.util.Base64;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.net.URI;

public class MainActivity extends Activity {
    private static final String TAG = "ThorVuzix";
    private static final String THOR_WS_URL = "ws://192.168.1.207:8765";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    // Audio recording settings
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private TextView statusText;
    private TextView responseText;
    private Button micButton;
    private ThorWebSocketClient wsClient;
    private Handler mainHandler;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize UI elements
        statusText = findViewById(R.id.statusText);
        responseText = findViewById(R.id.responseText);
        micButton = findViewById(R.id.micButton);

        // Check for audio permission
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
        }

        // Microphone button click handler
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });

        // Connect to Thor WebSocket
        connectToThor();
    }

    private void connectToThor() {
        try {
            URI serverUri = new URI(THOR_WS_URL);
            wsClient = new ThorWebSocketClient(serverUri, new ThorWebSocketClient.MessageListener() {
                @Override
                public void onConnected() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("CONNECTED");
                            statusText.setTextColor(Color.parseColor("#44FF44"));
                            responseText.setText("Tap mic to speak\n(records 3 seconds)");
                        }
                    });
                }

                @Override
                public void onDisconnected() {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("DISCONNECTED");
                            statusText.setTextColor(Color.parseColor("#FF4444"));
                        }
                    });
                }

                @Override
                public void onMessage(final String message) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            responseText.setText(message);
                        }
                    });
                }

                @Override
                public void onError(final String error) {
                    Log.e(TAG, "WebSocket error: " + error);
                }
            });

            wsClient.connect();

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect: " + e.getMessage());
            statusText.setText("ERROR");
            statusText.setTextColor(Color.parseColor("#FF4444"));
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            responseText.setText("Mic permission needed");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2; // 1 second of audio
        }

        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                responseText.setText("Audio init failed");
                return;
            }

            isRecording = true;
            micButton.setBackgroundColor(Color.parseColor("#FF4444"));
            micButton.setText("⏹");
            responseText.setText("Recording...\n(3 seconds)");

            audioRecord.startRecording();

            // Record for 3 seconds then send
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    recordAndSend();
                }
            });
            recordingThread.start();

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            responseText.setText("Mic permission denied");
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            responseText.setText("Recording error");
        }
    }

    private void recordAndSend() {
        // Record 3 seconds of audio
        int totalSamples = SAMPLE_RATE * 3; // 3 seconds
        short[] audioData = new short[totalSamples];
        int samplesRead = 0;

        while (samplesRead < totalSamples && isRecording) {
            int result = audioRecord.read(audioData, samplesRead, Math.min(1024, totalSamples - samplesRead));
            if (result > 0) {
                samplesRead += result;
            } else {
                break;
            }
        }

        // Stop recording
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopRecording();
                responseText.setText("Sending to Thor...");
            }
        });

        // Convert to bytes and base64 encode
        byte[] audioBytes = new byte[samplesRead * 2];
        for (int i = 0; i < samplesRead; i++) {
            audioBytes[i * 2] = (byte) (audioData[i] & 0xff);
            audioBytes[i * 2 + 1] = (byte) ((audioData[i] >> 8) & 0xff);
        }

        String audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP);

        // Send to Thor
        if (wsClient != null) {
            String message = "{\"type\":\"audio\",\"format\":\"pcm16\",\"sample_rate\":16000,\"data\":\"" + audioBase64 + "\"}";
            wsClient.sendMessage(message);
            Log.i(TAG, "Sent " + audioBytes.length + " bytes of audio");
        }
    }

    private void stopRecording() {
        isRecording = false;
        micButton.setBackgroundColor(Color.parseColor("#0099FF"));
        micButton.setText("🎤");

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording: " + e.getMessage());
            }
            audioRecord = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                responseText.setText("Mic ready!\nTap to speak");
            } else {
                responseText.setText("Mic permission denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
