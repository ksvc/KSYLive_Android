package com.ksyun.live.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ksyun.live.demo.activity.PlayerActivity;
import com.tencent.bugly.crashreport.CrashReport;


public class MainActivity extends AppCompatActivity {

    private Button btn_stream;
    private Button btn_player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CrashReport.initCrashReport(getApplicationContext(), "900040861", true);
        btn_player = (Button)findViewById(R.id.btn_player);
        btn_stream = (Button)findViewById(R.id.btn_stream);

        btn_stream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,StreamActivity.class);
                startActivity(intent);
            }
        });
        btn_player.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,PlayerActivity.class);
                startActivity(intent);
            }
        });
    }
}
