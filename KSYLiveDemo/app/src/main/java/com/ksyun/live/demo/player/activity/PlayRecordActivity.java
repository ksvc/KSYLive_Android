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
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.live.demo.R;
import com.ksyun.live.demo.player.model.NetState;
import com.ksyun.live.demo.player.model.Strings;
import com.ksyun.live.demo.player.record.KSYPlayerRecord;
import com.ksyun.live.demo.player.util.NetStateUtil;
import com.ksyun.live.demo.player.util.ProgressTextView;
import com.ksyun.live.demo.player.util.QosObject;
import com.ksyun.live.demo.player.util.QosThread;
import com.ksyun.live.demo.player.util.Settings;
import com.ksyun.live.demo.player.util.VerticalSeekBar;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaMeta;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;
import com.ksyun.media.player.misc.KSYQosInfo;
import com.ksyun.media.streamer.kit.StreamerConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by liubohua on 2017/2/16.
 */
public class PlayRecordActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "TextureVodActivity";

    public static final int UPDATE_SEEKBAR = 0;
    public static final int HIDDEN_SEEKBAR = 1;
    public static final int UPDATE_QOSMESS = 2;
    public static final int UPADTE_QOSVIEW = 3;

    private SharedPreferences settings;
    private String chooseDecode;
    private String chooseDebug;
    private String bufferTime;
    private String bufferSize;

    private Context mContext;
    private QosThread mQosThread;

    KSYTextureView mVideoView = null;
    private Handler mHandler;

    private VerticalSeekBar mAudioSeekbar;
    private ProgressTextView mProgressTextView;

    private RelativeLayout mPlayerPanel;
    private ImageView mPlayerStartBtn;
    private SeekBar mPlayerSeekbar;
    private TextView mPlayerPosition;
    private TextView mLoadText;
    private TextView mCpu;
    private TextView mMemInfo;
    private TextView mVideoResolution;
    private TextView mVideoBitrate;
    private TextView mVideoBufferTime;
    private TextView mAudioBufferTime;
    private TextView mServerIp;
    private TextView mSdkVersion;
    private TextView mDNSTime;
    private TextView mHttpConnectionTime;
    //卡顿信息
    private TextView mBufferEmptyCnt;
    private TextView mBufferEmptyDuration;
    private TextView mDecodeFps;
    private TextView mOutputFps;

    private RelativeLayout topPanel;
    private ImageView reload;
    private ImageView mPlayerVolume;
    private ImageView mPlayerRotate;
    private ImageView mPlayerScreen;
    private ImageView mPlayerScale;

    private boolean mPlayerPanelShow = false;
    private boolean mPause = false;

    private boolean showAudioBar = false;

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
    private int rotateNum = 0;

    private String mDataSource;

    private Button mBtnRecord;
    private boolean mRecording = false;
    private KSYPlayerRecord playerRecord;

    private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(IMediaPlayer mp) {
            Log.d("VideoPlayer", "OnPrepared");
            mVideoWidth = mVideoView.getVideoWidth();
            mVideoHeight = mVideoView.getVideoHeight();

            playerRecord.setTargetResolution(mVideoWidth, mVideoHeight);

            // Set Video Scaling Mode
            mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);

            ByteBuffer rawBuffer[] = new ByteBuffer[5];     //5 buffers is just an example
            for (int index = 0; index < rawBuffer.length; index++) {
                int yStride = (mVideoWidth + 15) / 16 * 16;
                int cStride = ((yStride / 2) + 15) / 16 * 16;
                rawBuffer[index] = ByteBuffer.allocate(yStride * mVideoHeight + cStride * mVideoHeight);
                mVideoView.addVideoRawBuffer(rawBuffer[index].array());
            }


            //start player
            mVideoView.start();

            //set progress
            setVideoProgress(0);

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
            chooseDebug = settings.getString("choose_debug", "信息为空");
            if (chooseDebug.isEmpty() || chooseDebug.equals(Settings.DEBUGOFF)) {
                Log.e("VideoPlayer", "关闭");
                mSdkVersion.setVisibility(View.GONE);
                mVideoResolution.setVisibility(View.GONE);
                mVideoBitrate.setVisibility(View.GONE);
                mLoadText.setVisibility(View.GONE);
                mCpu.setVisibility(View.GONE);
                mMemInfo.setVisibility(View.GONE);
                mVideoBufferTime.setVisibility(View.GONE);
                mAudioBufferTime.setVisibility(View.GONE);
                mServerIp.setVisibility(View.GONE);
                mDNSTime.setVisibility(View.GONE);
                mHttpConnectionTime.setVisibility(View.GONE);
                mBufferEmptyCnt.setVisibility(View.GONE);
                mBufferEmptyDuration.setVisibility(View.GONE);
                mDecodeFps.setVisibility(View.GONE);
                mOutputFps.setVisibility(View.GONE);
            } else {
                Log.e("VideoPlayer", "开启");

                mSdkVersion.setVisibility(View.VISIBLE);
                mVideoResolution.setVisibility(View.VISIBLE);
                mVideoBitrate.setVisibility(View.VISIBLE);
                mLoadText.setVisibility(View.VISIBLE);
                mCpu.setVisibility(View.VISIBLE);
                mMemInfo.setVisibility(View.VISIBLE);
                mVideoBufferTime.setVisibility(View.VISIBLE);
                mAudioBufferTime.setVisibility(View.VISIBLE);
                mServerIp.setVisibility(View.VISIBLE);
                mDNSTime.setVisibility(View.VISIBLE);
                mHttpConnectionTime.setVisibility(View.VISIBLE);
                mBufferEmptyCnt.setVisibility(View.VISIBLE);
                mBufferEmptyDuration.setVisibility(View.VISIBLE);
                mDecodeFps.setVisibility(View.VISIBLE);
                mOutputFps.setVisibility(View.VISIBLE);
            }
        }
    };

    private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            long duration = mVideoView.getDuration();
            long progress = duration * percent / 100;
            mPlayerSeekbar.setSecondaryProgress((int) progress);
        }
    };

    private IMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangeListener = new IMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                if (width != mVideoWidth || height != mVideoHeight) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();

                    if (mVideoView != null)
                        mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
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
            Toast.makeText(mContext, "OnCompletionListener, play complete.", Toast.LENGTH_LONG).show();
            videoPlayEnd();
        }
    };

    private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            switch (what) {
                //case KSYVideoView.MEDIA_ERROR_UNKNOWN:
                // Log.e(TAG, "OnErrorListener, Error Unknown:" + what + ",extra:" + extra);
                //  break;
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

    private IMediaPlayer.OnMessageListener mOnMessageListener = new IMediaPlayer.OnMessageListener() {
        @Override
        public void onMessage(IMediaPlayer iMediaPlayer, String name, String info, double number) {
            Log.e(TAG, "name:" + name + ",info:" + info + ",number:" + number);
        }
    };

    private SeekBar.OnSeekBarChangeListener audioSeekbarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            mProgressTextView.setProgress(i, i + "%");
            mVideoView.setVolume((float) i / 100, (float) i / 100);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this.getApplicationContext();
        useHwCodec = getIntent().getBooleanExtra("HWCodec", false);

        setContentView(R.layout.activity_record);

        mPlayerPanel = (RelativeLayout) findViewById(R.id.player_panel);
        mPlayerStartBtn = (ImageView) findViewById(R.id.player_start);
        mPlayerSeekbar = (SeekBar) findViewById(R.id.player_seekbar);
        mPlayerVolume = (ImageView) findViewById(R.id.player_volume);
        mPlayerRotate = (ImageView) findViewById(R.id.player_rotate);
        mPlayerScreen = (ImageView) findViewById(R.id.player_screen);
        mPlayerScale = (ImageView) findViewById(R.id.player_scale);
        mPlayerPosition = (TextView) findViewById(R.id.player_time);
        mLoadText = (TextView) findViewById(R.id.loading_text);
        mCpu = (TextView) findViewById(R.id.player_cpu);
        mMemInfo = (TextView) findViewById(R.id.player_mem);
        mVideoResolution = (TextView) findViewById(R.id.player_re);
        mVideoBitrate = (TextView) findViewById(R.id.player_br);
        mVideoBufferTime = (TextView) findViewById(R.id.player_video_time);
        mAudioBufferTime = (TextView) findViewById(R.id.player_audio_time);
        mServerIp = (TextView) findViewById(R.id.player_ip);
        mSdkVersion = (TextView) findViewById(R.id.player_sdk_version);
        mDNSTime = (TextView) findViewById(R.id.player_dns_time);
        mHttpConnectionTime = (TextView) findViewById(R.id.player_http_connection_time);
        mBufferEmptyCnt = (TextView) findViewById(R.id.player_buffer_empty_count);
        mBufferEmptyDuration = (TextView) findViewById(R.id.player_buffer_empty_duration);
        mDecodeFps = (TextView) findViewById(R.id.player_decode_fps);
        mOutputFps = (TextView) findViewById(R.id.player_output_fps);

        topPanel = (RelativeLayout) findViewById(R.id.rightPanel_player);
        reload = (ImageView) findViewById(R.id.player_reload);
        //mReplay = (Button) findViewById(R.id.btn_replay);

        mAudioSeekbar = (VerticalSeekBar) findViewById(R.id.player_audio_seekbar);
        mProgressTextView = (ProgressTextView) findViewById(R.id.ptv_open_percentage);
        mAudioSeekbar.setProgress(100);
        mAudioSeekbar.setOnSeekBarChangeListener(audioSeekbarListener);

        reload.setOnClickListener(this);
        mPlayerVolume.setOnClickListener(this);
        mPlayerRotate.setOnClickListener(this);
        mPlayerScreen.setOnClickListener(this);
        mPlayerScale.setOnClickListener(mVideoScaleButton);

        mPlayerStartBtn.setOnClickListener(mStartBtnListener);
        mPlayerSeekbar.setOnSeekBarChangeListener(mSeekBarListener);
        mPlayerSeekbar.setEnabled(true);

        mPlayerSeekbar.bringToFront();

        mVideoView = (KSYTextureView) findViewById(R.id.texture_view);
        mVideoView.setOnTouchListener(mTouchListener);
        mVideoView.setKeepScreenOn(true);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UPDATE_SEEKBAR:
                        setVideoProgress(0);
                        break;
                    case HIDDEN_SEEKBAR:
                        mPlayerPanelShow = false;
                        if (showAudioBar) {
                            hideAudioBar();
                        }
                        mPlayerPanel.setVisibility(View.GONE);
                        topPanel.setVisibility(View.GONE);
                        break;
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
        mVideoView.setOnMessageListener(mOnMessageListener);
        mVideoView.setScreenOnWhilePlaying(true);
        mVideoView.setTimeout(5, 30);

        settings = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        chooseDecode = settings.getString("choose_decode", "undefind");
        bufferTime = settings.getString("buffertime", "2");
        bufferSize = settings.getString("buffersize", "15");


        if (!TextUtils.isEmpty(bufferTime)) {
            mVideoView.setBufferTimeMax(Integer.parseInt(bufferTime));
            Log.e(TAG, "palyer buffertime :" + bufferTime);
        }

        if (!TextUtils.isEmpty(bufferSize)) {
            mVideoView.setBufferSize(Integer.parseInt(bufferSize));
            Log.e(TAG, "palyer buffersize :" + bufferSize);
        }

        playerRecord = new KSYPlayerRecord(this);
        playerRecord.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
        playerRecord.setTargetFps(15);

        mVideoView.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", KSYMediaPlayer.SDL_FCC_YV12);
        mVideoView.setVideoRawDataListener(playerRecord.getPlayerCapture());
        mVideoView.setOnAudioPCMAvailableListener(playerRecord.getPlayerCapture());

        mBtnRecord = (Button) findViewById(R.id.record);
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mRecordUrl = "/storage/emulated/0/playerRecord.mp4";
                if (playerRecord == null)
                    return;

                if ( mRecording == false) {
                    mBtnRecord.setTextColor(getResources().getColor(R.color.player_red));
                    playerRecord.startRecord(mRecordUrl);
                    mRecording = true;
                }
                else {
                    mBtnRecord.setTextColor(getResources().getColor(R.color.player_white));
                    playerRecord.stopRecord();
                    mRecording = false;
                }
            }
        });

        mVideoView.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_SOFTWARE);

        try {
            mVideoView.setDataSource(mDataSource);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mVideoView.prepareAsync();
    }

    private NetStateUtil.NetChangeListener netChangeListener = new NetStateUtil.NetChangeListener() {
        @Override
        public void onNetStateChange(int netWorkState) {
            switch (netWorkState) {
                case NetState.NETWORK_MOBILE:
                    break;
                case NetState.NETWORK_WIFI:
                    break;
                case NetState.NETWORK_NONE:
                    Toast.makeText(PlayRecordActivity.this, "没有监测到网络,请检查网络连接", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
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
            mHandler.sendMessageDelayed(msg, 3000);
            if (mVideoView != null) {
                if (mode == 1) {
                    mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
                    mPlayerScale.setImageResource(R.drawable.scale);
                } else {
                    mVideoView.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                    mPlayerScale.setImageResource(R.drawable.scale_fit);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        mVideoView = null;
        NetStateUtil.unregisterNetState(this);
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
        NetStateUtil.registerNetState(this, netChangeListener);
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

        if (mPlayerPanelShow) {
            mPlayerPanel.setVisibility(View.VISIBLE);
            topPanel.setVisibility(View.VISIBLE);

            Message msg = new Message();
            msg.what = HIDDEN_SEEKBAR;
            mHandler.sendMessageDelayed(msg, 3000);
        } else {
            mPlayerPanel.setVisibility(View.GONE);
            topPanel.setVisibility(View.GONE);
            mHandler.removeMessages(HIDDEN_SEEKBAR);
        }
    }

    public int setVideoProgress(int currentProgress) {

        if (mVideoView == null)
            return -1;

        long time = currentProgress > 0 ? currentProgress : mVideoView.getCurrentPosition();
        long length = mVideoView.getDuration();

        // Update all view elements
        mPlayerSeekbar.setMax((int) length);
        mPlayerSeekbar.setProgress((int) time);

        if (time >= 0) {
            String progress = Strings.millisToString(time) + "/" + Strings.millisToString(length);
            mPlayerPosition.setText(progress);
        }

        Message msg = new Message();
        msg.what = UPDATE_SEEKBAR;

        if (mHandler != null)
            mHandler.sendMessageDelayed(msg, 1000);
        return (int) time;
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
            mBufferEmptyCnt.setText("BufferEmptyCount:" + mVideoView.bufferEmptyCount());
            mBufferEmptyDuration.setText("BufferEmptyDuration:" + mVideoView.bufferEmptyDuration());
            mDecodeFps.setText("DecodeFps:" + mVideoView.getVideoDecodeFramesPerSecond());
            mOutputFps.setText("OutputFps:" + mVideoView.getVideoOutputFramesPerSecond());
            if (info != null) {
                mVideoBufferTime.setText("VideoBufferTime:" + info.videoBufferTimeLength + "(ms)");
                mAudioBufferTime.setText("AudioBufferTime:" + info.audioBufferTimeLength + "(ms)");
            }
        }
    }

    private void videoPlayEnd() {

        if (playerRecord != null ) {
            if (mRecording) {
                playerRecord.stopRecord();
            }
            playerRecord = null;
            mRecording = false;
        }

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

    private View.OnClickListener mStartBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPause = !mPause;
            mHandler.removeMessages(HIDDEN_SEEKBAR);
            Message msg = new Message();
            msg.what = HIDDEN_SEEKBAR;
            mHandler.sendMessageDelayed(msg, 3000);
            if (mPause) {
                mPlayerStartBtn.setBackgroundResource(R.drawable.ksy_pause_btn);
                mVideoView.pause();
                mPauseStartTime = System.currentTimeMillis();
            } else {
                mPlayerStartBtn.setBackgroundResource(R.drawable.ksy_playing_btn);
                mVideoView.start();
                mPausedTime += System.currentTimeMillis() - mPauseStartTime;
                mPauseStartTime = 0;
            }
        }
    };

    private int mVideoProgress = 0;
    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mVideoProgress = progress;
                mHandler.removeMessages(HIDDEN_SEEKBAR);
                Message msg = new Message();
                msg.what = HIDDEN_SEEKBAR;
                mHandler.sendMessageDelayed(msg, 3000);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mVideoView.seekTo(mVideoProgress);
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
        mHandler.sendMessageDelayed(msg, 3000);
        switch (view.getId()) {
            case R.id.player_volume:
                if (!showAudioBar) {
                    showAudioBar();
                } else {
                    hideAudioBar();
                }
                break;
            case R.id.player_reload:
                String mVideoUrl2 = "rtmp://live.hkstv.hk.lxdns.com/live/hks";
                // 播放新的视频
                mVideoView.reload(mVideoUrl2, true);
                break;

            case R.id.player_rotate: {
                rotateNum += 90;
                mVideoView.setRotateDegree(rotateNum % 360);
            }
            break;
            case R.id.player_screen: {
                Bitmap bitmap = mVideoView.getScreenShot();
                savebitmap(bitmap);
                if (bitmap != null) {
                    Toast.makeText(PlayRecordActivity.this, "截图成功", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                break;
        }
    }

    private void hideAudioBar() {
        mAudioSeekbar.setVisibility(View.INVISIBLE);
        mProgressTextView.setVisibility(View.INVISIBLE);
        showAudioBar = false;
    }

    private void showAudioBar() {
        mAudioSeekbar.setVisibility(View.VISIBLE);
        mProgressTextView.setVisibility(View.VISIBLE);
        int Progress = mAudioSeekbar.getProgress();
        mProgressTextView.setProgress(Progress, Progress + "%");
        showAudioBar = true;
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
