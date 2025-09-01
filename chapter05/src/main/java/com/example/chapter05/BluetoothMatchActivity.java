package com.example.chapter05;

import android.graphics.Color;
import android.os.Bundle;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import generator.SudokuGenerator;

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

    private GridLayout sudokuGrid;
    private int[][] sudokuData;
    private Button[] numberButtons;
    private TextView timerText;
    private TextView errorText;
    private TextView opponentErrorText;
    private TextView statusText;
    private long startTime;
    private Map<String, TextView> cellViews = new HashMap<>();
    private int selectedRow = -1;
    private int selectedCol = -1;

    // 游戏状态变量
    private int errorCount = 0;
    private int opponentErrorCount = 0;
    private final int MAX_ERRORS = 3;
    private int filledCells = 0;
    private final int TOTAL_CELLS = 81;
    private int fixedCells = 0;
    private int[][] fullSolution;
    private int[][] predefinedValues;

    // 蓝牙对战相关变量
    private long sharedSeed; // 双方共享的随机数种子
    // 在类变量部分修改：
    private int myFilledCells = 0; // 我自己填的格子数
    private int opponentFilledCells = 0; // 对方填的格子数
    private boolean isGameEnded = false; // 添加这个变量，用于标记游戏是否已结束

    // 笔记模式相关变量
    private boolean isNoteMode = false;
    private ToggleButton btnNoteToggle;
    private int[][][] noteData = new int[9][9][9]; // 存储每个格子的笔记数字
    // 提示相关变量
    private int hintCount = 0;
    private final int MAX_HINTS = 3;
    private Button btnSingleHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_match);

        // 获取蓝牙对战参数
        Intent intent = getIntent();
        sharedSeed = intent.getLongExtra("seed", System.currentTimeMillis());
        isHost = intent.getBooleanExtra("isHost", false);

        initViews();
        setupViews();
        setupListeners();

        // 添加以下调用
        setupNumberButtons();
        setupFunctionButtons();
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

        timerText = findViewById(R.id.timer_text);
        errorText = findViewById(R.id.error_text);
        opponentErrorText = findViewById(R.id.opponent_error_text);
        statusText = findViewById(R.id.status_text);
        sudokuGrid = findViewById(R.id.sudoku_grid);
        // 初始化提示按钮
        btnSingleHint = findViewById(R.id.btn_single_hint);

        // 添加数字按钮初始化
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


        // 添加功能按钮初始化
        findViewById(R.id.btn_hint);
        findViewById(R.id.btn_hint_close);
        findViewById(R.id.btn_return);
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

        // 在准备游戏时
        btnStartGame.setOnClickListener(v -> {
            if (!isConnected) {
                Toast.makeText(this, "请先建立连接", Toast.LENGTH_SHORT).show();
                return;
            }

            isReady = true;
            btnStartGame.setEnabled(false);
            btnStartGame.setText("等待对方准备...");

            sendMessage("READY|1");
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
                if (received == null) break;

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
                        // 使用相同的种子生成数独谜题
                        predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(2, gameSeed);
                        runOnUiThread(() -> {
                            initializeSudokuData();  // 初始化数独数据
                            createSudokuGrid();      // 创建数独网格
                            updateGameInfo();        // 更新游戏信息
                            Toast.makeText(this, "已接收游戏种子: " + gameSeed, Toast.LENGTH_SHORT).show();
                        });
                        break;

                    case "ERROR":
                        opponentErrorCount = Integer.parseInt(data);
                        runOnUiThread(this::updateErrorDisplay);
                        break;

                    case "MOVE":
                        String[] moveParts = data.split(",");
                        int row = Integer.parseInt(moveParts[0]);
                        int col = Integer.parseInt(moveParts[1]);
                        int num = Integer.parseInt(moveParts[2]);
                        runOnUiThread(() -> onOpponentMove(row, col, num));
                        break;

                    // 在 receiveMessages() 方法的 switch 语句中添加一个新的 case
                    case "WIN":
                        runOnUiThread(() -> {
                            // 对手已经获胜，我方胜利（因为对手失误超过三次）
                            gameWin(); // 修改为调用gameWin()而不是gameOver(false)
                        });
                        break;
                    case "DRAW":
                        runOnUiThread(() -> {
                            // 对手发送了平局消息
                            gameDraw();
                        });
                        break;
                    case "COMPLETED":
                        runOnUiThread(() -> {
                            // 对手通知游戏已完成，强制检查游戏状态
                            if (isSudokuComplete()) {
                                checkGameCompletion();
                            }
                        });
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

    // 在开始游戏时
    private void startGame() {
        runOnUiThread(() -> {
            if (isHost) {
                gameSeed = System.currentTimeMillis();
                predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(2, gameSeed);
                sendMessage("SEED|" + gameSeed);
            }

            // 只有在主机端立即初始化，客户端等待接收种子
            if (isHost) {
                initializeSudokuData();
                createSudokuGrid();
            }

            Toast.makeText(this, "游戏开始! 种子: " + gameSeed, Toast.LENGTH_SHORT).show();
            updateGameInfo();
        });
    }

    // 修改 updateGameInfo 方法：
    private void updateGameInfo() {
        runOnUiThread(() -> {
            tvGameSeed.setText("随机种子: " + (gameSeed == 0 ? "未生成" : gameSeed));
            tvMyProgress.setText("我的进度: " + myFilledCells); // 只显示自己填的格子数
            tvOpponentProgress.setText("对方进度: " + opponentFilledCells); // 只显示对方填的格子数
            tvMyStars.setText("我的星星: ☆" + (MAX_ERRORS - errorCount));
            tvOpponentStars.setText("对方星星: ☆" + (MAX_ERRORS - opponentErrorCount));
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

    // 修改 initializeSudokuData 方法：
    private void initializeSudokuData() {
        sudokuData = new int[9][9];
        fixedCells = 0;
        myFilledCells = 0;
        opponentFilledCells = 0;
        filledCells = 0;
        isGameEnded = false;
        noteData = new int[9][9][9]; // 重置笔记数据
        // 初始化所有格子为0（空白）
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                sudokuData[i][j] = 0;
            }
        }

        // 根据预定义值设置谜题
        if (predefinedValues != null) {
            for (int[] predefined : predefinedValues) {
                int row = predefined[0];
                int col = predefined[1];
                int value = predefined[2];
                sudokuData[row][col] = value;
                fixedCells++;
                filledCells++; // 初始谜题的格子也算在总填充数中
            }
        }

        // 生成与预填数字匹配的解决方案
        if (predefinedValues != null) {
            fullSolution = SudokuGenerator.generateSolutionForPuzzle(predefinedValues);
        }
    }

    private void createSudokuGrid() {
        sudokuGrid.removeAllViews();

        // 设置网格背景为黑色
        sudokuGrid.setBackgroundColor(Color.BLACK);

        // 获取屏幕宽度，计算单元格大小
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = (int)(displayMetrics.widthPixels*0.9);
        int padding = (int) (16 * getResources().getDisplayMetrics().density); // 转换为像素

        // 计算可用宽度（减去左右padding）
        int availableWidth = screenWidth - 2 * padding;

        // 计算总网格线宽度（8条细线 + 2条粗线）
        int totalGridLineWidth = 8 * 1 + 2 * 3;

        // 计算单元格大小（确保所有单元格和网格线都能放下）
        int cellSize = (availableWidth - totalGridLineWidth) / 9;

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cellView = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize; // 确保高度和宽度相同，形成正方形
                params.rowSpec = GridLayout.spec(row);
                params.columnSpec = GridLayout.spec(col);

                // 设置边距来创建网格线效果
                int leftMargin = (col % 3 == 0) ? 3 : 1;    // 九宫格左边加粗
                int topMargin = (row % 3 == 0) ? 3 : 1;     // 九宫格上边加粗
                int rightMargin = (col % 3 == 2) ? 3 : 1;   // 九宫格右边加粗
                int bottomMargin = (row % 3 == 2) ? 3 : 1;  // 九宫格下边加粗

                params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                cellView.setLayoutParams(params);
                cellView.setGravity(Gravity.CENTER);
                cellView.setTextSize(20);

                // 设置单元格背景为白色
                cellView.setBackgroundColor(Color.WHITE);

                int value = sudokuData[row][col];
                if (value != 0) {
                    cellView.setText(String.valueOf(value));
                    cellView.setTextColor(Color.BLACK);
                    cellView.setTag("fixed");
                } else {
                    // 显示笔记（如果有）
                    StringBuilder notes = new StringBuilder();
                    for (int i = 0; i < 9; i++) {
                        if (noteData[row][col][i] != 0) {
                            notes.append(noteData[row][col][i]);
                        }
                    }
                    if (notes.length() > 0) {
                        cellView.setText(notes.toString());
                        cellView.setTextSize(12);
                    } else {
                        cellView.setText("");
                    }
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
            numberButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNumberSelected(number);
                }
            });
        }
    }
    private void setupFunctionButtons() {

        // 单个提示按钮
        btnSingleHint = findViewById(R.id.btn_single_hint);
        btnSingleHint.setOnClickListener(v -> {
            if (hintCount >= MAX_HINTS) {
                Toast.makeText(this, "提示次数已用完", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRow == -1 || selectedCol == -1) {
                Toast.makeText(this, "请先选择一个格子", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查是否是预填格子
            TextView cell = cellViews.get(selectedRow + "_" + selectedCol);
            if (cell != null && "fixed".equals(cell.getTag())) {
                Toast.makeText(this, "不能提示预填格子", Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查格子是否已填
            if (sudokuData[selectedRow][selectedCol] != 0) {
                Toast.makeText(this, "格子已填写", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用提示
            useSingleHint();
        });

// 更新提示按钮显示
        updateHintButtonDisplay();

        // 笔记模式开关按钮
        btnNoteToggle = findViewById(R.id.btn_note_toggle);
        btnNoteToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isNoteMode = isChecked;
            if (!isChecked) {
                clearAllNotes();
            }
            Toast.makeText(this, "笔记模式已" + (isChecked ? "开启" : "关闭"),
                    Toast.LENGTH_SHORT).show();
        });
        // 提示按钮
        Button hintButton = findViewById(R.id.btn_hint);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullSolution();
            }
        });

        // 关闭提示按钮
        Button closeHintButton = findViewById(R.id.btn_hint_close);
        closeHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSolutionHint();
            }
        });

        // 返回按钮
        Button returnButton = findViewById(R.id.btn_return);
        returnButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认退出")
                    .setMessage("确定要退出对战吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 通知对手自己已退出
                        // sendQuitMessage();

                        // 返回主界面
                        Intent intent = new Intent(this, UserMsgActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
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

    // 修改 onNumberSelected 方法中的相关部分：
    private void onNumberSelected(int number) {
        if (selectedRow == -1 || selectedCol == -1) {
            Toast.makeText(this, "请先选择一个格子", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView cell = cellViews.get(selectedRow + "_" + selectedCol);
        if (cell != null && !"fixed".equals(cell.getTag())) {
            if (isNoteMode) {
                // 笔记模式处理
                if (noteData[selectedRow][selectedCol][number - 1] == 0) {
                    noteData[selectedRow][selectedCol][number - 1] = number;
                    // 显示所有笔记数字
                    StringBuilder notes = new StringBuilder();
                    for (int i = 0; i < 9; i++) {
                        if (noteData[selectedRow][selectedCol][i] != 0) {
                            notes.append(noteData[selectedRow][selectedCol][i]);
                        }
                    }
                    cell.setText(notes.toString());
                    cell.setTextSize(12); // 小字号显示笔记
                } else {
                    noteData[selectedRow][selectedCol][number - 1] = 0;
                    // 更新显示
                    StringBuilder notes = new StringBuilder();
                    for (int i = 0; i < 9; i++) {
                        if (noteData[selectedRow][selectedCol][i] != 0) {
                            notes.append(noteData[selectedRow][selectedCol][i]);
                        }
                    }
                    cell.setText(notes.toString());
                    if (notes.length() == 0) {
                        cell.setText("");
                        cell.setTextSize(20); // 恢复普通字号
                    }
                }
            } else {
                // 正常模式处理（原有逻辑）
                if (isValidMove(selectedRow, selectedCol, number)) {
                    cell.setText(String.valueOf(number));
                    cell.setTextColor(Color.BLUE);
                    cell.setTextSize(20); // 恢复普通字号
                    sudokuData[selectedRow][selectedCol] = number;
                    myFilledCells++;
                    filledCells++;

                    // 更新进度显示
                    updateGameInfo();

                    // 检查游戏是否完成
                    checkGameCompletion();

                    // 发送移动消息给对手
                    sendMessage("MOVE|" + selectedRow + "," + selectedCol + "," + number);
                } else {
                    // 无效输入
                    errorCount++;
                    updateErrorDisplay();
                    // 发送错误计数给对手
                    sendMessage("ERROR|" + errorCount);

                    if (errorCount >= MAX_ERRORS) {
                        gameOver(true);
                        return;
                    } else {
                        Toast.makeText(this, "输入错误！剩余机会: " + (MAX_ERRORS - errorCount),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private boolean isValidMove(int row, int col, int number) {
        // 检查行
        for (int i = 0; i < 9; i++) {
            if (i != col && sudokuData[row][i] == number) {
                return false;
            }
        }

        // 检查列
        for (int i = 0; i < 9; i++) {
            if (i != row && sudokuData[i][col] == number) {
                return false;
            }
        }

        // 检查3x3小宫格
        int boxRowStart = (row / 3) * 3;
        int boxColStart = (col / 3) * 3;

        for (int i = boxRowStart; i < boxRowStart + 3; i++) {
            for (int j = boxColStart; j < boxColStart + 3; j++) {
                if (i != row && j != col && sudokuData[i][j] == number) {
                    return false;
                }
            }
        }

        return true;
    }

    private void updateErrorDisplay() {
        runOnUiThread(() -> {
            errorText.setText("你: " + errorCount + "/" + MAX_ERRORS);
            opponentErrorText.setText("对手: " + opponentErrorCount + "/" + MAX_ERRORS);

            // 更新星星显示
            tvMyStars.setText("我的星星: ☆" + (MAX_ERRORS - errorCount));
            tvOpponentStars.setText("对方星星: ☆" + (MAX_ERRORS - opponentErrorCount));
        });
    }

    private void updateStatusText(String status) {
        statusText.setText(status);
    }

    private void checkGameCompletion() {
        if (isSudokuComplete()) {
            // 游戏完成时，比较双方填的格子数
            if (myFilledCells > opponentFilledCells) {
                // 我方填的格子多，我方胜利
                gameWin();
            } else if (myFilledCells < opponentFilledCells) {
                // 对方填的格子多，我方失败
                gameOver(false);
            } else {
                // 填的格子数相同，平局
                gameDraw();
            }

            // 发送游戏完成消息给对手
            sendMessage("COMPLETED|1");
        }
    }

    private void gameDraw() {
        if (isGameEnded) return;
        isGameEnded = true;
        // 发送平局消息给对手
        sendMessage("DRAW|1");

        // 启动平局界面
        Intent intent = new Intent(this, BluetoothMatchDrawActivity.class);
        intent.putExtra("isHost", isHost);
        intent.putExtra("filledCells", myFilledCells);
        startActivity(intent);
        finish();
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
    private void gameOver(boolean isVoluntary) {
        if (isGameEnded) return;
        isGameEnded = true;

        // 只有在主动失败（错误次数达到上限）时才通知对手
        if (isVoluntary) {
            sendMessage("WIN|1");
        }

        // 禁用所有数字按钮
        for (Button btn : numberButtons) {
            btn.setEnabled(false);
        }

        // 启动失败界面
        Intent intent = new Intent(this, BluetoothMatchFailedActivity.class);
        intent.putExtra("isHost", isHost);
        intent.putExtra("isVoluntary", isVoluntary); // 传递是否是主动失败
        startActivity(intent);
        finish();
    }
    private void gameWin() {
        if (isGameEnded) return;
        isGameEnded = true;

        // 移除格子数比较检查，因为当对手失误超过三次时，无论格子数如何都应该胜利

        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        // 计算获得的星星数 (3 - 错误数)
        int starsEarned = MAX_ERRORS - errorCount;

        // 启动胜利界面
        Intent intent = new Intent(this, BluetoothMatchSucceedActivity.class);
        intent.putExtra("isHost", isHost);
        intent.putExtra("time", formattedTime);
        intent.putExtra("filledCells", myFilledCells);
        intent.putExtra("stars", starsEarned);
        intent.putExtra("opponentFilledCells", opponentFilledCells);
        startActivity(intent);
        finish();
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

    // 显示完整解决方案
    private void showFullSolution() {
        if (fullSolution == null) return;

        if (!isSolutionValid(fullSolution)) {
            Toast.makeText(this, "解决方案无效，请重新生成", Toast.LENGTH_LONG).show();
            return;
        }

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = cellViews.get(row + "_" + col);
                if (cell != null && !"fixed".equals(cell.getTag())) {
                    if (cell.getText().length() > 0 && !"hint".equals(cell.getTag())) {
                        sudokuData[row][col] = Integer.parseInt(cell.getText().toString());
                    }
                    cell.setText(String.valueOf(fullSolution[row][col]));
                    cell.setTextColor(Color.RED);
                    cell.setTag("hint");
                }
            }
        }

        for (Button btn : numberButtons) {
            btn.setEnabled(false);
        }

        Toast.makeText(this, "已显示完整解决方案", Toast.LENGTH_SHORT).show();
    }

    // 验证解决方案是否符合数独规则
    private boolean isSolutionValid(int[][] solution) {
        // 检查所有行
        for (int row = 0; row < 9; row++) {
            Set<Integer> rowSet = new HashSet<>();
            for (int col = 0; col < 9; col++) {
                if (!rowSet.add(solution[row][col])) {
                    return false;
                }
            }
        }

        // 检查所有列
        for (int col = 0; col < 9; col++) {
            Set<Integer> colSet = new HashSet<>();
            for (int row = 0; row < 9; row++) {
                if (!colSet.add(solution[row][col])) {
                    return false;
                }
            }
        }

        // 检查所有3x3宫格
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                Set<Integer> boxSet = new HashSet<>();
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        int actualRow = boxRow * 3 + i;
                        int actualCol = boxCol * 3 + j;
                        if (!boxSet.add(solution[actualRow][actualCol])) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    // 隐藏解决方案提示，恢复用户输入
    private void hideSolutionHint() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = cellViews.get(row + "_" + col);
                if (cell != null && "hint".equals(cell.getTag())) {
                    cell.setTag(null);
                    if (sudokuData[row][col] != 0) {
                        cell.setText(String.valueOf(sudokuData[row][col]));
                        cell.setTextColor(Color.BLUE);
                    } else {
                        cell.setText("");
                    }
                }
            }
        }

        for (Button btn : numberButtons) {
            btn.setEnabled(true);
        }

        Toast.makeText(this, "已隐藏解决方案，恢复游戏", Toast.LENGTH_SHORT).show();
    }

    // 修改 onOpponentMove 方法：
    public void onOpponentMove(int row, int col, int number) {
        runOnUiThread(() -> {
            TextView cell = cellViews.get(row + "_" + col);
            if (cell != null && !"fixed".equals(cell.getTag()) && sudokuData[row][col] == 0) {
                cell.setText(String.valueOf(number));
                // 如果是提示的格子，显示绿色，否则显示蓝色
                cell.setTextColor(cell.getTextColors().getDefaultColor() == Color.GREEN ?
                        Color.GREEN : Color.BLUE);
                sudokuData[row][col] = number;
                opponentFilledCells++;
                filledCells++;

                updateGameInfo();

                if (isSudokuComplete()) {
                    checkGameCompletion();
                }
            }
        });
    }

    // 处理对手的错误更新
    public void onOpponentErrorUpdate(int errorCount) {
        runOnUiThread(() -> {
            this.opponentErrorCount = errorCount;
            updateErrorDisplay();

            if (opponentErrorCount >= MAX_ERRORS) {
                // 对手已经输了，你赢了
                gameWin();
            }
        });
    }

    private void sendMessage(String message) {
        if (out != null && !out.checkError()) {
            new Thread(() -> {
                try {
                    out.println(message);
                    out.flush();
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "发送失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetConnection();
                    });
                }
            }).start();
        }
    }

    // 清除所有笔记
    private void clearAllNotes() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (sudokuData[i][j] == 0) { // 只清空白格子的笔记
                    TextView cell = cellViews.get(i + "_" + j);
                    if (cell != null && !"fixed".equals(cell.getTag())) {
                        cell.setText("");
                    }
                }
            }
        }
        noteData = new int[9][9][9];
    }

    // 使用单个提示
    private void useSingleHint() {
        if (fullSolution == null || selectedRow == -1 || selectedCol == -1) {
            return;
        }

        int correctValue = fullSolution[selectedRow][selectedCol];
        TextView cell = cellViews.get(selectedRow + "_" + selectedCol);

        if (cell != null) {
            cell.setText(String.valueOf(correctValue));
            cell.setTextColor(Color.GREEN); // 用绿色表示提示
            cell.setTextSize(20);
            sudokuData[selectedRow][selectedCol] = correctValue;
            myFilledCells++;
            filledCells++;

            hintCount++;
            updateHintButtonDisplay();

            // 发送移动消息给对手
            sendMessage("MOVE|" + selectedRow + "," + selectedCol + "," + correctValue);

            // 检查游戏是否完成
            checkGameCompletion();
        }
    }

    // 更新提示按钮显示
    private void updateHintButtonDisplay() {
        runOnUiThread(() -> {
            btnSingleHint.setText("提示 (" + (MAX_HINTS - hintCount) + ")");
        });
    }



}