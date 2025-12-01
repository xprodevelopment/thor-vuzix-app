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

import java.net.URI;

public class MainActivity extends Activity {
    private static final String TAG = "ThorVuzix";
    private static final String THOR_WS_URL = "ws://192.168.1.207:8765";

    private TextView statusText;
    private TextView responseText;
    private Button micButton;
    private ThorWebSocketClient wsClient;
    private Handler mainHandler;
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
        isListening = !isListening;

        if (isListening) {
            // Start listening
            micButton.setBackgroundColor(Color.parseColor("#FF4444"));
            responseText.setText("Listening...");
            // TODO: Implement speech recognition
            // For now, send a test message
            if (wsClient != null) {
                wsClient.sendMessage("{\"type\":\"voice_command\",\"text\":\"Test voice command\"}");
            }
        } else {
            // Stop listening
            micButton.setBackgroundColor(Color.parseColor("#0099FF"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
