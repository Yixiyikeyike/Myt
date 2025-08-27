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

public class ButtonClickActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_button_click);
        Button btn_click_single = findViewById(R.id.btn_click_single);
        tv_result = findViewById(R.id.tv_result);
        btn_click_single.setOnClickListener(new MyOnClickListener(tv_result));

        Button btn_click_public = findViewById(R.id.btn_click_public);
        btn_click_public.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.btn_click_public){
            String desc = String.format("%s 点击了按钮: %s", DateUtil.getNowTime(),((Button) v).getText());
            tv_result.setText(desc);
        }
    }

    static class MyOnClickListener implements View.OnClickListener{

        private final TextView tv_result;

        public MyOnClickListener(TextView tv_result) {
            this.tv_result = tv_result;
        }

        @Override
        public void onClick(View view) {
            String desc = String.format("%s 点击了按钮: %s", DateUtil.getNowTime(),((Button) view).getText());
            tv_result.setText(desc);
        }
    }
}