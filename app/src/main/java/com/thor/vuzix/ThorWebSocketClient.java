package com.thor.vuzix;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;

public class ThorWebSocketClient extends WebSocketClient {
    private static final String TAG = "ThorWebSocket";
    private MessageListener listener;

    public interface MessageListener {
        void onConnected();
        void onDisconnected();
        void onMessage(String message);
        void onError(String error);
    }

    public ThorWebSocketClient(URI serverUri, MessageListener listener) {
        super(serverUri);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "WebSocket connected");
        if (listener != null) {
            listener.onConnected();
        }
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Message received: " + message);
        if (listener != null) {
            try {
                // Parse JSON message from Thor
                JSONObject json = new JSONObject(message);
                String displayText = "";

                if (json.has("body")) {
                    displayText = json.getString("body");
                } else if (json.has("title")) {
                    displayText = json.getString("title");
                } else {
                    displayText = message;
                }

                listener.onMessage(displayText);
            } catch (Exception e) {
                // If not JSON, display as-is
                listener.onMessage(message);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(TAG, "WebSocket closed: " + reason);
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG, "WebSocket error", ex);
        if (listener != null) {
            listener.onError(ex.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (isOpen()) {
            send(message);
            Log.d(TAG, "Message sent: " + message);
        } else {
            Log.w(TAG, "Cannot send message, WebSocket not connected");
        }
    }
}
