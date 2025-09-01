package com.example.chapter05;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import generator.SudokuGenerator;
import utils.SharedPreferencesUtil;

public class SudokuActivity extends AppCompatActivity {

    private GridLayout sudokuGrid;
    private int[][] sudokuData;
    private Button[] numberButtons;
    private TextView timerText;
    private TextView errorText;
    private TextView starText;
    private long startTime;
    private Map<String, TextView> cellViews = new HashMap<>();
    private int selectedRow = -1;
    private int selectedCol = -1;

    // 游戏状态变量
    private int errorCount = 0;
    private final int MAX_ERRORS = 3;
    private int filledCells = 0;
    private final int TOTAL_CELLS = 81;
    private int fixedCells = 0; // 预填的格子数量
    private int[][] fullSolution; // 存储完整解决方案
    // 预填数字的位置
    private int[][] predefinedValues;
    // 在类变量中添加
    private boolean isNoteMode = false;
    private ToggleButton btnNoteToggle;

    private int hintCount = 0;
    private final int MAX_HINTS = 3;
    private Button btnSingleHint;
    private int[][][] noteData = new int[9][9][9]; // 存储每个格子的笔记数字
    // 在类变量中添加
    private long elapsedTime = 0;
    private boolean isGameLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        if (SharedPreferencesUtil.hasSavedGame(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("加载存档")
                    .setMessage("检测到有保存的游戏，是否加载？")
                    .setPositiveButton("加载", (dialog, which) -> {
                        loadGame();
                        isGameLoaded = true;
                    })
                    .setNegativeButton("新建游戏", (dialog, which) -> {
                        SharedPreferencesUtil.clearSavedGame(this);
                        isGameLoaded = false;
                    })
                    .show();
        }

        // 读取保存的难度，如果没有则使用传入的难度
        int savedDifficulty = SharedPreferencesUtil.getDifficulty(this);
        int intentDifficulty = getIntent().getIntExtra("difficulty", savedDifficulty);


        // 使用随机种子生成谜题
        long seed = System.currentTimeMillis();
        int difficulty = getIntent().getIntExtra("difficulty", 2); // 默认为中等难度
        predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(difficulty, seed);

        // 设置顶部信息
        timerText = findViewById(R.id.timer_text);
        errorText = findViewById(R.id.error_text);
        starText = findViewById(R.id.star_text);

        // 设置数独网格
        sudokuGrid = findViewById(R.id.sudoku_grid);



        // 初始化数独数据
        initializeSudokuData();

        // 创建数独网格
        createSudokuGrid();

        // 设置数字按钮
        setupNumberButtons();

        // 设置功能按钮
        setupFunctionButtons();

        // 开始计时
        startTimer();

        // 更新错误计数显示
        updateErrorDisplay();
    }

    private void initializeSudokuData() {
        sudokuData = new int[9][9];

        // 初始化所有格子为0（空白）
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                sudokuData[i][j] = 0;
            }
        }

        // 根据预定义值设置谜题
        for (int[] predefined : predefinedValues) {
            int row = predefined[0];
            int col = predefined[1];
            int value = predefined[2];
            sudokuData[row][col] = value;
            fixedCells++;
        }

        // 生成与预填数字匹配的解决方案
        fullSolution = SudokuGenerator.generateSolutionForPuzzle(predefinedValues);

        filledCells = fixedCells;
    }

    private void showDifficultyDialog() {
        String[] difficultyOptions = {"简单", "中等", "困难"};

        new AlertDialog.Builder(this)
                .setTitle("选择难度")
                .setItems(difficultyOptions, (dialog, which) -> {
                    // which: 0=简单, 1=中等, 2=困难
                    setDifficulty(which + 1); // 转换为1-3的难度级别
                    Toast.makeText(this, "已选择" + difficultyOptions[which] + "难度",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    // 添加难度选择功能
    public void setDifficulty(int difficulty) {
        // 验证难度值在有效范围内
        if (difficulty < 1 || difficulty > 3) {
            difficulty = 2; // 默认中等难度
        }

        // 保存当前选择的难度
        SharedPreferencesUtil.saveDifficulty(this, difficulty);

        // 使用随机种子生成新谜题
        long seed = System.currentTimeMillis();
        predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(difficulty, seed);

        // 重置游戏
        resetGame();
    }

    // 重置游戏
    private void resetGame() {
        // 重置游戏状态
        errorCount = 0;
        filledCells = 0;
        fixedCells = 0;
        selectedRow = -1;
        selectedCol = -1;
        hintCount = 0;
        updateHintButtonDisplay();

        // 重新初始化数据
        initializeSudokuData();

        // 重新创建网格
        createSudokuGrid();

        // 更新显示
        updateErrorDisplay();

        // 重新启用按钮
        for (Button btn : numberButtons) {
            btn.setEnabled(true);
        }

        // 重新开始计时
        startTimer();
    }
    // 修改createSudokuGrid方法，在创建格子时考虑笔记
    private void createSudokuGrid() {
        sudokuGrid.removeAllViews();

        // 设置网格背景为深灰色
        sudokuGrid.setBackgroundColor(ContextCompat.getColor(this, R.color.grid_background));

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cellView = new TextView(this);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(row, 1, 1f);
                params.columnSpec = GridLayout.spec(col, 1, 1f);

                // 设置边距来创建网格线效果
                int leftMargin = (col % 3 == 0) ? 4 : 1;    // 九宫格左边加粗（深蓝色）
                int topMargin = (row % 3 == 0) ? 4 : 1;     // 九宫格上边加粗（深蓝色）
                int rightMargin = (col % 3 == 2) ? 4 : 1;   // 九宫格右边加粗（深蓝色）
                int bottomMargin = (row % 3 == 2) ? 4 : 1;  // 九宫格下边加粗（深蓝色）

                params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                cellView.setLayoutParams(params);
                cellView.setGravity(Gravity.CENTER);
                cellView.setTextSize(20);

                // 设置单元格背景为白色
                cellView.setBackgroundColor(ContextCompat.getColor(this, R.color.cell_background));

                // 为单元格添加自定义边框
                GradientDrawable border = new GradientDrawable();
                border.setColor(ContextCompat.getColor(this, R.color.cell_background));
//                border.setStroke(1, ContextCompat.getColor(this, R.color.dark_red)); // 细线条为深红色

                // 为九宫格边界添加特殊边框（深蓝色加粗）
//                if (col % 3 == 2) {
//                    border.setStroke(3, ContextCompat.getColor(this, R.color.dark_blue)); // 右边加粗深蓝色
//                }
//                if (row % 3 == 2) {
//                    border.setStroke(3, ContextCompat.getColor(this, R.color.dark_blue)); // 下边加粗深蓝色
//                }
//                if (col % 3 == 0) {
//                    border.setStroke(3, ContextCompat.getColor(this, R.color.dark_blue)); // 左边加粗深蓝色
//                }
//                if (row % 3 == 0) {
//                    border.setStroke(3, ContextCompat.getColor(this, R.color.dark_blue)); // 上边加粗深蓝色
//                }

                cellView.setBackground(border);

                int value = sudokuData[row][col];
                if (value != 0) {
                    cellView.setText(String.valueOf(value));
                    cellView.setTextColor(Color.BLACK);
                    cellView.setTag("fixed");
                } else {
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
        // 在setupFunctionButtons方法中添加保存按钮
        Button btnSaveGame = findViewById(R.id.btn_save_game);
        btnSaveGame.setOnClickListener(v -> saveGame());
        // 提示按钮（显示完整解决方案）
        Button hintButton = findViewById(R.id.btn_hint);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullSolution();
                //作弊按钮，直接判定获胜
//                gameWin();
            }
        });

        // 关闭提示按钮（隐藏提示，恢复用户输入）
        Button closeHintButton = findViewById(R.id.btn_hint_close);
        closeHintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSolutionHint();
            }
        });
        // 在setupFunctionButtons()方法中添加
        Button btnDifficulty = findViewById(R.id.btn_difficulty);
        btnDifficulty.setOnClickListener(v -> showDifficultyDialog());
// 返回主界面按钮
        Button returnButton = findViewById(R.id.btn_return);
        returnButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认退出")
                    .setMessage("确定要返回主界面吗？")
                    .setPositiveButton("保存并退出", (dialog, which) -> {
                        saveGame(); // 保存当前游戏
                        Intent intent = new Intent(this, UserMsgActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("直接退出", (dialog, which) -> {
                        SharedPreferencesUtil.clearSavedGame(this); // 清除存档
                        Intent intent = new Intent(this, UserMsgActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setNeutralButton("取消", null)
                    .show();
        });

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

    }
    // 添加笔记相关方法
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
    // 显示完整解决方案
    private void showFullSolution() {
        if (fullSolution == null) return;

        // 验证解决方案是否正确
        if (!isSolutionValid(fullSolution)) {
            Toast.makeText(this, "解决方案无效，请重新生成", Toast.LENGTH_LONG).show();
            return;
        }
        // 遍历所有格子
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = cellViews.get(row + "_" + col);
                if (cell != null && !"fixed".equals(cell.getTag())) {
                    // 保存当前用户输入的值（如果有）
                    if (cell.getText().length() > 0 && !"hint".equals(cell.getTag())) {
                        sudokuData[row][col] = Integer.parseInt(cell.getText().toString());
                    }
                    // 显示解决方案
                    cell.setText(String.valueOf(fullSolution[row][col]));
                    cell.setTextColor(Color.RED); // 用红色显示提示
                    cell.setTag("hint"); // 标记为提示
                }
            }
        }

        // 禁用数字按钮，防止覆盖提示
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
        // 遍历所有格子
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                TextView cell = cellViews.get(row + "_" + col);
                if (cell != null && "hint".equals(cell.getTag())) {
                    // 清除提示标记
                    cell.setTag(null);

                    // 恢复用户输入或清空
                    if (sudokuData[row][col] != 0) {
                        cell.setText(String.valueOf(sudokuData[row][col]));
                        cell.setTextColor(Color.BLUE);
                    } else {
                        cell.setText("");
                    }
                }
            }
        }

        // 重新启用数字按钮
        for (Button btn : numberButtons) {
            btn.setEnabled(true);
        }

        Toast.makeText(this, "已隐藏解决方案，恢复游戏", Toast.LENGTH_SHORT).show();
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

    // 修改onNumberSelected方法
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
                    sudokuData[selectedRow][selectedCol] = number;
                    cell.setTextColor(Color.BLUE);
                    cell.setTextSize(20); // 恢复普通字号
                    filledCells++;

                    if (filledCells == TOTAL_CELLS) {
                        checkGameCompletion();
                    }
                } else {
                    errorCount++;
                    updateErrorDisplay();

                    if (errorCount >= MAX_ERRORS) {
                        gameOver();
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
        errorText.setText("错误" + errorCount + "/" + MAX_ERRORS);
        starText.setText("☆" + (MAX_ERRORS - errorCount));
    }

    private void checkGameCompletion() {
        // 检查整个数独是否完全正确
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

    private void gameOver() {
        // 禁用所有数字按钮
        for (Button btn : numberButtons) {
            btn.setEnabled(false);
        }

        // 保存游戏记录
        SharedPreferencesUtil.saveGameRecord(this,
                new SharedPreferencesUtil.GameRecord(0, "00:00", hintCount));

        // 启动失败界面
        Intent intent = new Intent(this, SingleFailedActivity.class);
        startActivity(intent);
        // 清除存档
        SharedPreferencesUtil.clearSavedGame(this);
        finish();
    }

    private void gameWin() {
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        // 计算获得的星星 (3 - 错误次数)
        int starsEarned = MAX_ERRORS - errorCount;

        // 可以在保存游戏记录时加入提示使用情况
        SharedPreferencesUtil.saveGameRecord(this,
                new SharedPreferencesUtil.GameRecord(starsEarned, formattedTime, hintCount));

        // 启动结算界面
        Intent intent = new Intent(this, SingleSucceedActivity.class);
        intent.putExtra("stars", starsEarned);
        intent.putExtra("time", formattedTime);
        startActivity(intent);
        // 清除存档
        SharedPreferencesUtil.clearSavedGame(this);
        finish();
    }

    // 修改startTimer方法，考虑已用时间
    private void startTimer() {
        if (!isGameLoaded) {
            startTime = SystemClock.elapsedRealtime();
            elapsedTime = 0;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isFinishing() && errorCount < MAX_ERRORS && filledCells < TOTAL_CELLS) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long currentElapsed = SystemClock.elapsedRealtime() - startTime;
                            long totalElapsed = elapsedTime + currentElapsed;
                            long minutes = (totalElapsed / 1000) / 60;
                            long seconds = (totalElapsed / 1000) % 60;
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
            filledCells++;

            hintCount++;
            updateHintButtonDisplay();

            // 检查游戏是否完成
            if (filledCells == TOTAL_CELLS) {
                checkGameCompletion();
            }
        }
    }

    // 更新提示按钮显示
    private void updateHintButtonDisplay() {
        btnSingleHint.setText("提示 (" + (MAX_HINTS - hintCount) + ")");
    }
    // 添加保存游戏方法
    private void saveGame() {
        // 计算已用时间
        long currentElapsed = SystemClock.elapsedRealtime() - startTime;

        // 将3D笔记数据转换为2D数组（简化处理）
        int[][] simplifiedNotes = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                for (int k = 0; k < 9; k++) {
                    if (noteData[i][j][k] != 0) {
                        simplifiedNotes[i][j] |= (1 << k); // 使用位掩码简化存储
                    }
                }
            }
        }

        SharedPreferencesUtil.saveGameState(this, sudokuData, simplifiedNotes,
                errorCount, filledCells, currentElapsed + elapsedTime, hintCount); // 添加hintCount参数

        Toast.makeText(this, "游戏已保存", Toast.LENGTH_SHORT).show();
    }

    // 添加加载游戏方法
    private void loadGame() {
        SharedPreferencesUtil.SavedGameState savedState = SharedPreferencesUtil.loadGameState(this);

        if (savedState != null) {
            sudokuData = savedState.sudokuData;
            errorCount = savedState.errorCount;
            filledCells = savedState.filledCells;
            elapsedTime = savedState.elapsedTime;
            hintCount = savedState.hintCount; // 恢复提示数量

            // 恢复笔记数据
            int[][] simplifiedNotes = savedState.noteData;
            noteData = new int[9][9][9];
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (simplifiedNotes[i][j] != 0) {
                        for (int k = 0; k < 9; k++) {
                            if ((simplifiedNotes[i][j] & (1 << k)) != 0) {
                                noteData[i][j][k] = k + 1;
                            }
                        }
                    }
                }
            }

            // 更新UI
            createSudokuGrid();
            updateErrorDisplay();
            updateHintButtonDisplay(); // 更新提示按钮显示

            // 恢复计时器
            startTime = SystemClock.elapsedRealtime() - elapsedTime;

            Toast.makeText(this, "游戏已加载", Toast.LENGTH_SHORT).show();
        }
    }

}