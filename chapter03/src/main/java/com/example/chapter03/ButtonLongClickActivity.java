package com.example.chapter03;

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

public class ButtonLongClickActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_button_long_click);

        TextView tv_result = findViewById(R.id.tv_result);
        Button btn_long_click_single = findViewById(R.id.btn_long_click_single);
        btn_long_click_single.setOnLongClickListener(v -> {
            String desc = String.format("%s 点击了按钮: %s", DateUtil.getNowTime(),((Button) v).getText());
            tv_result.setText(desc);
            return true;
        });
    }
}