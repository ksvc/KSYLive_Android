package com.ksyun.live.demo.player.activity;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ksyun.live.demo.R;

public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private LocalFragment localFragment;

    private final static int SETTINGREQUEST = 0;

    private Button mediaNet;
    private Button mediaSetting;
    private Button mediaHistory;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        setContentView(R.layout.activity_player);

        setActionBarLayout(R.layout.media_actionbar, this);

        permissionCheck();

        setDefaultFragment();

    }

    private void permissionCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int readPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                readPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e("Player", "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        1);
            }
        }
    }


    public void setActionBarLayout(int layoutId, Context mContext) {
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            LayoutInflater inflator = (LayoutInflater) this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflator.inflate(layoutId, new LinearLayout(mContext), false);
            ActionBar.LayoutParams layout = new ActionBar.LayoutParams(
                    android.support.v7.app.ActionBar.LayoutParams.MATCH_PARENT, android.support.v7.app.ActionBar.LayoutParams.MATCH_PARENT);
            actionBar.setCustomView(v, layout);
            mediaNet = (Button) findViewById(R.id.media_network);
            mediaHistory = (Button) findViewById(R.id.media_history);
            mediaSetting = (Button) findViewById(R.id.media_setting);
            mediaNet.setOnClickListener(this);
            mediaSetting.setOnClickListener(this);
            mediaHistory.setOnClickListener(this);
        } else {
            Toast.makeText(PlayerActivity.this, "ActionBar不存在", Toast.LENGTH_SHORT).show();
        }

    }

    private void setDefaultFragment() {

        FragmentManager fm = this.getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        localFragment = new LocalFragment();
        localFragment.setSettings(getSharedPreferences("SETTINGS", Context.MODE_PRIVATE));
        transaction.replace(R.id.contentFrame, localFragment);
        transaction.commit();
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.media_network:
                Toast.makeText(PlayerActivity.this, "mediaNet", Toast.LENGTH_SHORT).show();
                intent = new Intent(PlayerActivity.this, NetMediaActivty.class);
                startActivity(intent);
                break;
            case R.id.media_history:
                Intent intent2 = new Intent(this, HistoryActivity.class);
                startActivity(intent2);
                Toast.makeText(PlayerActivity.this, "mediaHistory", Toast.LENGTH_SHORT).show();
                break;
            case R.id.media_setting:
                intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent,SETTINGREQUEST);
                Toast.makeText(PlayerActivity.this, "mediaSetting", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SETTINGREQUEST:
                if (resultCode==1){
                    PlayerActivity.this.recreate();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        localFragment.onBackPressed();
    }
}
