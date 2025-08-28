package com.example.chapter05;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import utils.SharedPreferencesUtil;

public class UserMsgActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvUsername;
    private TextView tvAverageScore;
    private Button btnSingleMode;
    private Button btnBluetoothMode;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_msg);

        initViews();
        setupViews();
        setupListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvUsername = findViewById(R.id.tv_username);
        tvAverageScore = findViewById(R.id.tv_average_score);
        btnSingleMode = findViewById(R.id.btn_single_mode);
        btnBluetoothMode = findViewById(R.id.btn_bluetooth_mode);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupViews() {
        // 获取用户名
        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = SharedPreferencesUtil.getUsername(this);
        }

        if (username != null && !username.isEmpty()) {
            tvWelcome.setText(getString(R.string.welcome_back));
            tvUsername.setText(getString(R.string.username_label, username));

            // 计算并显示平均分
            double averageScore = SharedPreferencesUtil.calculateAverageScore(this);
            tvAverageScore.setText(String.format("平均得分: %.1f", averageScore));
        } else {
            // 如果没有用户名，返回登录界面
            goToLoginActivity();
        }
    }

    private void setupListeners() {
        // 单机模式按钮
        btnSingleMode.setOnClickListener(v -> {
            String username = SharedPreferencesUtil.getUsername(this);
            Intent intent = new Intent(this, SudokuActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // 蓝牙联机模式按钮
        btnBluetoothMode.setOnClickListener(v -> {
            Toast.makeText(this, R.string.bluetooth_developing, Toast.LENGTH_SHORT).show();
        });

        // 退出登录按钮
        btnLogout.setOnClickListener(v -> {
            // 清除用户名
            SharedPreferencesUtil.clearUsername(this);
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            // 返回登录界面
            goToLoginActivity();
        });
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // 关闭当前界面
    }
}