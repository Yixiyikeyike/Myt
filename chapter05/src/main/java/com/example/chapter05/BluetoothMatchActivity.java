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
import java.net.SocketTimeoutException;

public class BluetoothMatchActivity extends AppCompatActivity {

    private TextView tvMyIp;
    private EditText etTargetIp;
    private Button btnConnect;
    private Button btnHostGame;
    private EditText etMessage;
    private Button btnSendMessage;
    private TextView tvChat;
    private Button btnStartGame;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isHost = false;
    private boolean isConnected = false;
    private boolean isReady = false;
    private boolean opponentReady = false;

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
    }

    private void setupViews() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        tvMyIp.setText("本机IP: " + ipAddress);

        enableChat(false);
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
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) return;

            new Thread(() -> {
                try {
                    out.println("CHAT|" + message);
                    out.flush();
                    runOnUiThread(() -> {
                        tvChat.append("我: " + message + "\n");
                        etMessage.setText("");
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        btnStartGame.setOnClickListener(v -> {
            isReady = true;
            btnStartGame.setEnabled(false);
            btnStartGame.setText("等待对方准备...");

            new Thread(() -> {
                try {
                    out.println("READY|1");
                    out.flush();
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "发送准备消息失败", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }

    private void connectToTarget(String ip) {
        new Thread(() -> {
            try {
                socket = new Socket(ip, 8888);
                // 移除超时设置，改为0表示无限等待
                socket.setSoTimeout(0);  // 关键修改点1
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

                // 启动心跳检测
                startHeartbeat();  // 关键修改点2
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
                // 移除超时设置
                socket.setSoTimeout(0);  // 关键修改点3
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                isConnected = true;
                runOnUiThread(() -> {
                    Toast.makeText(this, "玩家已连接!", Toast.LENGTH_SHORT).show();
                    enableChat(true);
                    btnStartGame.setEnabled(true);
                });

                // 启动心跳检测
                startHeartbeat();  // 关键修改点4
                receiveMessages();
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "服务器错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetConnection();
                });
            }
        }).start();
    }
    // 添加心跳检测机制
    private void startHeartbeat() {
        new Thread(() -> {
            while (isConnected && socket != null && !socket.isClosed()) {
                try {
                    Thread.sleep(3000); // 每3秒发送一次心跳
                    out.println("HEARTBEAT|PING");
                    out.flush();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "连接已断开", Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                    break;
                }
            }
        }).start();
    }
    private void receiveMessages() {
        try {
            while (isConnected && socket != null && !socket.isClosed()) {
                String received = in.readLine();
                if (received == null) {
                    // 对方关闭了连接
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
                                new Handler().postDelayed(this::enterGame, 500);
                            }
                        });
                        break;

                    case "HEARTBEAT":
                        // 收到心跳包，不做任何处理，只是保持连接活跃
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

    private void enterGame() {
        if (!isConnected) {
            Toast.makeText(this, "连接已断开，无法开始游戏", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MultiplayerSudokuActivity.class);
        intent.putExtra("isHost", isHost);
        startActivity(intent);
        finish();
    }

    private void enableChat(boolean enable) {
        etMessage.setEnabled(enable);
        btnSendMessage.setEnabled(enable);
    }

    private void resetConnection() {
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
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