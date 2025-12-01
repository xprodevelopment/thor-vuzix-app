package com.thor.vuzix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.graphics.Color;
import android.util.Log;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.Manifest;
import android.content.pm.PackageManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "ThorVuzix";
    private static final String THOR_WS_URL = "ws://192.168.1.207:8765";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    private TextView statusText;
    private TextView responseText;
    private Button micButton;
    private ThorWebSocketClient wsClient;
    private Handler mainHandler;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;

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

        // Initialize speech recognizer
        initSpeechRecognizer();

        // Microphone button click handler
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMicrophone();
            }
        });

        // Connect to Thor WebSocket
        connectToThor();
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    responseText.setText("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    responseText.setText("Hearing you...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    responseText.setText("Processing...");
                    stopListening();
                }

                @Override
                public void onError(int error) {
                    String errorMsg = getErrorMessage(error);
                    Log.e(TAG, "Speech error: " + errorMsg);
                    responseText.setText("Error: " + errorMsg + "\nTap mic to retry");
                    stopListening();
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        Log.i(TAG, "Recognized: " + spokenText);
                        responseText.setText("You said:\n" + spokenText);

                        // Send to Thor AI
                        if (wsClient != null) {
                            wsClient.sendMessage("{\"text\":\"" + spokenText.replace("\"", "\\\"") + "\"}");
                        }
                    }
                    stopListening();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> partial = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partial != null && !partial.isEmpty()) {
                        responseText.setText("..." + partial.get(0));
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Log.e(TAG, "Speech recognition not available");
            responseText.setText("Speech not available\non this device");
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: return "Audio error";
            case SpeechRecognizer.ERROR_CLIENT: return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Need mic permission";
            case SpeechRecognizer.ERROR_NETWORK: return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "Recognizer busy";
            case SpeechRecognizer.ERROR_SERVER: return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No speech heard";
            default: return "Unknown error";
        }
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

    private void toggleMicrophone() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (speechRecognizer == null) {
            responseText.setText("Speech not available");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            responseText.setText("Mic permission needed");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }

        isListening = true;
        micButton.setBackgroundColor(Color.parseColor("#FF4444"));
        micButton.setText("⏹");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition: " + e.getMessage());
            responseText.setText("Error starting mic");
            stopListening();
        }
    }

    private void stopListening() {
        isListening = false;
        micButton.setBackgroundColor(Color.parseColor("#0099FF"));
        micButton.setText("🎤");

        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping speech recognition: " + e.getMessage());
            }
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
