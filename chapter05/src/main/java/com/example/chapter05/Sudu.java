package com.example.chapter05;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Sudu extends AppCompatActivity {

    private GridLayout sudokuContainer;
    private int[][] sudokuData;
    private Button[] numberButtons;
    private TextView timerText;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudu);

        // 初始化数独数据
        initializeSudokuData();

        // 设置顶部信息
        timerText = findViewById(R.id.timer_text);

        // 设置数独网格容器
        sudokuContainer = findViewById(R.id.sudoku_container);

        // 创建数独网格
        createSudokuGrid();

        // 设置数字按钮
        setupNumberButtons();

        // 设置功能按钮
        setupFunctionButtons();

        // 开始计时
        startTimer();
    }

    private void initializeSudokuData() {
        // 根据图片中的数独布局初始化数据
        sudokuData = new int[9][9];

        // 初始化所有格子为0（空白）
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                sudokuData[i][j] = 0;
            }
        }

        // 设置已知数字（根据图片中的布局）
        // 第一行
        sudokuData[0][7] = 7;

        // 第二行
        sudokuData[1][4] = 1;
        sudokuData[1][5] = 7;
        sudokuData[1][7] = 2;

        // 第三行
        sudokuData[2][0] = 2;
        sudokuData[2][1] = 1;
        sudokuData[2][2] = 7;
        sudokuData[2][6] = 8;
        sudokuData[2][7] = 6;
        sudokuData[2][8] = 3;

        // 第四行
        sudokuData[3][1] = 8;
        sudokuData[3][2] = 6;
        sudokuData[3][4] = 2;
        sudokuData[3][5] = 7;

        // 第五行
        sudokuData[4][2] = 5;
        sudokuData[4][3] = 7;

        // 第六行
        sudokuData[5][0] = 4;
        sudokuData[5][1] = 7;
        sudokuData[5][3] = 1;
        sudokuData[5][7] = 9;
        sudokuData[5][8] = 6;

        // 第七行
        sudokuData[6][1] = 5;
        sudokuData[6][2] = 9;
        sudokuData[6][6] = 6;
        sudokuData[6][7] = 3;

        // 第八行
        sudokuData[7][0] = 6;
        sudokuData[7][2] = 3;
        sudokuData[7][3] = 9;
        sudokuData[7][4] = 8;

        // 第九行
        sudokuData[8][3] = 6;
        sudokuData[8][5] = 2;
        sudokuData[8][7] = 9;
    }

    private void createSudokuGrid() {
        // 清除所有子视图
        sudokuContainer.removeAllViews();

        // 创建9个3x3的小型矩阵
        for (int boxRow = 0; boxRow < 3; boxRow++) {
            for (int boxCol = 0; boxCol < 3; boxCol++) {
                // 为每个小型矩阵创建一个GridLayout
                GridLayout smallGrid = new GridLayout(this);
                smallGrid.setRowCount(3);
                smallGrid.setColumnCount(3);
                smallGrid.setBackgroundResource(android.R.color.darker_gray);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = 0;
                params.rowSpec = GridLayout.spec(boxRow, 1, 1f);
                params.columnSpec = GridLayout.spec(boxCol, 1, 1f);
                params.setMargins(2, 2, 2, 2);
                smallGrid.setLayoutParams(params);

                // 填充小型矩阵的单元格
                for (int cellRow = 0; cellRow < 3; cellRow++) {
                    for (int cellCol = 0; cellCol < 3; cellCol++) {
                        // 计算在完整数独网格中的行和列
                        int fullRow = boxRow * 3 + cellRow;
                        int fullCol = boxCol * 3 + cellCol;

                        // 创建单元格视图
                        SudokuCellView cellView = new SudokuCellView(this);
                        cellView.setValue(sudokuData[fullRow][fullCol]);
                        cellView.setRow(fullRow);
                        cellView.setColumn(fullCol);

                        GridLayout.LayoutParams cellParams = new GridLayout.LayoutParams();
                        cellParams.width = 0;
                        cellParams.height = 0;
                        cellParams.rowSpec = GridLayout.spec(cellRow, 1, 1f);
                        cellParams.columnSpec = GridLayout.spec(cellCol, 1, 1f);
                        cellParams.setMargins(1, 1, 1, 1);
                        cellView.setLayoutParams(cellParams);

                        smallGrid.addView(cellView);
                    }
                }

                sudokuContainer.addView(smallGrid);
            }
        }
    }

    private void setupNumberButtons() {
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

        for (int i = 0; i < 9; i++) {
            final int number = i + 1;
            numberButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 处理数字按钮点击
                    onNumberSelected(number);
                }
            });
        }
    }

    private void setupFunctionButtons() {
        Button undoBtn = findViewById(R.id.btn_undo);
        Button eraseBtn = findViewById(R.id.btn_erase);
        Button noteBtn = findViewById(R.id.btn_note);
        Button hintBtn = findViewById(R.id.btn_hint);

        undoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 撤销操作
            }
        });

        eraseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 擦除操作
            }
        });

        noteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 笔记模式切换
            }
        });

        hintBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 提示操作
            }
        });
    }

    private void onNumberSelected(int number) {
        // 处理数字选择
        // 这里需要实现将选中的数字填入当前选中的格子
    }

    private void startTimer() {
        startTime = SystemClock.elapsedRealtime();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isFinishing()) {
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

    // 自定义数独单元格视图
    private class SudokuCellView extends androidx.appcompat.widget.AppCompatTextView {
        private int row;
        private int column;
        private int value;
        private boolean isFixed;

        public SudokuCellView(android.content.Context context) {
            super(context);
            setTextSize(20);
            setTextColor(0xFF000000);
            setBackgroundColor(0xFFFFFFFF);
            setGravity(android.view.Gravity.CENTER);
        }

        public void setValue(int value) {
            this.value = value;
            if (value == 0) {
                setText("");
            } else {
                setText(String.valueOf(value));
            }
        }

        public int getValue() {
            return value;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getRow() {
            return row;
        }

        public void setColumn(int column) {
            this.column = column;
        }

        public int getColumn() {
            return column;
        }

        public void setFixed(boolean fixed) {
            isFixed = fixed;
            if (fixed) {
                setTextColor(0xFF000000);
            } else {
                setTextColor(0xFF0000FF);
            }
        }

        public boolean isFixed() {
            return isFixed;
        }
    }
}