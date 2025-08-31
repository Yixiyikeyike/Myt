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

import androidx.appcompat.app.AppCompatActivity;

public class DifficultySelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_difficulty_selection);

        Button btnEasy = findViewById(R.id.btn_easy);
        Button btnMedium = findViewById(R.id.btn_medium);
        Button btnHard = findViewById(R.id.btn_hard);

        btnEasy.setOnClickListener(v -> startGameWithDifficulty(1)); // 1表示简单难度
        btnMedium.setOnClickListener(v -> startGameWithDifficulty(2)); // 2表示中等难度
        btnHard.setOnClickListener(v -> startGameWithDifficulty(3)); // 3表示困难难度
    }

    private void startGameWithDifficulty(int difficulty) {
        Intent intent = new Intent(this, SudokuActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("username", getIntent().getStringExtra("username"));
        startActivity(intent);
        finish();
    }
}