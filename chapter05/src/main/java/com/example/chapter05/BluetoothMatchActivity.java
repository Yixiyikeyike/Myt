package com.example.chapter05;

import android.os.Bundle;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import network.NetworkProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BluetoothMatchActivity extends AppCompatActivity {

    private TextView tvMyIp;
    private EditText etTargetIp;
    private Button btnConnect;
    private Button btnHostGame;
    private EditText etMessage;
    private Button btnSendMessage;
    private TextView tvChat;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isHost = false;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_match);

        initViews();
        setupViews();
        setupListeners();
    }

    private void initViews() {
        tvMyIp = findViewById(R.id.tv_my_ip);
        etTargetIp = findViewById(R.id.et_target_ip);
        btnConnect = findViewById(R.id.btn_connect);
        btnHostGame = findViewById(R.id.btn_host_game);
        etMessage = findViewById(R.id.et_message);
        btnSendMessage = findViewById(R.id.btn_send_message);
        tvChat = findViewById(R.id.tv_chat);
    }

    private void setupViews() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        tvMyIp.setText("本机IP: " + ipAddress);

        // 初始禁用聊天功能
        enableChat(false);
    }

    private void setupListeners() {
        btnConnect.setOnClickListener(v -> {
            String targetIp = etTargetIp.getText().toString().trim();
            if (targetIp.isEmpty()) {
                Toast.makeText(this, "请输入目标IP地址", Toast.LENGTH_SHORT).show();
                return;
            }
            connectToTarget(targetIp);
        });

        btnHostGame.setOnClickListener(v -> startHosting());

        btnSendMessage.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, "未建立连接", Toast.LENGTH_SHORT).show();
                return;
            }

            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    // 使用协议格式化消息
                    String protocolMsg = NetworkProtocol.createMessage(
                            NetworkProtocol.TYPE_CHAT,
                            message
                    );
                    out.println(protocolMsg);
                    out.flush();

                    runOnUiThread(() -> {
                        appendToChat("我: " + message);
                        etMessage.setText("");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                }
            }).start();
        });
    }

    private void connectToTarget(String ip) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, 8888);
                socket.setSoTimeout(5000); // 设置超时5秒
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;

                runOnUiThread(() -> {
                    Toast.makeText(this, "连接成功!", Toast.LENGTH_SHORT).show();
                    btnConnect.setEnabled(false);
                    btnHostGame.setEnabled(false);
                    enableChat(true);
                });

                // 消息接收线程
                new Thread(this::receiveMessages).start();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetConnection();
                });
            }
        }).start();
    }

    private void startHosting() {
        isHost = true;
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "等待连接中...", Toast.LENGTH_SHORT).show();
                    btnConnect.setEnabled(false);
                    btnHostGame.setEnabled(false);
                });

                socket = serverSocket.accept();
                socket.setSoTimeout(5000); // 设置超时5秒
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;

                runOnUiThread(() -> {
                    Toast.makeText(this, "玩家已连接!", Toast.LENGTH_SHORT).show();
                    enableChat(true);
                });

                // 消息接收线程
                new Thread(this::receiveMessages).start();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "服务器错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetConnection();
                });
            }
        }).start();
    }

    private void receiveMessages() {
        try {
            while (isConnected && socket != null && !socket.isClosed()) {
                String received = in.readLine();
                if (received == null) {
                    // 连接断开
                    runOnUiThread(() -> {
                        Toast.makeText(this, "连接已断开", Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                    break;
                }

                // 解析协议消息
                String[] parsed = NetworkProtocol.parseMessage(received);
                if (parsed.length >= 2 && NetworkProtocol.TYPE_CHAT.equals(parsed[0])) {
                    runOnUiThread(() -> appendToChat("对方: " + parsed[1]));
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "接收消息错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                resetConnection();
            });
        }
    }

    private void appendToChat(String message) {
        tvChat.append(message + "\n");
    }

    private void enableChat(boolean enable) {
        etMessage.setEnabled(enable);
        btnSendMessage.setEnabled(enable);
    }

    private void resetConnection() {
        isConnected = false;

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        runOnUiThread(() -> {
            btnConnect.setEnabled(true);
            btnHostGame.setEnabled(true);
            enableChat(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetConnection();
    }
}