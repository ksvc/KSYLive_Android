package com.ksyun.live.demo.player.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ksyun.live.demo.R;
import com.ksyun.live.demo.player.model.GetList;
import com.ksyun.live.demo.player.util.Settings;
import com.ksyun.live.demo.player.util.Video;

import java.util.ArrayList;


public class LocalFragment extends android.app.Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final int UPDATE = 1;
    private ListView listView;
    private JieVideoListViewAdapter madapter;
    private ArrayList<Video> showlistVideos;
    private SwipeRefreshLayout swipeLayout;
    public static Handler mHandler;
    private GetList getList;
    private boolean updateok = false;
    private SharedPreferences settings;

    public LocalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        showlistVideos = new ArrayList<Video>();
        getList = new GetList();
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what){
                    case UPDATE:
                        if(updateok){
                            updateList();
                            swipeLayout.setRefreshing(false);
                            Toast.makeText(getActivity(), "更新成功", Toast.LENGTH_SHORT).show();
                        }else{
                            swipeLayout.setRefreshing(false);
                            Toast.makeText(getActivity(), "更新失败,请等待加载完毕", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        if(msg.obj instanceof ArrayList) {
                            showlistVideos.clear();
                            showlistVideos.addAll((ArrayList<Video>)msg.obj);
                            updateList();
                        }
                        break;
                    case 3:
                        if(msg.obj instanceof ArrayList) {
                            updateok = true;
                            showlistVideos.clear();
                            showlistVideos.addAll((ArrayList<Video>)msg.obj);
                        }
                }
            }
        };

        View view = inflater.inflate(R.layout.fragment_local, container, false);

        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        listView = (ListView)view.findViewById(R.id.list_local_frag);
        swipeLayout.setOnRefreshListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Video v = showlistVideos.get(position);

                String chooseview;
                chooseview = settings.getString("choose_view","undefind");
               if(chooseview.equals(Settings.USEKSYTEXTURE)){
                    Intent intent = new Intent(getActivity(),TextureVideoActivity.class);
                    intent.putExtra("path",v.getPath());
                    startActivity(intent);
               }
               else if (chooseview.equals(Settings.USEKGLRENDER)){
                   Intent intent = new Intent(getActivity(),TextureViewMediaActivity.class);
                   intent.putExtra("path",v.getPath());
                   startActivity(intent);
               }
               else if (chooseview.equals(Settings.USEKSYVIVEW)){
                   Intent intent = new Intent(getActivity(),KSYSurfaceActivity.class);
                   intent.putExtra("path",v.getPath());
                   startActivity(intent);
               }
               else{
                    Intent intent = new Intent(getActivity(), SurfaceActivity.class);
                    intent.putExtra("path",v.getPath());
                    startActivity(intent);
                }
            }
        });

        getList.startThread(showlistVideos, Environment.getExternalStorageDirectory());
        return view;
    }
    public void updateList(){
        madapter = new JieVideoListViewAdapter(getActivity(),showlistVideos);
        listView.setAdapter(madapter);
    }

    public void setSettings( SharedPreferences set){
        settings = set;
    }

    @Override
    public void onRefresh() {
        Message msg = new Message();
        msg.what = UPDATE;
        mHandler.sendMessageDelayed(msg,3000);
    }
}

