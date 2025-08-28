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
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import utils.SharedPreferencesUtil;

public class SingleSucceedActivity extends AppCompatActivity {

    private TextView tvStars;
    private TextView tvTime;
    private TextView tvScore;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_succeed);

        initViews();
        setupViews();
        setupListeners();
    }

    private void initViews() {
        tvStars = findViewById(R.id.tv_stars);
        tvTime = findViewById(R.id.tv_time);
        tvScore = findViewById(R.id.tv_score);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupViews() {
        // 获取传递的数据
        Intent intent = getIntent();
        int stars = intent.getIntExtra("stars", 0);
        String time = intent.getStringExtra("time");

        // 计算分数 (星星数 * 100 - 秒数，确保分数总是正数)
        long seconds = convertTimeToSeconds(time);
        double score = stars * 100 - seconds;

        // 保存游戏记录
        SharedPreferencesUtil.saveGameRecord(this,
                new SharedPreferencesUtil.GameRecord(stars, time, score));

        // 显示数据
        tvStars.setText(String.format("获得星星: ☆%d", stars));
        tvTime.setText(String.format("完成时间: %s", time));
        tvScore.setText(String.format("本次得分: %.1f", score));
    }

    private long convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        long minutes = Long.parseLong(parts[0]);
        long seconds = Long.parseLong(parts[1]);
        return minutes * 60 + seconds;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            // 返回UserMsgActivity
            Intent intent = new Intent(this, UserMsgActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}