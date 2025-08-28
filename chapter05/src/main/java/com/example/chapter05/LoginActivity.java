package com.example.chapter05;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chapter05.R;
import utils.SharedPreferencesUtil;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 检查是否已有用户名
        String savedUsername = SharedPreferencesUtil.getUsername(this);
        if (savedUsername != null && !savedUsername.isEmpty()) {
            startUserMsgActivity(savedUsername);
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "用户名不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (username.length() > 10) {
                Toast.makeText(this, "用户名不能超过10个字符", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存用户名
            SharedPreferencesUtil.saveUsername(this, username);

            // 启动用户信息界面
            startUserMsgActivity(username);
        });
    }

    private void startUserMsgActivity(String username) {
        Intent intent = new Intent(this, UserMsgActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish(); // 关闭登录界面
    }
}