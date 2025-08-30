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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.widget.TextView;
import network.NetworkProtocol;

public class MultiplayerSudokuActivity extends AppCompatActivity {
    // 网络相关
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isHost = false;

    // 游戏状态
    private int[][] sudokuData;
    private int[][] solution;
    private int opponentStars = 3;
    private int opponentProgress = 0;

    // UI组件
    private TextView tvOpponentStars;
    private TextView tvOpponentProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_sudoku);

        // 先检查连接是否仍然有效
        if (getIntent().getBooleanExtra("connection_lost", false)) {
            Toast.makeText(this, "连接已断开", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupNetwork();
        initGame();
    }

    private void initViews() {
        tvOpponentStars = findViewById(R.id.tv_opponent_stars);
        tvOpponentProgress = findViewById(R.id.tv_opponent_progress);
    }

    private void setupNetwork() {
        Intent intent = getIntent();
        isHost = "host".equals(intent.getStringExtra("mode"));
        String localIp = intent.getStringExtra("local_ip");
        String targetIp = intent.getStringExtra("target_ip");

        if (isHost) {
            setupAsHost(localIp);
        } else {
            setupAsClient(targetIp);
        }
    }

    private void setupAsHost(String localIp) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8888)) {
                runOnUiThread(() ->
                        Toast.makeText(this, "等待玩家连接...", Toast.LENGTH_SHORT).show());

                socket = serverSocket.accept();
                socket.setSoTimeout(15000); // 15秒超时
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() ->
                        Toast.makeText(this, "玩家已连接!", Toast.LENGTH_SHORT).show());

                startListening();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "服务器错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void setupAsClient(String targetIp) {
        new Thread(() -> {
            try {
                socket = new Socket();
                // 设置连接超时15秒
                socket.connect(new InetSocketAddress(targetIp, 8888), 15000);
                socket.setSoTimeout(15000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() ->
                        Toast.makeText(this, "连接成功!", Toast.LENGTH_SHORT).show());

                startListening();

            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "连接失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    handleNetworkMessage(message);
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "连接断开", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void handleNetworkMessage(String message) {
        String[] parts = NetworkProtocol.parseMessage(message);
        if (parts.length < 2) return;

        String type = parts[0];
        String data = parts[1];

        switch (type) {
            case NetworkProtocol.TYPE_GAME_DATA:
                // 房主发送的游戏数据
                if (!isHost) {
                    receiveGameData(data);
                }
                break;

            case NetworkProtocol.TYPE_MOVE:
                // 对方走棋
                updateOpponentMove(data);
                break;

            case NetworkProtocol.TYPE_STARS:
                // 对方星星数更新
                opponentStars = Integer.parseInt(data);
                updateOpponentInfo();
                break;

            case NetworkProtocol.TYPE_PROGRESS:
                // 对方进度更新
                opponentProgress = Integer.parseInt(data);
                updateOpponentInfo();
                break;
        }
    }

    private void initGame() {
        if (isHost) {
            // 房主生成谜题
            generatePuzzle();
            // 发送给客户端
            sendGameData();
        }
        // 客户端等待接收数据
    }

    private void generatePuzzle() {
        // 简化的谜题生成逻辑
        sudokuData = new int[9][9];
        solution = new int[9][9];
        // TODO: 实现实际的谜题生成算法
    }

    private void sendGameData() {
        // 将谜题数据序列化为字符串
        String gameData = serializeGameData();
        String message = NetworkProtocol.createMessage(
                NetworkProtocol.TYPE_GAME_DATA,
                gameData
        );
        out.println(message);
    }

    private String serializeGameData() {
        // 简化的序列化实现
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                sb.append(sudokuData[i][j]);
                if (j < 8) sb.append(",");
            }
            if (i < 8) sb.append(";");
        }
        return sb.toString();
    }

    private void receiveGameData(String data) {
        // 反序列化游戏数据
        String[] rows = data.split(";");
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            for (int j = 0; j < cells.length; j++) {
                sudokuData[i][j] = Integer.parseInt(cells[j]);
            }
        }

        // TODO: 初始化游戏UI
    }

    private void updateOpponentMove(String moveData) {
        // 解析对方走棋 (格式: row,col,value)
        String[] parts = moveData.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);
        int value = Integer.parseInt(parts[2]);

        // TODO: 更新UI显示对方的走棋
    }

    private void updateOpponentInfo() {
        runOnUiThread(() -> {
            tvOpponentStars.setText("对方星星: " + opponentStars);
            tvOpponentProgress.setText("对方进度: " + opponentProgress + "/81");
        });
    }

    // 当玩家走棋时调用
    private void onPlayerMove(int row, int col, int value) {
        // 更新自己的游戏状态
        sudokuData[row][col] = value;

        // 发送给对方
        String moveData = row + "," + col + "," + value;
        String message = NetworkProtocol.createMessage(
                NetworkProtocol.TYPE_MOVE,
                moveData
        );
        out.println(message);
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