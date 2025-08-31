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

public class BluetoothMatchDrawActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_match_draw);

        // 获取传递过来的数据
        Intent intent = getIntent();
        int filledCells = intent.getIntExtra("filledCells", 0);

        TextView tvResult = findViewById(R.id.tv_result);
        tvResult.setText("平局!\n双方都填了 " + filledCells + " 个格子");

        Button btnReturn = findViewById(R.id.btn_return);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 返回UserMsgActivity并清除活动栈
                Intent intent = new Intent(BluetoothMatchDrawActivity.this, UserMsgActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 禁用返回键，强制用户点击返回按钮
        // super.onBackPressed();
    }
}