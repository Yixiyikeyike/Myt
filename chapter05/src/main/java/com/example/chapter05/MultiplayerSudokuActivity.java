package com.example.chapter05;

import static generator.SudokuGenerator.convertToPredefinedFormat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.HashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.widget.TextView;

import generator.SudokuGenerator;
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
    // 游戏UI相关
    private GridLayout sudokuGrid;
    private Button[] numberButtons;
    private TextView timerText;
    private TextView errorText;
    private TextView starText;

    // 游戏状态相关
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int errorCount = 0;
    private final int MAX_ERRORS = 3;
    private int filledCells = 0;
    private long startTime;
    private final int TOTAL_CELLS = 81;
    private Map<String, TextView> cellViews = new HashMap<>();

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
        // 初始化数独网格
        sudokuGrid = findViewById(R.id.sudoku_grid);

        // 初始化数字按钮
        numberButtons = new Button[9];
        numberButtons[0] = findViewById(R.id.btn_1);
        numberButtons[1] = findViewById(R.id.btn_2);
        numberButtons[2] = findViewById(R.id.btn_3);
        numberButtons[3] = findViewById(R.id.btn_4);
        numberButtons[4] = findViewById(R.id.btn_5);
        numberButtons[5] = findViewById(R.id.btn_6);
        numberButtons[6] = findViewById(R.id.btn_7);
        numberButtons[7] = findViewById(R.id.btn_8);
        numberButtons[8] = findViewById(R.id.btn_9);

        // 初始化信息显示
        timerText = findViewById(R.id.timer_text);
        errorText = findViewById(R.id.error_text);
        starText = findViewById(R.id.star_text);

        // 初始化对手信息显示
        tvOpponentStars = findViewById(R.id.tv_opponent_stars);
        tvOpponentProgress = findViewById(R.id.tv_opponent_progress);

        // 初始化功能按钮
        Button hintButton = findViewById(R.id.btn_hint);
        Button closeHintButton = findViewById(R.id.btn_hint_close);
        Button returnButton = findViewById(R.id.btn_return);

        // 设置按钮点击监听器
        hintButton.setOnClickListener(v -> showFullSolution());
        closeHintButton.setOnClickListener(v -> hideSolutionHint());
        returnButton.setOnClickListener(v -> confirmExit());
    }
    private void confirmExit() {
        new AlertDialog.Builder(this)
                .setTitle("确认退出")
                .setMessage("确定要返回主界面吗？当前游戏进度将丢失")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 关闭网络连接
                    try {
                        if (socket != null) socket.close();
                        if (out != null) out.close();
                        if (in != null) in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // 返回主界面
                    Intent intent = new Intent(this, UserMsgActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showFullSolution() {
        // 实现显示完整解决方案的逻辑
        // 类似于单机模式中的实现
    }

    private void hideSolutionHint() {
        // 实现隐藏解决方案提示的逻辑
        // 类似于单机模式中的实现
    }
    private void initGame() {
        // 初始化UI组件
        timerText = findViewById(R.id.timer_text);
        errorText = findViewById(R.id.error_text);
        starText = findViewById(R.id.star_text);
        sudokuGrid = findViewById(R.id.sudoku_grid);

        // 设置数字按钮
        setupNumberButtons();

        // 如果是房主，生成谜题并发送
        if (isHost) {
            generatePuzzle();
            sendGameData();
        }

        // 开始计时
        startTimer();
        updateErrorDisplay();
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
            case NetworkProtocol.TYPE_GAME_OVER:
                // 对方已结束游戏
                runOnUiThread(() -> {
                    String result = data.equals("win") ? "lose" : "win";
                    Intent intent = new Intent(this, MultiplayerResultActivity.class);
                    intent.putExtra("result", result);
                    startActivity(intent);
                    finish();
                });
                break;
        }
    }


    private void generatePuzzle() {
        // 使用生成器创建谜题
        long seed = System.currentTimeMillis();
        int[][] predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(2, seed);

        // 初始化数独数据
        sudokuData = new int[9][9];
        solution = new int[9][9];

        // 应用预填数字
        for (int[] predefined : predefinedValues) {
            int row = predefined[0];
            int col = predefined[1];
            int value = predefined[2];
            sudokuData[row][col] = value;
        }

        // 生成完整解决方案
        solution = SudokuGenerator.generateSolutionForPuzzle(predefinedValues);

        // 创建UI网格
        createSudokuGrid();
    }
    private void createSudokuGrid() {
        sudokuGrid.removeAllViews();
        cellViews.clear();

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cellView = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                params.columnSpec = GridLayout.spec(col, 1, 1f);
                params.setMargins(1, 1, 1, 1);
                cellView.setLayoutParams(params);

                cellView.setGravity(Gravity.CENTER);
                cellView.setTextSize(20);
                cellView.setBackgroundColor(Color.WHITE);

                int value = sudokuData[row][col];
                if (value != 0) {
                    cellView.setText(String.valueOf(value));
                    cellView.setTextColor(Color.BLACK);
                    cellView.setTag("fixed");
                    filledCells++;
                } else {
                    cellView.setText("");
                    cellView.setTextColor(Color.BLUE);
                }

                final int finalRow = row;
                final int finalCol = col;
                cellView.setOnClickListener(v -> onCellSelected(finalRow, finalCol));

                cellViews.put(row + "_" + col, cellView);
                sudokuGrid.addView(cellView);
            }
        }
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
                if (sudokuData[i][j] != 0) {
                    filledCells++;
                }
            }
        }

        // 生成解决方案
        int[][] predefinedValues = convertToPredefinedFormat(sudokuData);
        solution = SudokuGenerator.generateSolutionForPuzzle(predefinedValues);

        // 初始化UI
        runOnUiThread(this::createSudokuGrid);
    }

    private void updateOpponentMove(String moveData) {
        // 解析对方走棋 (格式: row,col,value)
        String[] parts = moveData.split(",");
        final int row = Integer.parseInt(parts[0]);
        final int col = Integer.parseInt(parts[1]);
        final int value = Integer.parseInt(parts[2]);

        runOnUiThread(() -> {
            TextView cell = cellViews.get(row + "_" + col);
            if (cell != null && !"fixed".equals(cell.getTag())) {
                cell.setText(String.valueOf(value));
                cell.setTextColor(Color.GREEN); // 用绿色显示对方的走棋
                sudokuData[row][col] = value;
                filledCells++;

                // 检查游戏是否结束
                if (filledCells == 81) {
                    checkGameCompletion();
                }
            }
        });
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
    private void setupNumberButtons() {
        numberButtons = new Button[9];
        int[] buttonIds = {
                R.id.btn_1, R.id.btn_2, R.id.btn_3,
                R.id.btn_4, R.id.btn_5, R.id.btn_6,
                R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i < 9; i++) {
            numberButtons[i] = findViewById(buttonIds[i]);
            final int number = i + 1;
            numberButtons[i].setOnClickListener(v -> onNumberSelected(number));
        }
    }
    private void onCellSelected(int row, int col) {
        if (selectedRow != -1 && selectedCol != -1) {
            TextView prevCell = cellViews.get(selectedRow + "_" + selectedCol);
            if (prevCell != null) {
                prevCell.setBackgroundColor(Color.WHITE);
            }
        }

        selectedRow = row;
        selectedCol = col;
        TextView cell = cellViews.get(row + "_" + col);
        if (cell != null && !"fixed".equals(cell.getTag())) {
            cell.setBackgroundColor(Color.parseColor("#E3F2FD"));
        }
    }

    private void onNumberSelected(int number) {
        if (selectedRow == -1 || selectedCol == -1) {
            Toast.makeText(this, "请先选择一个格子", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView cell = cellViews.get(selectedRow + "_" + selectedCol);
        if (cell != null && !"fixed".equals(cell.getTag())) {
            // 发送给对方
            String moveData = selectedRow + "," + selectedCol + "," + number;
            String message = NetworkProtocol.createMessage(
                    NetworkProtocol.TYPE_MOVE,
                    moveData
            );
            out.println(message);

            // 更新本地UI
            cell.setText(String.valueOf(number));
            sudokuData[selectedRow][selectedCol] = number;
            filledCells++;

            // 检查是否完成游戏
            if (filledCells == 81) {
                checkGameCompletion();
            }
        }
    }

    private void checkGameCompletion() {
        if (isSudokuComplete()) {
            gameWin();
        }
    }

    private boolean isSudokuComplete() {
        // 检查所有行
        for (int row = 0; row < 9; row++) {
            Set<Integer> rowSet = new HashSet<>();
            for (int col = 0; col < 9; col++) {
                if (sudokuData[row][col] == 0 || !rowSet.add(sudokuData[row][col])) {
                    return false;
                }
            }
        }

        // 检查所有列
        for (int col = 0; col < 9; col++) {
            Set<Integer> colSet = new HashSet<>();
            for (int row = 0; row < 9; row++) {
                if (sudokuData[row][col] == 0 || !colSet.add(sudokuData[row][col])) {
                    return false;
                }
            }
        }

        // 检查所有3x3小宫格
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<Integer> boxSet = new HashSet<>();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int actualRow = boxRow * 3 + i;
                        int actualCol = boxCol * 3 + j;
                        if (sudokuData[actualRow][actualCol] == 0 ||
                                !boxSet.add(sudokuData[actualRow][actualCol])) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private void updateErrorDisplay() {
        errorText.setText("错误" + errorCount + "/" + MAX_ERRORS);
        starText.setText("☆" + (MAX_ERRORS - errorCount));
    }

    private void startTimer() {
        startTime = SystemClock.elapsedRealtime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isFinishing() && errorCount < MAX_ERRORS && filledCells < TOTAL_CELLS) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long elapsedMillis = SystemClock.elapsedRealtime() - startTime;
                            long minutes = (elapsedMillis / 1000) / 60;
                            long seconds = (elapsedMillis / 1000) % 60;
                            timerText.setText(String.format("%02d:%02d", minutes, seconds));
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    private void gameWin() {
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        // 发送胜利消息给对方
        String message = NetworkProtocol.createMessage(
                NetworkProtocol.TYPE_GAME_OVER,
                "win"
        );
        out.println(message);

        // 启动结算界面
        Intent intent = new Intent(this, MultiplayerResultActivity.class);
        intent.putExtra("result", "win");
        intent.putExtra("time", formattedTime);
        startActivity(intent);
        finish();
    }

    private void gameOver() {
        // 发送失败消息给对方
        String message = NetworkProtocol.createMessage(
                NetworkProtocol.TYPE_GAME_OVER,
                "lose"
        );
        out.println(message);

        // 启动结算界面
        Intent intent = new Intent(this, MultiplayerResultActivity.class);
        intent.putExtra("result", "lose");
        startActivity(intent);
        finish();
    }
}