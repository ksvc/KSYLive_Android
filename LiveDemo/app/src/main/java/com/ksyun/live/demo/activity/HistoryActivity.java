package com.ksyun.live.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.ksyun.live.demo.R;
import com.ksyun.live.demo.model.NetDbAdapter;
import com.ksyun.live.demo.util.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by liubohua on 16/7/20.
 */
public class HistoryActivity extends Activity{
    private ListView hislist;
    private ArrayList<Map<String,String>> listurl;
    private Cursor cursor;
    private NetDbAdapter NetDb;

    private SharedPreferences settings;
    private String choosevr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        listurl = new ArrayList<Map<String,String>>();
        settings = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        choosevr = settings.getString("choose_vr","信息为空");

        hislist = (ListView) findViewById(R.id.list_history);
        NetDb = new NetDbAdapter(HistoryActivity.this);
        NetDb.open();
        cursor = NetDb.getAllData();
        cursor.moveToFirst();
        if(cursor.getCount()>0){
            Map<String,String> map = new HashMap<String,String>();
            map.put("url", cursor.getString(cursor.getColumnIndex(NetDbAdapter.KEY_PATH)));
            listurl.add(map);
        }
        while(cursor.moveToNext()){
            Map<String,String> map = new HashMap<String,String>();
            map.put("url", cursor.getString(cursor.getColumnIndex(NetDbAdapter.KEY_PATH)));
            listurl.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this,listurl,R.layout.list_history,new String[]{"url"},new int[]{R.id.list_history_txt});

        hislist.setAdapter(adapter);
        hislist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String path = listurl.get(i).get("url");
                if (choosevr.equals(Settings.VRON)){
                    Intent intent  = new Intent(HistoryActivity.this,TestVideoActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);
                }else{
                    Intent intent = new Intent(HistoryActivity.this,VideoPlayerActivity.class);
                    intent.putExtra("path",path);
                    startActivity(intent);
                }

            }
        });

    }
}
