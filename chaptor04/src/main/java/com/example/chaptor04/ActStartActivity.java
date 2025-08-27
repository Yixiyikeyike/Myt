package com.example.chaptor04;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ActStartActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ning";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"ActStartActivity onCreate");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_act_start);
        findViewById(R.id.btn_next).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, ActFinishActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"ActStartActivity onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"ActStartActivity onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"ActStartActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"ActStartActivity onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"ActStartActivity onDestroy");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"ActStartActivity onRestart");
    }
}