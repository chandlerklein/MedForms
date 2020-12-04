package com.myscript.iink.getstarted;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        init();
    }

    private void init() {
        Button btnDocsBack = findViewById(R.id.btnDocsBack);
        btnDocsBack.setOnClickListener(v -> {
            Intent intent = new Intent(AboutActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }
}