package com.example.chapter05;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import utils.SharedPreferencesUtil;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import utils.SharedPreferencesUtil;

public class SingleFailedActivity extends AppCompatActivity {

    private Button btnRetry;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_failed);

        // 记录失败的游戏 (得分为0)
        SharedPreferencesUtil.saveGameRecord(this,
                new SharedPreferencesUtil.GameRecord(0, "00:00", 0));

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnRetry = findViewById(R.id.btn_retry);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupListeners() {
        // 再试一次按钮
        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(this, SudokuActivity.class);
            startActivity(intent);
            finish();
        });

        // 返回主菜单按钮
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserMsgActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}
