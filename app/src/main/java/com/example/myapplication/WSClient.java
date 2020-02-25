package com.example.myapplication;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class WSClient extends WebSocketClient {
    public interface MessageListener {
        void onMessage(ByteBuffer byteBuffer);
    }

    private MessageListener messageListener;

    public WSClient(URI serverUri, MessageListener messageListener) {
        super(serverUri, new Draft_6455());
        this.messageListener = messageListener;
        System.out.println("创建连接！！！！");
    }

    @Override
    public void onOpen(ServerHandshake handshak) {
        Log.e("JWebSocketClient", "onOpen()");
    }

    @Override
    public void onMessage(String message) {
        Log.e("JWebSocketClient", "onMessage()");
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onMessage(ByteBuffer bytes) {
        if (messageListener != null) {
            messageListener.onMessage(bytes);
        }//To overwrite
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("JWebSocketClient", "onClose()");
        this.connect();
    }

    @Override
    public void onError(Exception ex) {
        Log.e("JWebSocketClient", "onError()");
    }
}
