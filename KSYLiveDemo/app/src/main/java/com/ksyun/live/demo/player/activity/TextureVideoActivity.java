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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaMeta;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;
import com.ksyun.media.player.misc.KSYQosInfo;
import com.ksyun.live.demo.R;
import com.ksyun.live.demo.player.model.Strings;
import com.ksyun.live.demo.player.util.QosObject;
import com.ksyun.live.demo.player.util.QosThread;
import com.ksyun.live.demo.player.util.Settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;



public class TextureVideoActivity extends Activity implements View.OnClickListener{

    private static final String TAG = "TextureVideoActivity";

    public static final int UPDATE_QOSMESS = 2;
    public static final int UPADTE_QOSVIEW = 3;

    private SharedPreferences settings;
    private String choosedecode;
    private String choosedebug;
    private Context mContext;
    private QosThread mQosThread;

    KSYTextureView mVideoView = null;
    private Handler mHandler;

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
    private Button mReplay;
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
    private TimerTask timerTask = null;
    private long bits;
    private KSYQosInfo info;
    private String cpuUsage;
    private int pss;
    private int rotatenum = 0;
    private String mDataSource;

    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
        Log.d(TAG, "OnPrepared");
        mVideoWidth = mVideoView.getVideoWidth();
        mVideoHeight = mVideoView.getVideoHeight();

        // Set Video Scaling Mode
        mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

        if (mQosThread != null && !mQosThread.isAlive())
            mQosThread.start();


        if (mVideoView.getServerAddress() != null)
            mServerIp.setText("ServerIP: " + mVideoView.getServerAddress());

        //  get meta data
        Bundle bundle = mVideoView.getMediaMeta();
        KSYMediaMeta meta = KSYMediaMeta.parse(bundle);
        if (meta != null) {
            if (meta.mHttpConnectTime > 0) {
                double http_connection_time = Double.valueOf(meta.mHttpConnectTime);
                mHttpConnectionTime.setText("HTTP Connection Time: " + (int) http_connection_time);
            }

            int dns_time = meta.mAnalyzeDnsTime;
            if (dns_time > 0) {
                mDNSTime.setText("DNS time: " + dns_time);
            }
        }

        mSdkVersion.setText("SDK version: " + mVideoView.getVersion());

        mVideoResolution.setText("Resolution:" + mVideoView.getVideoWidth() + "x" + mVideoView.getVideoHeight());

        mStartTime = System.currentTimeMillis();
        choosedebug = settings.getString("choose_debug", "信息为空");
        if (choosedebug.isEmpty() || choosedebug.equals(Settings.DEBUGOFF)) {
            Log.e("VideoPlayer", "关闭");
            mSdkVersion.setVisibility(View.GONE);
            mVideoResolution.setVisibility(View.GONE);
            mFrameRate.setVisibility(View.GONE);
            mVideoBitrate.setVisibility(View.GONE);
            mCpu.setVisibility(View.GONE);
            mMemInfo.setVisibility(View.GONE);
            mVideoBufferTime.setVisibility(View.GONE);
            mAudioBufferTime.setVisibility(View.GONE);
            mServerIp.setVisibility(View.GONE);
            mDNSTime.setVisibility(View.GONE);
            mHttpConnectionTime.setVisibility(View.GONE);

        } else {
            Log.e("VideoPlayer", "开启");

            mSdkVersion.setVisibility(View.VISIBLE);
            mVideoResolution.setVisibility(View.VISIBLE);
            mFrameRate.setVisibility(View.VISIBLE);
            mVideoBitrate.setVisibility(View.VISIBLE);
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

        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangeListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (width != mVideoWidth || height != mVideoHeight) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();

                    if (mVideoView != null) {
                        int mode = mVideoScaleIndex % 2;
                        mVideoScaleIndex++;
                        if (mVideoView != null) {
                            if (mode == 1)
                                mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                            else
                                mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                        }
                    }
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

    private  IMediaPlayer.OnLogEventListener mOnLogEventListener = new IMediaPlayer.OnLogEventListener() {
        @Override
        public void onLogEvent(IMediaPlayer iMediaPlayer, String s) {
            Log.e(TAG, "onLogEvent : "+ s);
        }
    };
    private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            Toast.makeText(mContext, "OnCompletionListener, play complete.", Toast.LENGTH_LONG).show();
            videoPlayEnd();
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what) {
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
            if (mVideoView != null) {
                if (mode == 1)
                    mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                else
                    mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.texture_player);

        mContext = this.getApplicationContext();
        useHwCodec = getIntent().getBooleanExtra("HWCodec", false);

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

        toppanel = (RelativeLayout) findViewById(R.id.topPanel_player);
        reload = (Button) findViewById(R.id.player_reload);
        rotate = (Button) findViewById(R.id.btn_rotate_player);
        screen = (Button) findViewById(R.id.btn_screen_player);
        mute = (Button) findViewById(R.id.btn_mute_player);
        mReplay = (Button) findViewById(R.id.btn_replay);

        reload.setOnClickListener(this);
        rotate.setOnClickListener(this);
        screen.setOnClickListener(this);
        mute.setOnClickListener(this);
        mReplay.setOnClickListener(this);

        mPlayerScaleVideo = (Button) findViewById(R.id.player_scale);
        mPlayerScaleVideo.setOnClickListener(mVideoScaleButton);

        mVideoView = (KSYTextureView) findViewById(R.id.texture_view);
        mVideoView.setOnTouchListener(mTouchListener);
        mVideoView.setKeepScreenOn(true);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UPDATE_QOSMESS:
                        if (msg.obj instanceof QosObject) {
                            updateQosInfo((QosObject) msg.obj);
                        }
                        break;
                    case UPADTE_QOSVIEW:
                        updateQosView();
                        break;
                }
            }
        };

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Message message = new Message();
                        message.what = TextureVideoActivity.UPADTE_QOSVIEW;
                        if (mHandler != null && message != null) {
                            mHandler.sendMessage(message);
                        }
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                }
            };
        }

        if (timer == null) {
            timer = new Timer(true);
        }

        timer.schedule(timerTask, 2000, 5000);

        mQosThread = new QosThread(mContext, mHandler);

        mDataSource = getIntent().getStringExtra("path");

        mVideoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnInfoListener(mOnInfoListener);
        mVideoView.setOnVideoSizeChangedListener(mOnVideoSizeChangeListener);
        mVideoView.setOnErrorListener(mOnErrorListener);
        mVideoView.setOnSeekCompleteListener(mOnSeekCompletedListener);
        mVideoView.setOnLogEventListener(mOnLogEventListener);
        mVideoView.setScreenOnWhilePlaying(true);
        mVideoView.setBufferTimeMax(2.0f);
        mVideoView.setTimeout(5, 30);

        MediaController mctl = (MediaController) findViewById(R.id.media_control);
        mVideoView.setMediaController(mctl);
        //mVideoView.setMediaController(new MediaController(TextureVideoActivity.this));

        settings = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        choosedecode = settings.getString("choose_decode", "undefind");

        if (choosedecode.equals(Settings.USEHARD)) {
            useHwCodec = true;
        } else {
            useHwCodec = false;
        }

        if (useHwCodec) {
            //硬解264&265
            Log.e(TAG, "Hardware !!!!!!!!");
            mVideoView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_AUTO);
        }
        else {
            Log.e(TAG, "Software !!!!!!!!");
            mVideoView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_SOFTWARE);
        }

        try {
            mVideoView.setDataSource(mDataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoView.prepareAsync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        mVideoView = null;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mVideoView != null) {
            mVideoView.runInBackground(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.runInForeground();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
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
        toppanel.setVisibility(View.VISIBLE);
    }

    private void updateQosInfo(QosObject obj) {
        cpuUsage = obj.cpuUsage;
        pss = obj.pss;

        if (mVideoView != null) {
            bits = mVideoView.getDecodedDataSize() * 8 / (mPause ? mPauseStartTime - mPausedTime - mStartTime : System.currentTimeMillis() - mPausedTime - mStartTime);

            info = mVideoView.getStreamQosInfo();

        }
    }

    private void updateQosView() {
        mCpu.setText("Cpu usage:" + cpuUsage);
        mMemInfo.setText("Memory:" + pss + " KB");

        if (mVideoView != null) {
            mVideoBitrate.setText("Bitrate: " + bits + " kb/s");
            mFrameRate.setText("VideoOutputFrameRate:" + mVideoView.getVideoOutputFramesPerSecond());

            if (info != null) {
                mVideoBufferTime.setText("VideoBufferTime:" + info.videoBufferTimeLength + "(ms)");
                mAudioBufferTime.setText("AudioBufferTime:" + info.audioBufferTimeLength + "(ms)");
            }
        }
    }

    private String md5(String string) {
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
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }

        if (mQosThread != null) {
            mQosThread.stopThread();
            mQosThread = null;
        }

        mHandler = null;

        finish();
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            dealTouchEvent(v, event);
            return false;
        }
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_reload:
                String mVideoUrl2 = "rtmp://live.hkstv.hk.lxdns.com/live/hks";
                // 播放新的视频
                mVideoView.reload(mVideoUrl2, true);

                break;
            case R.id.btn_rotate_player:
            {
                mVideoView.setRotateDegree((rotatenum + 90) % 360);
                rotatenum += 90;
            }
            break;
            case R.id.btn_screen_player:
            {
                Bitmap bitmap = mVideoView.getScreenShot();
                savebitmap(bitmap);
                if (bitmap != null) {
                    Toast.makeText(TextureVideoActivity.this, "截图成功", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case R.id.btn_mute_player:
                if (mVideoView != null) {
                    if (mmute == false) {
                        mVideoView.setPlayerMute(1);
                        mmute = true;
                    } else {
                        mVideoView.setPlayerMute(0);
                        mmute = false;
                    }
                }
                break;
            case R.id.btn_replay:
                if (mVideoView != null){
                    mVideoView.stop();
                    //mVideoView.reset();
                    mVideoView.softReset();
                    if (useHwCodec) {
                        mVideoView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_AUTO);
                    }
                    //other settings.....
                    try {
                        mVideoView.setDataSource("http://cxy.kss.ksyun.com/S09E14.mp4");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mVideoView.prepareAsync();
                }
                break;
            default:
                break;
        }
    }

    public void savebitmap(Bitmap bitmap) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "com.ksy.recordlib.demo.demo");
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
