package com.example.chapter05;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        // 使用随机种子生成谜题
        long seed = System.currentTimeMillis();
        predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(2, seed);

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
    // 添加难度选择功能
    public void setDifficulty(int difficulty) {
        long seed = System.currentTimeMillis();
        predefinedValues = SudokuGenerator.generatePredefinedValuesWithSeed(difficulty, seed);
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
    private void createSudokuGrid() {
        sudokuGrid.removeAllViews();

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
                } else {
                    cellView.setText("");
                    cellView.setTextColor(Color.BLUE);
                }

                final int finalRow = row;
                final int finalCol = col;
                cellView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCellSelected(finalRow, finalCol);
                    }
                });

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
        // 提示按钮（显示完整解决方案）
        Button hintButton = findViewById(R.id.btn_hint);
        hintButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFullSolution();
                //作弊按钮，直接判定获胜
                gameWin();
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
// 返回主界面按钮
        Button returnButton = findViewById(R.id.btn_return);
        returnButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("确认退出")
                    .setMessage("确定要返回主界面吗？当前游戏进度将丢失")
                    .setPositiveButton("确定", (dialog, which) -> {
                        // 记录中途退出 (得分为0)
                        SharedPreferencesUtil.saveGameRecord(this,
                                new SharedPreferencesUtil.GameRecord(0, "00:00", 0));

                        // 返回UserMsgActivity
                        Intent intent = new Intent(this, UserMsgActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
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

    private void onNumberSelected(int number) {
        if (selectedRow == -1 || selectedCol == -1) {
            Toast.makeText(this, "请先选择一个格子", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView cell = cellViews.get(selectedRow + "_" + selectedCol);
        if (cell != null && !"fixed".equals(cell.getTag())) {
            // 检查输入是否有效
            if (isValidMove(selectedRow, selectedCol, number)) {
                // 有效输入
                cell.setText(String.valueOf(number));
                sudokuData[selectedRow][selectedCol] = number;
                cell.setTextColor(Color.BLUE);
                filledCells++;

                // 检查是否完成游戏
                if (filledCells == TOTAL_CELLS) {
                    checkGameCompletion();
                }
            } else {
                // 无效输入
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

        // 启动失败界面
        Intent intent = new Intent(this, SingleFailedActivity.class);
        startActivity(intent);
        finish();
    }

    private void gameWin() {
        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);

        // 计算获得的星星 (3 - 错误次数)
        int starsEarned = MAX_ERRORS - errorCount;

        // 启动结算界面
        Intent intent = new Intent(this, SingleSucceedActivity.class);
        intent.putExtra("stars", starsEarned);
        intent.putExtra("time", formattedTime);
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

}