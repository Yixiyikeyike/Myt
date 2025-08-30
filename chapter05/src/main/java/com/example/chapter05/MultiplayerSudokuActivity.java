package com.example.chapter05;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiplayerSudokuActivity extends AppCompatActivity {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku); // 复用单机界面

        // 获取连接信息
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        String ip = intent.getStringExtra("target_ip");

        // 初始化游戏和网络连接
        initGame();
        if ("host".equals(mode)) {
            initAsHost();
        } else {
            initAsClient(ip);
        }
    }

    private void initGame() {
        // 初始化数独游戏逻辑
    }

    private void initAsHost() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                socket = serverSocket.accept();
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 处理游戏数据同步
                handleNetworkCommunication();
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "连接错误", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void initAsClient(String ip) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, 8888);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 处理游戏数据同步
                handleNetworkCommunication();
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "连接错误", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void handleNetworkCommunication() {
        // 游戏数据同步逻辑
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}