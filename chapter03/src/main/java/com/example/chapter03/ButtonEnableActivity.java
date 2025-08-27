package com.example.chapter03;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chapter03.utils.DateUtil;

public class ButtonEnableActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_enable;
    private Button btn_disable;
    private Button btn_test;
    private TextView tv_result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_button_enable);
        btn_enable = findViewById(R.id.btn_enable);
        btn_disable = findViewById(R.id.btn_disable);
        btn_test = findViewById(R.id.btn_test);
        tv_result = findViewById(R.id.tv_result);
        btn_enable.setOnClickListener(this);
        btn_disable.setOnClickListener(this);
        btn_test.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_enable){
            btn_test.setEnabled(true);
            btn_test.setTextColor(Color.BLACK);
        }
        if(v.getId() == R.id.btn_disable){
            btn_test.setEnabled(false);
            btn_test.setTextColor(Color.GRAY);
        }
        if(v.getId() == R.id.btn_test){
            String desc = String.format("%s 点击了按钮: %s", DateUtil.getNowTime(),((Button) v).getText());
            tv_result.setText(desc);
        }
    }
}