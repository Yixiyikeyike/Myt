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
import java.net.SocketTimeoutException;

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

    private Button btnStartGame;
    private boolean isReady = false;
    private boolean opponentReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_match);
        btnStartGame = findViewById(R.id.btn_start_game);
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
        // 初始禁用开始游戏按钮
        btnStartGame.setEnabled(false);
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
        // 添加开始游戏按钮监听
//        btnStartGame.setOnClickListener(v -> {
//            if (!isConnected) {
//                Toast.makeText(this, "请先建立连接", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            goToMultiplayerGame();
//        });
        btnStartGame.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, "请先建立连接", Toast.LENGTH_SHORT).show();
                return;
            }

            // 发送准备就绪消息
            sendReadyMessage();
        });
    }

    private void connectToTarget(String ip) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, 8888);
                socket.setSoTimeout(0); // 设置为0表示无限等待
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;
                onConnectedSuccess(); // 调用成功处理方法
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
                socket.setSoTimeout(0); // 设置为0表示无限等待
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;
                onConnectedSuccess(); // 调用成功处理方法
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
                    runOnUiThread(() -> {
                        Toast.makeText(this, "连接已关闭", Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                    break;
                }

                String[] parsed = NetworkProtocol.parseMessage(received);
                if (parsed.length < 2) continue;

                String type = parsed[0];
                String data = parsed[1];

                switch (type) {
                    case NetworkProtocol.TYPE_CHAT:
                        runOnUiThread(() -> appendToChat("对方: " + data));
                        break;

                    case NetworkProtocol.TYPE_READY:
                        opponentReady = true;
                        runOnUiThread(() -> {
                            if (isReady) {
                                // 双方都准备好了，发送开始游戏消息
                                sendStartGameMessage();
                            } else {
                                btnStartGame.setText("对方已准备");
                            }
                        });
                        break;

                    case NetworkProtocol.TYPE_START_GAME:
                        runOnUiThread(this::goToMultiplayerGame);
                        break;
                }
            }
        } catch (SocketTimeoutException e) {
            // 超时处理
            runOnUiThread(() -> {
                Toast.makeText(this, "操作超时", Toast.LENGTH_SHORT).show();
            });
        }
        catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "连接错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                resetConnection();
            });
        }
    }

    private void sendStartGameMessage() {
        new Thread(() -> {
            try {
                String startMsg = NetworkProtocol.createMessage(
                        NetworkProtocol.TYPE_START_GAME,
                        "1" // 开始游戏
                );
                out.println(startMsg);
                out.flush();

                // 自己也进入游戏
                runOnUiThread(this::goToMultiplayerGame);

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "发送开始消息失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
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
        isHost = false;

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
            btnStartGame.setEnabled(false);
            enableChat(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetConnection();
    }
    private void onConnectedSuccess() {
        runOnUiThread(() -> {
            Toast.makeText(this, "连接成功!", Toast.LENGTH_SHORT).show();
            btnConnect.setEnabled(false);
            btnHostGame.setEnabled(false);
            enableChat(true);
            // 启用开始游戏按钮
            btnStartGame.setEnabled(true);
        });
    }
    private void goToMultiplayerGame() {
        try {
            // 确保socket不被关闭
            socket.setKeepAlive(true);

            Intent intent = new Intent(this, MultiplayerSudokuActivity.class);
            intent.putExtra("mode", isHost ? "host" : "client");
            intent.putExtra("local_ip", tvMyIp.getText().toString().replace("本机IP: ", ""));
            if (!isHost) {
                intent.putExtra("target_ip", etTargetIp.getText().toString());
            }
            // 传递socket信息
            intent.putExtra("socket_port", 8888);
            startActivity(intent);
            // 不要立即finish，保持连接
        } catch (Exception e) {
            Toast.makeText(this, "启动游戏失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void sendReadyMessage() {
        new Thread(() -> {
            try {
                String readyMsg = NetworkProtocol.createMessage(
                        NetworkProtocol.TYPE_READY,
                        "1" // 1表示准备就绪
                );
                out.println(readyMsg);
                out.flush();

                isReady = true;
                runOnUiThread(() -> {
                    btnStartGame.setEnabled(false);
                    btnStartGame.setText("等待对方准备...");
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "发送准备消息失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
    private void startHeartbeat() {
        new Thread(() -> {
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    Thread.sleep(3000); // 每3秒发送一次心跳
                    String heartbeatMsg = NetworkProtocol.createMessage(
                            NetworkProtocol.TYPE_CHAT,
                            "HEARTBEAT"
                    );
                    out.println(heartbeatMsg);
                    out.flush();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "连接异常", Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                    break;
                }
            }
        }).start();
    }
}