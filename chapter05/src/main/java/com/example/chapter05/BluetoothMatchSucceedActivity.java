package com.example.chapter05;

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

public class BluetoothMatchSucceedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_match_succeed);

        // 获取传递过来的数据
        Intent intent = getIntent();
        int filledCells = intent.getIntExtra("filledCells", 0);
        int stars = intent.getIntExtra("stars", 3);
        boolean isHost = intent.getBooleanExtra("isHost", false);
        String time = intent.getStringExtra("time");

        TextView tvProgress = findViewById(R.id.tv_progress);
        TextView tvStars = findViewById(R.id.tv_stars);

        // 设置显示内容
        tvProgress.setText(String.format("完成格子: %d", filledCells));

        // 根据星星数设置星星显示
        String starsText = "获得星星: ";
        for (int i = 0; i < stars; i++) {
            starsText += "☆";
        }
        tvStars.setText(starsText);

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 返回UserMsgActivity并清除活动栈
                Intent intent = new Intent(BluetoothMatchSucceedActivity.this, UserMsgActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
        // 在onCreate方法中添加：
        int opponentFilledCells = intent.getIntExtra("opponentFilledCells", 0);
        TextView tvComparison = findViewById(R.id.tv_comparison);
        tvComparison.setText(String.format("你填了 %d 格\n对手填了 %d 格", filledCells, opponentFilledCells));
    }

//    @Override
//    public void onBackPressed() {
//        // 禁用返回键，强制用户点击返回按钮
//        // super.onBackPressed();
//    }
}