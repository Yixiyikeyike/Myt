package com.example.chaptor04;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chaptor04.util.DateUtil;

public class ActResponseActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String mResponse = "否";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_act_response);

        TextView tv_request = findViewById(R.id.tv_request);

        // 从上一个页面中取得包裹，添加空值检查
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String request_time = bundle.getString("request_time");
            String request_content = bundle.getString("request_content");
            String desc = String.format("收到请求消息：\n请求时间为%s，\n请求内容为%s", request_time, request_content);
            // 把请求消息显示在文本上
            tv_request.setText(desc);
        } else {
            tv_request.setText("未收到请求数据");
            Toast.makeText(this, "未收到请求数据", Toast.LENGTH_SHORT).show();
        }

        findViewById(R.id.btn_response).setOnClickListener(this);

        TextView tv_response = findViewById(R.id.tv_response);
        tv_response.setText("待返回的消息为：" + mResponse);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        // 修复：使用正确的键名（与 ActRequestActivity 中的期望一致）
        bundle.putString("response_time", DateUtil.getNowTime());
        bundle.putString("response_content", mResponse);
        // 修复：将 Bundle 放入 Intent 中
        intent.putExtras(bundle);
        // 携带意图，返回上一个页面
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}