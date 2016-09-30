package com.ksyun.live.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.ksy.recordlib.service.core.KSYStreamer;
import com.ksy.recordlib.service.core.KSYStreamerConfig;
import com.ksy.recordlib.service.hardware.ksyfilter.KSYImageFilter;
import com.ksy.recordlib.service.stats.OnLogEventListener;
import com.ksy.recordlib.service.stats.StreamStatusEventHandler;
import com.ksy.recordlib.service.streamer.OnStatusListener;
import com.ksy.recordlib.service.streamer.RecorderConstants;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivityPip extends Activity {

    private static final String TAG = "CameraActivityPip";

    private ArrayList<HashMap<String,String>> videolist;
    private ArrayList<HashMap<String,String>> imagelist;
    private PopupWindow videopop;
    private ListView videopoplist;
    private PopupWindow pop;
    private ListView piplist;

    private GLSurfaceView mCameraPreview;

    private KSYStreamer mStreamer;

    private KSYMediaPlayer mKsyMediaPlayer;

    private Handler mHandler;

    Bitmap mPipBitmap;
    private final ButtonObserver mObserverButton = new ButtonObserver();

    private Chronometer chronometer;
    private View mDeleteView;
    private View mSwitchCameraView;
    private View mFlashView;
    private CheckBox enable_beauty;
    private TextView mPip;
    private TextView mPicturePip;
    private TextView mShootingText;
    private boolean recording = false;
    private boolean isFlashOpened = false;
    private boolean startAuto = false;
    private boolean landscape = false;
    private boolean printDebugInfo = false;
    private boolean mPipMode = false;
    private boolean mPicPipMode = false;
    private long lastPipClickTime = 0;
    private float pipVolume = 0.6f;
    private float bgmVolume = 0.6f;
    private String mUrl, mDebugInfo = "";
    private String mPipPath = "/sdcard/test.mp4";
    private String mPicture = null;
    private static final String START_STRING = "开始直播";
    private static final String STOP_STRING = "停止直播";
    private TextView mUrlTextView, mDebugInfoTextView;
    private volatile boolean mAcitivityResumed = false;
    private KSYStreamerConfig.ENCODE_METHOD encode_method = KSYStreamerConfig.ENCODE_METHOD.SOFTWARE;
    public final static String URL = "url";
    public final static String FRAME_RATE = "framerate";
    public final static String VIDEO_BITRATE = "video_bitrate";
    public final static String AUDIO_BITRATE = "audio_bitrate";
    public final static String VIDEO_RESOLUTION = "video_resolution";
    public final static String EncodeWithHEVC = "encode_with_hevc";
    public final static String LANDSCAPE = "landscape";
    public final static String ENCDODE_METHOD = "ENCDODE_METHOD";
    public final static String START_ATUO = "start_auto";
    public final static String MANUAL_FOCUS = "manual_focus";
    public static final String SHOW_DEBUGINFO = "SHOW_DEBUGINFO";

    Timer timer;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void startActivity(Context context, int fromType,
                                     String rtmpUrl, int frameRate, int videoBitrate, int audioBitrate,
                                     int videoResolution, boolean isLandscape, KSYStreamerConfig.ENCODE_METHOD encodeMethod, boolean startAuto,
                                     boolean manualFocus, boolean showDebugInfo,int audiohz) {
        Intent intent = new Intent(context, CameraActivityPip.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", fromType);
        intent.putExtra(URL, rtmpUrl);
        intent.putExtra(FRAME_RATE, frameRate);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(VIDEO_RESOLUTION, videoResolution);
        intent.putExtra(LANDSCAPE, isLandscape);
        intent.putExtra(ENCDODE_METHOD, encodeMethod);
        intent.putExtra(START_ATUO, startAuto);
        intent.putExtra(MANUAL_FOCUS, manualFocus);
        intent.putExtra(SHOW_DEBUGINFO, showDebugInfo);
        intent.putExtra("audiohz",audiohz);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.camera_activity_pip);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraPreview = (GLSurfaceView) findViewById(R.id.camera_preview);
        mUrlTextView = (TextView) findViewById(R.id.url);
        enable_beauty = (CheckBox) findViewById(R.id.click_to_switch_beauty);
        mPip = (TextView) findViewById(R.id.pip);
        mPip.setClickable(true);
        mPicturePip = (TextView) findViewById(R.id.picture_pip);
        mPicturePip.setClickable(true);

        KSYStreamerConfig.Builder builder = new KSYStreamerConfig.Builder();
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (msg != null && msg.obj != null) {
                    String content = msg.obj.toString();
                    switch (msg.what) {
                        case RecorderConstants.KSYVIDEO_ENCODED_FRAMES_FAILED:
                        case RecorderConstants.KSYVIDEO_CODEC_OPEN_FAILED:
                        case RecorderConstants.KSYVIDEO_CONNECT_FAILED:
                        case RecorderConstants.KSYVIDEO_DNS_PARSE_FAILED:
                        case RecorderConstants.KSYVIDEO_RTMP_PUBLISH_FAILED:
                        case RecorderConstants.KSYVIDEO_CONNECT_BREAK:
                        case RecorderConstants.KSYVIDEO_AUDIO_INIT_FAILED:
                        case RecorderConstants.KSYVIDEO_OPEN_CAMERA_FAIL:
                        case RecorderConstants.KSYVIDEO_CAMERA_PARAMS_ERROR:
                        case RecorderConstants.KSYVIDEO_AUDIO_START_FAILED:
                            Toast.makeText(CameraActivityPip.this, content,
                                    Toast.LENGTH_LONG).show();
                            chronometer.stop();
                            mShootingText.setText(START_STRING);
                            mShootingText.postInvalidate();
                            break;
                        case RecorderConstants.KSYVIDEO_OPEN_STREAM_SUCC:
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            // 开始计时
                            chronometer.start();
                            mShootingText.setText(STOP_STRING);
                            mShootingText.postInvalidate();
                            beginInfoUploadTimer();
                            break;
                        case RecorderConstants.KSYVIDEO_INIT_DONE:
                            if (mShootingText != null)
                                mShootingText.setEnabled(true);
                            Toast.makeText(getApplicationContext(), "初始化完成", Toast.LENGTH_SHORT).show();
                            checkPermission();
                            if (startAuto && mStreamer.startStream()) {
                                mShootingText.setText(STOP_STRING);
                                mShootingText.postInvalidate();
                                recording = true;
                            }
                            break;
                        default:
                            Toast.makeText(CameraActivityPip.this, content,
                                    Toast.LENGTH_SHORT).show();
                    }
                }
            }

        };

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String url = bundle.getString(URL);
            if (!TextUtils.isEmpty(url)) {
                builder.setmUrl(url);
                mUrl = url;
                mUrlTextView.setText(mUrl);
            }

            int audiohz = bundle.getInt("audiohz",0);
            builder.setSampleAudioRateInHz(audiohz);
            int frameRate = bundle.getInt(FRAME_RATE, 0);
            if (frameRate > 0) {
                builder.setFrameRate(frameRate);
            }

            int videoBitrate = bundle.getInt(VIDEO_BITRATE, 0);
            if (videoBitrate > 0) {
                //设置最高码率，即目标码率
                builder.setMaxAverageVideoBitrate(videoBitrate);
                //设置最低码率
                builder.setMinAverageVideoBitrate(videoBitrate * 2 / 8);
                //设置初始码率
                builder.setInitAverageVideoBitrate(videoBitrate * 5 / 8);
            }

            int audioBitrate = bundle.getInt(AUDIO_BITRATE, 0);
            if (audioBitrate > 0) {
                builder.setAudioBitrate(audioBitrate);
            }

            int videoResolution = bundle.getInt(VIDEO_RESOLUTION, 0);
            builder.setVideoResolution(videoResolution);

            encode_method = (KSYStreamerConfig.ENCODE_METHOD) bundle.get(ENCDODE_METHOD);
            builder.setEncodeMethod(encode_method);

            builder.setSampleAudioRateInHz(44100);
            builder.setEnableStreamStatModule(true);

            landscape = bundle.getBoolean(LANDSCAPE, false);
            builder.setDefaultLandscape(landscape);

            if (landscape) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            startAuto = bundle.getBoolean(START_ATUO, false);
            printDebugInfo = bundle.getBoolean(SHOW_DEBUGINFO, false);

            builder.setIsSlightBeauty(false);
            builder.setAutoAdjustBitrate(false);
        }

        //可以在这里做权限检查,若没有audio和camera权限,进一步引导用户做权限设置
        checkPermission();
        mStreamer = new KSYStreamer(this);
        mStreamer.setConfig(builder.build());
        mStreamer.setDisplayPreview(mCameraPreview);
        //老的状态回调机制
        mStreamer.setOnStatusListener(mOnErrorListener);

        //新的状态回调机制
        StreamStatusEventHandler.getInstance().addOnStatusErrorListener(mOnStatusErrorListener);
        StreamStatusEventHandler.getInstance().addOnStatusInfoListener(mOnStatusInfoListener);

        mStreamer.setOnLogListener(mOnLogListener);
        mStreamer.enableDebugLog(true);
        mStreamer.setBeautyFilter(RecorderConstants.FILTER_BEAUTY_DENOISE);

        enable_beauty.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (encode_method == KSYStreamerConfig.ENCODE_METHOD.SOFTWARE) {
                        mStreamer.setBeautyFilter(RecorderConstants.FILTER_BEAUTY_DENOISE);
                    } else {
                        showChooseFilter();
                    }
                } else {
                    if (encode_method == KSYStreamerConfig.ENCODE_METHOD.SOFTWARE) {
                        mStreamer.setBeautyFilter(RecorderConstants.FILTER_BEAUTY_DISABLE);
                    } else {
                        mStreamer.setBeautyFilter(new KSYImageFilter());
                    }
                }
            }
        });


        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.pippopwindow,null);
        pop = new PopupWindow(view, ActionBar.LayoutParams.MATCH_PARENT, 600,false);

        ColorDrawable cd = new ColorDrawable(0xa0000000);
        pop.setBackgroundDrawable(cd);
        pop.setFocusable(true);

        LayoutInflater videoinflater = LayoutInflater.from(this);
        View videoview = inflater.inflate(R.layout.videopopwindow,null);
        videopop = new PopupWindow(videoview, ActionBar.LayoutParams.MATCH_PARENT, 600,false);

        ColorDrawable videocd = new ColorDrawable(0xa0000000);
        videopop.setBackgroundDrawable(videocd);
        videopop.setFocusable(true);

        videolist= gitVideo();
        videopoplist = (ListView)videoview.findViewById(R.id.list_video);
        SimpleAdapter adapter2 = new SimpleAdapter(this,videolist,R.layout.listlayout,new String[]{"data"},new int[]{R.id.list_text} );
        videopoplist.setAdapter(adapter2);
        videopoplist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mPipPath = videolist.get(i).get("data");
                Toast.makeText(CameraActivityPip.this, mPicture, Toast.LENGTH_SHORT).show();
                pop.dismiss();
                showVideo();
            }
        });
        mPip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                long curTime = System.currentTimeMillis();
                if (curTime - lastPipClickTime < 1000) {
                    return;
                }
                lastPipClickTime = curTime;
                if (mPicPipMode) {
                    mStreamer.hidePipBitmap();
                    if (mPipBitmap != null) {
                        mPipBitmap.recycle();
                        mPipBitmap = null;
                    }
                    mPicPipMode = false;
                    mPicturePip.setText(CameraActivityPip.this.getResources().getString(R.string.picture_pip));
                    mPicturePip.postInvalidate();
                }

                if (!mPipMode) {
                    if(videopop.isShowing()){
                        videopop.dismiss();
                    }else{
                        videopop.showAtLocation(view.getRootView(), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                    }
                } else {
                    if (mKsyMediaPlayer != null) {
                        mKsyMediaPlayer.stop();
                        mKsyMediaPlayer.release();
                        mStreamer.stopPlayer();
                        mKsyMediaPlayer = null;
                    }
                    mPipMode = false;
                    mPip.setText(CameraActivityPip.this.getResources().getString(R.string.pip));
                    mPip.postInvalidate();
                }
            }
        });

        imagelist= getImages();
        piplist = (ListView)view.findViewById(R.id.list_pop);
        SimpleAdapter adapter = new SimpleAdapter(this,imagelist,R.layout.listlayout,new String[]{"data"},new int[]{R.id.list_text} );
        piplist.setAdapter(adapter);
        piplist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mPicture = imagelist.get(i).get("data");
                Toast.makeText(CameraActivityPip.this, mPicture, Toast.LENGTH_SHORT).show();
                pop.dismiss();
                showPicture();
            }
        });

        mPicturePip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                long curTime = System.currentTimeMillis();
                if (curTime - lastPipClickTime < 1000) {
                    return;
                }
                lastPipClickTime = curTime;
                if (mPipMode) {
                    if (mKsyMediaPlayer != null) {
                        mKsyMediaPlayer.stop();
                        mKsyMediaPlayer.release();
                        mStreamer.stopPlayer();
                        mKsyMediaPlayer = null;
                    }
                    mPipMode = false;
                    mPip.setText(CameraActivityPip.this.getResources().getString(R.string.pip));
                    mPip.postInvalidate();
                }
                if (!mPicPipMode) {
                    if(pop.isShowing()){
                        pop.dismiss();
                    }else{
                        pop.showAtLocation(view.getRootView(), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                    }

                } else {
                    mStreamer.hidePipBitmap();
                    if (mPipBitmap != null) {
                        mPipBitmap.recycle();
                        mPipBitmap = null;
                    }
                    mPicPipMode = false;
                    mPicturePip.setText(CameraActivityPip.this.getResources().getString(R.string.picture_pip));
                    mPicturePip.postInvalidate();
                }


            }
        });

        mShootingText = (TextView) findViewById(R.id.click_to_shoot);
        mShootingText.setClickable(true);
        mShootingText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (recording) {
                    if (mStreamer.stopStream()) {
                        chronometer.stop();
                        mShootingText.setText(START_STRING);
                        mShootingText.postInvalidate();
                        recording = false;
                    } else {
                        Log.e(TAG, "操作太频繁");
                    }
                } else {
                    checkPermission();
                    if (mStreamer.startStream()) {
                        mShootingText.setText(STOP_STRING);
                        mShootingText.postInvalidate();
                        recording = true;

                        mStreamer.setEnableReverb(true);
                        mStreamer.setReverbLevel(4);
                    } else {
                        Log.e(TAG, "操作太频繁");
                    }
                }

            }
        });
        if (startAuto) {
            mShootingText.setEnabled(false);
        }

        mDeleteView = findViewById(R.id.backoff);
        mDeleteView.setOnClickListener(mObserverButton);
        mDeleteView.setEnabled(true);

        mSwitchCameraView = findViewById(R.id.switch_cam);
        mSwitchCameraView.setOnClickListener(mObserverButton);
        mSwitchCameraView.setEnabled(true);

        mFlashView = findViewById(R.id.flash);
        mFlashView.setOnClickListener(mObserverButton);
        mFlashView.setEnabled(true);

        chronometer = (Chronometer) this.findViewById(R.id.chronometer);
        mDebugInfoTextView = (TextView) this.findViewById(R.id.debuginfo);

        for(HashMap<String,String> map:imagelist){
            Log.e("ImageList",map.get("data"));
        }
    }

    private void showVideo() {
        mKsyMediaPlayer = new KSYMediaPlayer.Builder(CameraActivityPip.this).build();
        mKsyMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mKsyMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mKsyMediaPlayer.setOnInfoListener(mOnInfoListener);
        mKsyMediaPlayer.setOnErrorListener(mOnPlayerErrorListener);
        mKsyMediaPlayer.setOnSeekCompleteListener(mOnSeekCompletedListener);
        mKsyMediaPlayer.setScreenOnWhilePlaying(true);
        mKsyMediaPlayer.setBufferTimeMax(5);
        mKsyMediaPlayer.setLooping(true);
        mKsyMediaPlayer.setVolume(pipVolume, pipVolume);
        mKsyMediaPlayer.setPlayerMute(0);
        mStreamer.setPipPlayer(mKsyMediaPlayer);
        mStreamer.setHeadsetPlugged(true);
        mStreamer.setPipLocation(0.6f, 0.6f, 0.4f, 0.4f);
        mStreamer.startPlayer(mPipPath);
        mPipMode = true;
        mPip.setText(CameraActivityPip.this.getResources().getString(R.string.stop_pip));
        mPip.postInvalidate();
    }

    public void showPicture(){

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                mPipBitmap = BitmapFactory.decodeFile(mPicture);
                mStreamer.showPipBitmap(mPipBitmap, 0.6f, 0.6f, 0.4f, 0.4f);
                mPicPipMode = true;
                mPicturePip.setText(CameraActivityPip.this.getResources().getString(R.string.stop_picture_pip));
                mPicturePip.postInvalidate();
            }
        });
    }

    public ArrayList<HashMap<String,String>> gitVideo(){

        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{"android.permission.READ_EXTERNAL_STORAGE"},//需要请求的所有权限，这是个数组String[]
                    1//请求码
            );
        }
        ArrayList<HashMap<String,String>> videolist = new ArrayList<HashMap<String,String>>();
        String[] mediaColumns = {MediaStore.Video.Media.DATA};
        Cursor cursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaColumns, null, null, null);

        if(cursor==null){
            Toast.makeText(CameraActivityPip.this, "没有找到任何文件", Toast.LENGTH_SHORT).show();
            return null;
        }

        if(cursor.moveToFirst()){
            do{
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("data",cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA)));
                videolist.add(map);
            }while(cursor.moveToNext());
        }
        return videolist;
    }


    public ArrayList<HashMap<String, String>> getImages() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        String[] projection = {MediaStore.Images.Media.DATA };
        String selection = MediaStore.Images.Media.MIME_TYPE + "=? or "+ MediaStore.Images.Media.MIME_TYPE + "=?";
        String[] selectionArgs = { "image/jpeg" , "image/png" };
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " desc";
        Cursor cursor = contentResolver.query(uri, projection, selection,
                selectionArgs, sortOrder);
        ArrayList<HashMap<String, String>> imageList = new ArrayList<HashMap<String, String>>();
        if (cursor != null) {
            HashMap<String, String> imageMap = null;
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                imageMap = new HashMap<String, String>();
                imageMap.put("data", cursor.getString(cursor
                        .getColumnIndex(MediaStore.Images.Media.DATA)));
                imageList.add(imageMap);
            }
            cursor.close();
        }
        return imageList;
    }


    private void beginInfoUploadTimer() {
        if (printDebugInfo && timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateDebugInfo();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugInfoTextView.setText(mDebugInfo);
                        }
                    });
                }
            }, 100, 3000);
        }
    }

    //update debug info
    private void updateDebugInfo() {
        if (mStreamer == null) return;
        mDebugInfo = String.format("RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%d DnsParseTime()=%d \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n" +
                        "CurrentBitrate=%f Version()=%s",
                mStreamer.getRtmpHostIP(), mStreamer.getDroppedFrameCount(),
                mStreamer.getConnectTime(), mStreamer.getDnsParseTime(),
                mStreamer.getUploadedKBytes(), mStreamer.getEncodedFrames(),
                mStreamer.getCurrentBitrate(), mStreamer.getVersion());
    }

    private void showChooseFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this).setTitle("请选择美颜滤镜").setSingleChoiceItems(
                new String[]{"BEAUTY_SOFT", "SKIN_WHITEN", "BEAUTY_ILLUSION", "DENOISE", "DEMOFILTER", "SPLIT_E/P_FILTER", "GROUP_FILTER"}, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < 4) {
                            mStreamer.setBeautyFilter(which + 16);
                        } else if (which == 4) {

                        } else if (which == 5) {
                            mStreamer.setBeautyFilter(new DEMOFILTER(), RecorderConstants.FILTER_USAGE_ENCODE);
                            mStreamer.setBeautyFilter(new DEMOFILTER2(), RecorderConstants.FILTER_USAGE_PREVIEW);
                        } else if (which == 6) {
                            mStreamer.setBeautyFilter(new GroupFilterDemo());
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        //可以在这里做权限检查,若没有audio和camera权限,进一步引导用户做权限设置
        checkPermission();
        if (mKsyMediaPlayer != null) {
            mKsyMediaPlayer.start();
        }
        mStreamer.onResume();
        mAcitivityResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mKsyMediaPlayer != null) {
            mKsyMediaPlayer.pause();
        }
        mStreamer.onPause();
        mAcitivityResumed = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                new AlertDialog.Builder(CameraActivityPip.this).setCancelable(true)
                        .setTitle("结束直播?")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {

                            }
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mStreamer.stopStream(true);
                                chronometer.stop();
                                recording = false;
                                CameraActivityPip.this.finish();
                            }
                        }).show();
                break;

            default:
                break;
        }
        return true;
    }

    //推流失败,需要退出重新启动推流,建议做log输出,方便快速定位问题
    public StreamStatusEventHandler.OnStatusErrorListener mOnStatusErrorListener = new StreamStatusEventHandler.OnStatusErrorListener() {
        @Override
        public void onError(int what, int arg1, int arg2, String msg) {
            switch (what) {
                case RecorderConstants.KSYVIDEO_ENCODED_FRAMES_FAILED:
                    //视频编码失败,发生在软解,会停止推流
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_ENCODED_FRAMES_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_CODEC_OPEN_FAILED:
                    //编码器初始化失败,发生在推流开始时
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CODEC_OPEN_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_CONNECT_FAILED:
                    //打开编码器的输入输出文件失败,发生在推流开始时,会停止推流
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CONNECT_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_DNS_PARSE_FAILED:
                    //打开编码器的输入输出文件失败,发生在推流开始时,会停止推流
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CONNECT_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_RTMP_PUBLISH_FAILED:
                    //打开编码器的输入输出文件失败,发生在推流开始时,会停止推流
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CONNECT_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_CONNECT_BREAK:
                    //写入一个音视频文件失败
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CONNECT_BREAK");
                    break;
                case RecorderConstants.KSYVIDEO_AUDIO_INIT_FAILED:
                    //软编,音频初始化失败
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_AUDIO_INIT_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_AUDIO_COVERT_FAILED:
                    //软编,音频转码失败
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_AUDIO_COVERT_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_OPEN_CAMERA_FAIL:
                    //openCamera失败
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_OPEN_CAMERA_FAIL");
                    break;
                case RecorderConstants.KSYVIDEO_CAMERA_PARAMS_ERROR:
                    //获取不到camera参数,android 6.0 以下没有camera权限时可能发生
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_CAMERA_PARAMS_ERROR");
                    break;
                case RecorderConstants.KSYVIDEO_AUDIO_START_FAILED:
                    //audio startRecord 失败
                    Log.e(TAG, "the streaming stopped because KSYVIDEO_AUDIO_START_FAILED");
                    break;
                default:
                    break;
            }

//            if (mHandler != null) {
//                mHandler.obtainMessage(what, msg).sendToTarget();
//            }
        }
    };

    public StreamStatusEventHandler.OnStatusInfoListener mOnStatusInfoListener = new StreamStatusEventHandler.OnStatusInfoListener() {
        @Override
        public void onInfo(int what, int arg1, int arg2, String msg) {
            switch (what) {
                case RecorderConstants.KSYVIDEO_OPEN_STREAM_SUCC:
                    //推流成功
                    Log.d("TAG", "KSYVIDEO_OPEN_STREAM_SUCC");
//                    mHandler.obtainMessage(what, "start stream succ")
//                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_FRAME_DATA_SEND_SLOW:
                    //网络状况不佳
//                    if (mHandler != null) {
//                        mHandler.obtainMessage(what, "network not good").sendToTarget();
//                    }
                    break;
                case RecorderConstants.KSYVIDEO_EST_BW_RAISE:
                    //码率上调
                    Log.d("TAG", "KSYVIDEO_EST_BW_RAISE");
                    break;
                case RecorderConstants.KSYVIDEO_EST_BW_DROP:
                    //码率下调
                    Log.d("TAG", "KSYVIDEO_EST_BW_DROP");
                    break;
                case RecorderConstants.KSYVIDEO_INIT_DONE:
                    //video preview init done
                    Log.d("TAG", "KSYVIDEO_INIT_DONE");
                    break;
                case RecorderConstants.KSYVIDEO_PIP_EXCEPTION:
                    Log.d("TAG", "KSYVIDEO_PIP_EXCEPTION");
//                    mHandler.obtainMessage(what, "pip exception")
//                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_RENDER_EXCEPTION:
                    Log.d("TAG", "KSYVIDEO_RENDER_EXCEPTION");
//                    mHandler.obtainMessage(what, "renderer exception")
//                            .sendToTarget();
                    break;
                default:
                    break;

            }
        }
    };

    public OnStatusListener mOnErrorListener = new OnStatusListener() {
        @Override
        public void onStatus(int what, int arg1, int arg2, String msg) {
            // msg may be null
            switch (what) {
                case RecorderConstants.KSYVIDEO_OPEN_STREAM_SUCC:
                    // 推流成功
                    Log.d("TAG", "KSYVIDEO_OPEN_STREAM_SUCC");
                    mHandler.obtainMessage(what, "start stream succ")
                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_ENCODED_FRAMES_FAILED:
                    //编码失败
                    Log.e(TAG, "---------KSYVIDEO_ENCODED_FRAMES_FAILED");
                    break;
                case RecorderConstants.KSYVIDEO_WLD_UPLOAD:
                    break;
                case RecorderConstants.KSYVIDEO_FRAME_DATA_SEND_SLOW:
                    //网络状况不佳
                    if (mHandler != null) {
                        mHandler.obtainMessage(what, "network not good").sendToTarget();
                    }
                    break;
                case RecorderConstants.KSYVIDEO_EST_BW_DROP:
                    //编码码率下降状态通知
                    break;
                case RecorderConstants.KSYVIDEO_EST_BW_RAISE:
                    //编码码率上升状态通知
                    break;
                case RecorderConstants.KSYVIDEO_AUDIO_INIT_FAILED:
                    Log.e("CameraActivity", "init audio failed");
                    //音频录制初始化失败回调
                    break;
                case RecorderConstants.KSYVIDEO_INIT_DONE:
                    mHandler.obtainMessage(what, "init done")
                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_PIP_EXCEPTION:
                    mHandler.obtainMessage(what, "pip exception")
                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_RENDER_EXCEPTION:
                    mHandler.obtainMessage(what, "renderer exception")
                            .sendToTarget();
                    break;
                case RecorderConstants.KSYVIDEO_AUDIO_START_FAILED:
                    Log.e("CameraActivity", "-------audio start failed");
                    break;
                case RecorderConstants.KSYVIDEO_CAMERA_PARAMS_ERROR:
                    Log.e("CameraActivity", "-------camera param is null");
                    break;
                case RecorderConstants.KSYVIDEO_OPEN_CAMERA_FAIL:
                    Log.e("CameraActivity", "-------open camera failed");
                    break;
                default:
                    if (msg != null) {
                        // 可以在这里处理断网重连的逻辑
                        if (TextUtils.isEmpty(mUrl)) {
                            mStreamer
                                    .updateUrl("rtmp://test.uplive.ksyun.com/live/androidtest");
                        } else {
                            mStreamer.updateUrl(mUrl);
                        }
                        if (!executorService.isShutdown()) {
                            executorService.submit(new Runnable() {

                                @Override
                                public void run() {
                                    boolean needReconnect = true;
                                    try {
                                        while (needReconnect) {
                                            Thread.sleep(3000);
                                            //只在Activity对用户可见时重连
                                            if (mAcitivityResumed) {
                                                if (mStreamer.startStream()) {
                                                    recording = true;
                                                    needReconnect = false;
                                                }
                                            }
                                        }
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                            });
                        }
                    }
                    if (mHandler != null) {
                        mHandler.obtainMessage(what, msg).sendToTarget();
                    }
            }
        }

    };

    private OnLogEventListener mOnLogListener = new OnLogEventListener() {
        @Override
        public void onLogEvent(StringBuffer singleLogContent) {
            Log.d(TAG, "***onLogEvent : " + singleLogContent.toString());
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        //新的状态回调机制
        StreamStatusEventHandler.getInstance().removeStatusErrorListener(mOnStatusErrorListener);
        StreamStatusEventHandler.getInstance().removeStatusInfoListener(mOnStatusInfoListener);

        if (mKsyMediaPlayer != null) {
            mKsyMediaPlayer.release();
            mKsyMediaPlayer = null;
        }
        mStreamer.onDestroy();
        executorService.shutdownNow();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (timer != null) {
            timer.cancel();
        }
    }

    private boolean clearState() {
        if (clearBackoff()) {
            return true;
        }
        return false;
    }

    private long lastClickTime = 0;

    private void onSwitchCamClick() {
        long curTime = System.currentTimeMillis();
        if (curTime - lastClickTime < 1000) {
            return;
        }
        lastClickTime = curTime;

        if (clearState()) {
            return;
        }
        mStreamer.switchCamera();

    }

    private void onFlashClick() {
        if (mStreamer.isFrontCamera())
            isFlashOpened = true;

        if (isFlashOpened) {
            mStreamer.toggleTorch(false);
            isFlashOpened = false;
        } else {
            mStreamer.toggleTorch(true);
            isFlashOpened = true;
        }
    }

    private boolean clearBackoff() {
        if (mDeleteView.isSelected()) {
            mDeleteView.setSelected(false);
            return true;
        }
        return false;
    }

    private void onBackoffClick() {

        new AlertDialog.Builder(CameraActivityPip.this).setCancelable(true)
                .setTitle("结束直播?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mStreamer.stopStream(true);
                        chronometer.stop();
                        recording = false;
                        CameraActivityPip.this.finish();
                    }
                }).show();
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.switch_cam:
                    onSwitchCamClick();
                    break;
                case R.id.backoff:
                    onBackoffClick();
                    break;
                case R.id.flash:
                    onFlashClick();
                    break;
                default:
                    break;
            }
        }
    }

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            long duration = mKsyMediaPlayer.getDuration();
            long progress = duration * percent / 100;
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompletedListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            Log.d(TAG, "onSeekComplete...............");
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {

        }
    };

    private IMediaPlayer.OnErrorListener mOnPlayerErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what) {
                case KSYMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    Log.e(TAG, "OnErrorListener, Error Unknown:" + what + ",extra:" + extra);
                    break;
                default:
                    Log.e(TAG, "OnErrorListener, Error:" + what + ",extra:" + extra);
            }

            return false;
        }
    };

    public IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            Log.d(TAG, "onInfo, what:" + i + ",extra:" + i1);
            return false;
        }
    };

    private boolean checkPermission() {
        try {
            int pRecordAudio = PermissionChecker.checkCallingOrSelfPermission(this, "android.permission.RECORD_AUDIO");
            int pCamera = PermissionChecker.checkCallingOrSelfPermission(this, "android.permission.CAMERA");

            if (pRecordAudio != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "do not have AudioRecord permission, please check");
                Toast.makeText(this, "do not have AudioRecord permission, please check", Toast.LENGTH_LONG).show();
                return false;
            }
            if (pCamera != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "do not have CAMERA permission, please check");
                Toast.makeText(this, "do not have CAMERA permission, please check", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

}
