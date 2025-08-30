package com.example.chapter05;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    private TextView tvHighScore;
    private TextView tvNewRecord;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_succeed);

        // 初始化视图
        tvScore = findViewById(R.id.tv_score);
        tvHighScore = findViewById(R.id.tv_high_score);
        tvNewRecord = findViewById(R.id.tv_new_record);

        // 获取游戏数据
        int stars = getIntent().getIntExtra("stars", 0);
        String time = getIntent().getStringExtra("time");

        // 计算本次得分 (简单示例: 星星数 * 1000 - 秒数 * 10)
        int score = calculateScore(stars, time);

        // 获取当前最高分
        int highScore = SharedPreferencesUtil.getHighScore(this);

        // 检查并更新最高分
        boolean isNewRecord = SharedPreferencesUtil.checkAndUpdateHighScore(this, score);

        // 显示分数
        tvScore.setText("本次得分: " + score);
        tvHighScore.setText("最高分: " + Math.max(score, highScore)/10);

        // 如果是新纪录，显示提示
        if (isNewRecord) {
            tvNewRecord.setVisibility(View.VISIBLE);
        }

        initViews();
        setupViews();
        setupListeners();
    }
    private int calculateScore(int stars, String time) {
        // 简单评分算法: 星星数 * 1000 - 秒数 * 10
        int seconds = (int) convertTimeToSeconds(time);
        return stars * 1000 - seconds * 10;
    }

    private long convertTimeToSeconds(String time) {
        String[] parts = time.split(":");
        long minutes = Long.parseLong(parts[0]);
        long seconds = Long.parseLong(parts[1]);
        return minutes * 60 + seconds;
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