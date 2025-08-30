package com.example.chapter05;

import android.os.Bundle;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BluetoothMatchActivity extends AppCompatActivity {

    // UI组件
    private TextView tvMyIp;
    private EditText etTargetIp;
    private Button btnConnect;
    private Button btnHostGame;
    private EditText etMessage;
    private Button btnSendMessage;
    private TextView tvChat;
    private Button btnStartGame;

    // 游戏信息显示
    private TextView tvGameSeed;
    private TextView tvMyProgress;
    private TextView tvOpponentProgress;
    private TextView tvMyStars;
    private TextView tvOpponentStars;

    // 网络连接
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isHost = false;
    private boolean isConnected = false;

    // 游戏状态
    private boolean isReady = false;
    private boolean opponentReady = false;
    private long gameSeed = 0;
    private int myProgress = 0;
    private int opponentProgress = 0;
    private int myStars = 3;
    private int opponentStars = 3;

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
        btnStartGame = findViewById(R.id.btn_start_game);

        // 游戏信息视图
        tvGameSeed = findViewById(R.id.tv_game_seed);
        tvMyProgress = findViewById(R.id.tv_my_progress);
        tvOpponentProgress = findViewById(R.id.tv_opponent_progress);
        tvMyStars = findViewById(R.id.tv_my_stars);
        tvOpponentStars = findViewById(R.id.tv_opponent_stars);
    }

    private void setupViews() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        tvMyIp.setText("本机IP: " + ipAddress);

        enableChat(false);
        btnStartGame.setEnabled(false);
        updateGameInfo();
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
                    String chatMsg = "CHAT|" + message;
                    out.println(chatMsg);
                    out.flush();

                    runOnUiThread(() -> {
                        tvChat.append("我: " + message + "\n");
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

        btnStartGame.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, "请先建立连接", Toast.LENGTH_SHORT).show();
                return;
            }

            isReady = true;
            btnStartGame.setEnabled(false);
            btnStartGame.setText("等待对方准备...");

            new Thread(() -> {
                try {
                    out.println("READY|1");
                    out.flush();

                    // 如果是房主，生成随机种子
                    if (isHost) {
                        gameSeed = System.currentTimeMillis();
                        runOnUiThread(this::updateGameInfo);
                        out.println("SEED|" + gameSeed);
                        out.flush();
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "发送准备消息失败", Toast.LENGTH_SHORT).show();
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
                socket.setSoTimeout(0);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "连接成功!", Toast.LENGTH_SHORT).show();
                    btnConnect.setEnabled(false);
                    btnHostGame.setEnabled(false);
                    enableChat(true);
                    btnStartGame.setEnabled(true);
                });

                receiveMessages();
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
                socket.setSoTimeout(0);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "玩家已连接!", Toast.LENGTH_SHORT).show();
                    enableChat(true);
                    btnStartGame.setEnabled(true);
                });

                receiveMessages();
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
                        Toast.makeText(this, "对方已断开连接", Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                    break;
                }

                String[] parts = received.split("\\|", 2);
                if (parts.length < 2) continue;

                String type = parts[0];
                String data = parts[1];

                switch (type) {
                    case "CHAT":
                        runOnUiThread(() -> tvChat.append("对方: " + data + "\n"));
                        break;

                    case "READY":
                        runOnUiThread(() -> {
                            opponentReady = true;
                            btnStartGame.setText("对方已准备");
                            if (isReady) {
                                startGame();
                            }
                        });
                        break;

                    case "SEED":
                        gameSeed = Long.parseLong(data);
                        runOnUiThread(this::updateGameInfo);
                        break;
                }
            }
        } catch (IOException e) {
            runOnUiThread(() -> {
                Toast.makeText(this, "连接错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                resetConnection();
            });
        }
    }

    private void startGame() {
        runOnUiThread(() -> {
            Toast.makeText(this, "游戏开始! 种子: " + gameSeed, Toast.LENGTH_SHORT).show();
            // 这里可以添加游戏初始化逻辑
        });
    }

    private void updateGameInfo() {
        runOnUiThread(() -> {
            tvGameSeed.setText("随机种子: " + (gameSeed == 0 ? "未生成" : gameSeed));
            tvMyProgress.setText("我的进度: " + myProgress + "/81");
            tvOpponentProgress.setText("对方进度: " + opponentProgress + "/81");
            tvMyStars.setText("我的星星: ☆" + myStars);
            tvOpponentStars.setText("对方星星: ☆" + opponentStars);
        });
    }

    private void enableChat(boolean enable) {
        runOnUiThread(() -> {
            etMessage.setEnabled(enable);
            btnSendMessage.setEnabled(enable);
        });
    }

    private void resetConnection() {
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

        isConnected = false;
        isHost = false;
        isReady = false;
        opponentReady = false;

        runOnUiThread(() -> {
            btnConnect.setEnabled(true);
            btnHostGame.setEnabled(true);
            btnStartGame.setEnabled(false);
            btnStartGame.setText("开始游戏");
            enableChat(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetConnection();
    }
}