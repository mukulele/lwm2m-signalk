package io.mukulele.lwm2msk;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import okhttp3.*;

public class SignalKClient {

    private final OkHttpClient http;
    private final String wsUrl;
    private final String token;
    private final Consumer<String> onMessage;
    private WebSocket socket;

    public SignalKClient(String wsUrl, String token, Consumer<String> onMessage) {
        this.wsUrl = wsUrl;
        this.token = token;
        this.onMessage = onMessage;
        this.http = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket keeps connection open
                .build();
    }

    public void connect() {
        Request.Builder rb = new Request.Builder().url(wsUrl);
        if (token != null && !token.isEmpty()) {
            rb.addHeader("Authorization", "Bearer " + token);
        }
        Request req = rb.build();

        this.socket = http.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("[SignalK] Connected");
                // If needed, send a subscription message here. Many Signal K servers stream deltas by default.
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (onMessage != null) onMessage.accept(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("[SignalK] Closing: " + code + " " + reason);
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("[SignalK] Closed: " + code + " " + reason);
                // Simple auto-reconnect with delay
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("[SignalK] Failure: " + t.getMessage());
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
            System.out.println("[SignalK] Reconnecting...");
            connect();
        }).start();
    }

    public void close() {
        if (socket != null) {
            socket.close(1000, "shutdown");
        }
        http.dispatcher().executorService().shutdown();
        http.connectionPool().evictAll();
    }
}