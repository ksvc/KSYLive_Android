package com.ksyun.live.demo.player.activity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.live.demo.R;
import com.ksyun.live.demo.player.util.QosObject;
import com.ksyun.live.demo.player.util.QosThread;
import com.ksyun.live.demo.player.util.Settings;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaMeta;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYVideoView;
import com.ksyun.media.player.misc.KSYQosInfo;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

public class KSYSurfaceActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "SurfaceActivity";

    public static final int UPDATE_SEEKBAR = 0;
    public static final int HIDDEN_SEEKBAR = 1;
    public static final int UPDATE_QOSMESS  = 2;
    public static final int UPADTE_QOSVIEW = 3;

    private SharedPreferences settings;
    private String choosedecode;
    private String choosedebug;

    private Context mContext;
    private KSYVideoView ksyMediaPlayer;
    private QosThread mQosThread;

    private Handler mHandler;
    private TextView mLoadText;
    private TextView mCpu;
    private TextView mMemInfo;
    private TextView mVideoResolution;
    private TextView mVideoBitrate;
    private TextView mFrameRate;
    private TextView mVideoBufferTime;
    private TextView mAudioBufferTime;
    private TextView mServerIp;
    private TextView mSdkVersion;
    private TextView mDNSTime;
    private TextView mHttpConnectionTime;

    private RelativeLayout toppanel;
    private Button reload;
    private Button screen;
    private Button rotate;
    private Button mute;

    private Button mPlayerScaleVideo;

    private boolean mPlayerPanelShow = false;
    private boolean mPause = false;
    private boolean mmute = false;

    private long mStartTime = 0;
    private long mPauseStartTime = 0;
    private long mPausedTime = 0;

    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private int mVideoScaleIndex = 0;
    boolean useHwCodec = false;

    private Timer timer = null;
    private TimerTask timerTask= null;
    private long bits;
    private KSYQosInfo info;
    private String cpuUsage;
    private int pss;
    private int rotatenum = 0;


    private String mDataSource;

    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            Log.e(TAG,"OnPreparedListener onPrepared");
            mVideoWidth = ksyMediaPlayer.getVideoWidth();
            mVideoHeight = ksyMediaPlayer.getVideoHeight();

            // Set Video Scaling Mode
            ksyMediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
            //start player
            ksyMediaPlayer.start();

            //set progress
            setVideoProgress(0);

            if (mQosThread != null && !mQosThread.isAlive())
                mQosThread.start();


            if(ksyMediaPlayer.getServerAddress() != null)
                mServerIp.setText("ServerIP: "+ ksyMediaPlayer.getServerAddress());

            //  get meta data
            Bundle bundle = ksyMediaPlayer.getMediaMeta();
            KSYMediaMeta meta = KSYMediaMeta.parse(bundle);
            if (meta != null)
            {
                if (meta.mHttpConnectTime > 0) {
                    double http_connection_time = Double.valueOf(meta.mHttpConnectTime);
                    mHttpConnectionTime.setText("HTTP Connection Time: " + (int)http_connection_time);
                }

                int dns_time = meta.mAnalyzeDnsTime;
                if (dns_time > 0) {
                    mDNSTime.setText("DNS time: " + dns_time);
                }
            }

            mSdkVersion.setText("SDK version: " + ksyMediaPlayer.getVersion());

            mVideoResolution.setText("Resolution:" + ksyMediaPlayer.getVideoWidth() + "x" + ksyMediaPlayer.getVideoHeight());

            mStartTime = System.currentTimeMillis();
            choosedebug = settings.getString("choose_debug","信息为空");
            if(choosedebug.isEmpty() || choosedebug.equals(Settings.DEBUGOFF)){
                Log.e("VideoPlayer","关闭");
                mSdkVersion.setVisibility(View.GONE);
                mVideoResolution.setVisibility(View.GONE);
                mFrameRate.setVisibility(View.GONE);
                mVideoBitrate.setVisibility(View.GONE);
                mLoadText.setVisibility(View.GONE);
                mCpu.setVisibility(View.GONE);
                mMemInfo.setVisibility(View.GONE);
                mVideoBufferTime.setVisibility(View.GONE);
                mAudioBufferTime.setVisibility(View.GONE);
                mServerIp.setVisibility(View.GONE);
                mDNSTime.setVisibility(View.GONE);
                mHttpConnectionTime.setVisibility(View.GONE);

            }else{
                Log.e("VideoPlayer","开启");

                mSdkVersion.setVisibility(View.VISIBLE);
                mVideoResolution.setVisibility(View.VISIBLE);
                mFrameRate.setVisibility(View.VISIBLE);
                mVideoBitrate.setVisibility(View.VISIBLE);
                mLoadText.setVisibility(View.VISIBLE);
                mCpu.setVisibility(View.VISIBLE);
                mMemInfo.setVisibility(View.VISIBLE);
                mVideoBufferTime.setVisibility(View.VISIBLE);
                mAudioBufferTime.setVisibility(View.VISIBLE);
                mServerIp.setVisibility(View.VISIBLE);
                mDNSTime.setVisibility(View.VISIBLE);
                mHttpConnectionTime.setVisibility(View.VISIBLE);
            }
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            long duration = ksyMediaPlayer.getDuration();
            long progress = duration * percent/100;
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangeListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            if(mVideoWidth > 0 && mVideoHeight > 0) {
                if(width != mVideoWidth || height != mVideoHeight) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    if(ksyMediaPlayer != null)
                        ksyMediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                }
            }
        }
    };

    private IMediaPlayer.OnSeekCompleteListener mOnSeekCompletedListener = new IMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            Log.e(TAG, "onSeekComplete...............");
        }
    };

    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            Log.e(TAG, "onCompletion...............");
            Toast.makeText(mContext, "OnCompletionListener, play complete.", Toast.LENGTH_LONG).show();
            videoPlayEnd();
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what)
            {
                case KSYMediaPlayer.MEDIA_ERROR_UNKNOWN:
                    Log.e(TAG, "OnErrorListener, Error Unknown:" + what + ",extra:" + extra);
                    break;
                default:
                    Log.e(TAG, "OnErrorListener, Error:" + what + ",extra:" + extra);
            }

            videoPlayEnd();

            return false;
        }
    };

    public IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            switch (i) {
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_START:
                    Log.d(TAG, "Buffering Start.");
                    break;
                case KSYMediaPlayer.MEDIA_INFO_BUFFERING_END:
                    Log.d(TAG, "Buffering End.");
                    break;
                case KSYMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                    Toast.makeText(mContext, "Audio Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                    Toast.makeText(mContext, "Video Rendering Start", Toast.LENGTH_SHORT).show();
                    break;
                case KSYMediaPlayer.MEDIA_INFO_SUGGEST_RELOAD:
                    // Player find a new stream(video or audio), and we could reload the video.
                    if(ksyMediaPlayer != null)
                        ksyMediaPlayer.reload(mDataSource, false);
                    break;
                case KSYMediaPlayer.MEDIA_INFO_RELOADED:
                    Toast.makeText(mContext, "Succeed to reload video.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Succeed to reload video.");
                    return false;
            }
            return false;
        }
    };

    private View.OnClickListener mVideoScaleButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int mode = mVideoScaleIndex % 2;
            mVideoScaleIndex++;
            mHandler.removeMessages(HIDDEN_SEEKBAR);
            Message msg = new Message();
            msg.what = HIDDEN_SEEKBAR;
            mHandler.sendMessageDelayed(msg,3000);
            if(ksyMediaPlayer != null) {
                if(mode == 1)
                    ksyMediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                else
                    ksyMediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this.getApplicationContext();
        useHwCodec = getIntent().getBooleanExtra("HWCodec",false);

        setContentView(R.layout.ksy_videoview_layout);

        mLoadText = (TextView) findViewById(R.id.loading_text);
        mCpu = (TextView) findViewById(R.id.player_cpu);
        mMemInfo = (TextView) findViewById(R.id.player_mem);
        mVideoResolution = (TextView) findViewById(R.id.player_re);
        mVideoBitrate = (TextView) findViewById(R.id.player_br);
        mFrameRate = (TextView) findViewById(R.id.player_fr);
        mVideoBufferTime = (TextView) findViewById(R.id.player_video_time);
        mAudioBufferTime = (TextView) findViewById(R.id.player_audio_time);
        mServerIp = (TextView) findViewById(R.id.player_ip);
        mSdkVersion = (TextView) findViewById(R.id.player_sdk_version);
        mDNSTime = (TextView) findViewById(R.id.player_dns_time);
        mHttpConnectionTime = (TextView) findViewById(R.id.player_http_connection_time);

        toppanel = (RelativeLayout)findViewById(R.id.topPanel_player);
        reload = (Button)findViewById(R.id.player_reload);
        rotate = (Button)findViewById(R.id.btn_rotate_player);
        screen = (Button)findViewById(R.id.btn_screen_player);
        mute = (Button)findViewById(R.id.btn_mute_player);
        reload.setOnClickListener(this);
        rotate.setOnClickListener(this);
        screen.setOnClickListener(this);
        mute.setOnClickListener(this);


        mPlayerScaleVideo = (Button) findViewById(R.id.player_scale);
        mPlayerScaleVideo.setOnClickListener(mVideoScaleButton);

        ksyMediaPlayer = (KSYVideoView) findViewById(R.id.ksy_videoview);
        MediaController controller = (MediaController) findViewById(R.id.controller);
        ksyMediaPlayer.setMediaController(controller);

        ksyMediaPlayer.setOnTouchListener(mTouchListener);
        ksyMediaPlayer.setKeepScreenOn(true);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);


        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UPDATE_SEEKBAR:
                        setVideoProgress(0);
                        break;

                    case UPDATE_QOSMESS:
                        if(msg.obj instanceof QosObject) {
                            updateQosInfo((QosObject)msg.obj);
                        }
                        break;
                    case UPADTE_QOSVIEW:
                        updateQosView();
                        break;
                }
            }
        };

        if(timerTask ==null){
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try{
                        Message message = new Message();
                        message.what = SurfaceActivity.UPADTE_QOSVIEW;
                        if(mHandler != null && message!=null){
                            mHandler.sendMessage(message);
                        }
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }
            };
        }

        if(timer == null){
            timer = new Timer(true);
        }

        timer.schedule(timerTask,2000,5000);

        mQosThread = new QosThread(mContext, mHandler);

        mDataSource = getIntent().getStringExtra("path");

        ksyMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        ksyMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        ksyMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        ksyMediaPlayer.setOnInfoListener(mOnInfoListener);
        ksyMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangeListener);
        ksyMediaPlayer.setOnErrorListener(mOnErrorListener);
        ksyMediaPlayer.setOnSeekCompleteListener(mOnSeekCompletedListener);
        ksyMediaPlayer.setScreenOnWhilePlaying(true);
        ksyMediaPlayer.setBufferTimeMax(3);
        ksyMediaPlayer.setTimeout(5, 30);
        settings = getSharedPreferences("SETTINGS",Context.MODE_PRIVATE);
        choosedecode = settings.getString("choose_decode","undefind");


        if(choosedecode.equals(Settings.USEHARD)){
            useHwCodec = true;
        }else{
            useHwCodec = false;
        }

        if (useHwCodec) {
            //硬解264&265
            Log.e(TAG, "Hardware !!!!!!!!");
            ksyMediaPlayer.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_AUTO);
        }

        try {
            ksyMediaPlayer.setDataSource(mDataSource);
            ksyMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(ksyMediaPlayer != null)
        {
            ksyMediaPlayer.pause();
            mPause = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(ksyMediaPlayer != null)
        {
            ksyMediaPlayer.start();
            mPause = false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            videoPlayEnd();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    // Maybe we could support gesture detect
    private void dealTouchEvent(View view, MotionEvent event) {
        mPlayerPanelShow = !mPlayerPanelShow;

        if(mPlayerPanelShow) {
            toppanel.setVisibility(View.VISIBLE);

            Message msg = new Message();
            msg.what = HIDDEN_SEEKBAR;
        }else{
            toppanel.setVisibility(View.GONE);
        }
    }

    public int setVideoProgress(int currentProgress) {

        if(ksyMediaPlayer == null)
            return -1;

        long time = currentProgress > 0 ? currentProgress : ksyMediaPlayer.getCurrentPosition();
        long length = ksyMediaPlayer.getDuration();

        return (int)time;
    }

    private void updateQosInfo(QosObject obj) {
        cpuUsage = obj.cpuUsage;
        pss = obj.pss;


        if(ksyMediaPlayer != null)
        {
            bits = ksyMediaPlayer.getDecodedDataSize() * 8 / (mPause ? mPauseStartTime - mPausedTime - mStartTime : System.currentTimeMillis() - mPausedTime - mStartTime);

            info = ksyMediaPlayer.getStreamQosInfo();
        }
    }

    private void updateQosView(){
        mCpu.setText("Cpu usage:"+cpuUsage);
        mMemInfo.setText("Memory:" + pss + " KB");

        if(ksyMediaPlayer != null) {
            mVideoBitrate.setText("Bitrate: " + bits + " kb/s");
            mFrameRate.setText("VideoOutputFrameRate:" + ksyMediaPlayer.getVideoOutputFramesPerSecond());

            if(info != null) {
                mVideoBufferTime.setText("VideoBufferTime:" + info.videoBufferTimeLength + "(ms)");
                mAudioBufferTime.setText("AudioBufferTime:" + info.audioBufferTimeLength + "(ms)");
            }
        }
    }

    private String md5(String string)
    {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }

    private void videoPlayEnd() {
        if(ksyMediaPlayer != null)
        {
            ksyMediaPlayer.release();
            ksyMediaPlayer = null;
        }

        if(mQosThread != null) {
            mQosThread.stopThread();
            mQosThread = null;
        }

        mHandler = null;

        finish();
    }

    private View.OnClickListener mStartBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    private int mVideoProgress = 0;
    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                mVideoProgress = progress;
                mHandler.removeMessages(HIDDEN_SEEKBAR);
                Message msg = new Message();
                msg.what = HIDDEN_SEEKBAR;
                mHandler.sendMessageDelayed(msg,3000);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (ksyMediaPlayer != null)
                ksyMediaPlayer.seekTo(mVideoProgress);
            setVideoProgress(mVideoProgress);
        }
    };

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dealTouchEvent(v, event);
            return false;
        }
    };

    @Override
    public void onClick(View view) {
        mHandler.removeMessages(HIDDEN_SEEKBAR);
        Message msg = new Message();
        msg.what = HIDDEN_SEEKBAR;
        mHandler.sendMessageDelayed(msg,3000);
        switch (view.getId()){
            case R.id.player_reload:
                String mVideoUrl2 = "http://cxy.kss.ksyun.com/S09E24.mp4";
                // 播放新的视频
                ksyMediaPlayer.reload(mVideoUrl2, true);
                break;
            case R.id.btn_rotate_player:
                if (useHwCodec){
                    Toast.makeText(KSYSurfaceActivity.this, "旋屏请设置软解", Toast.LENGTH_SHORT).show();
                }else
                {
                    ksyMediaPlayer.setRotateDegree((rotatenum+90)%360);
                    rotatenum += 90;
                }
                break;
            case R.id.btn_screen_player:
                if (useHwCodec){
                    Toast.makeText(KSYSurfaceActivity.this, "截图请设置软解", Toast.LENGTH_SHORT).show();
                }else
                {
                    Bitmap bitmap = ksyMediaPlayer.getScreenShot();
                    savebitmap(bitmap);
                    if(bitmap!=null){
                        Toast.makeText(KSYSurfaceActivity.this, "截图成功", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_mute_player:
                if(ksyMediaPlayer!=null){
                    if(mmute == false){
                        ksyMediaPlayer.setPlayerMute(1);
                        mmute = true;
                    }else{
                        ksyMediaPlayer.setVideoOffset(-0.5f,0.5f);
                        ksyMediaPlayer.setPlayerMute(0);
                        mmute = false;
                    }
                }
                break;
            default:
                break;
        }
    }

    public void savebitmap(Bitmap bitmap){
        if (bitmap == null)
            return ;
        File appDir = new File(Environment.getExternalStorageDirectory(),"com.ksy.recordlib.demo.demo");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
