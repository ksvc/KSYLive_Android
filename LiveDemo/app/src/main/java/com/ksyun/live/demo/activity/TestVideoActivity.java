package com.ksyun.live.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.asha.vrlib.MD360Director;
import com.asha.vrlib.MD360DirectorFactory;
import com.asha.vrlib.MDVRLibrary;
import com.ksyun.live.demo.R;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;

import java.io.IOException;

/**
 * Created by liubohua on 16/7/27.
 */
public class TestVideoActivity extends Activity {

    private static final SparseArray<String> sDisplayMode = new SparseArray<>();
    private static final SparseArray<String> sInteractiveMode = new SparseArray<>();
    private static final SparseArray<String> sProjectionMode = new SparseArray<>();
    private MDVRLibrary mVRLibrary;

    static {
        sDisplayMode.put(MDVRLibrary.DISPLAY_MODE_NORMAL,"NORMAL");
        sDisplayMode.put(MDVRLibrary.DISPLAY_MODE_GLASS,"GLASS");

        sInteractiveMode.put(MDVRLibrary.INTERACTIVE_MODE_MOTION,"MOTION");
        sInteractiveMode.put(MDVRLibrary.INTERACTIVE_MODE_TOUCH,"TOUCH");
        sInteractiveMode.put(MDVRLibrary.INTERACTIVE_MODE_MOTION_WITH_TOUCH,"M & T");

        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_SPHERE,"SPHERE");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_DOME180,"DOME 180");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_DOME230,"DOME 230");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_DOME180_UPPER,"DOME 180 UPPER");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_DOME230_UPPER,"DOME 230 UPPER");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_STEREO_SPHERE,"STEREO SPHERE");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_PLANE_FIT,"PLANE FIT");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_PLANE_CROP,"PLANE CROP");
//        sProjectionMode.put(MDVRLibrary.PROJECTION_MODE_PLANE_FULL,"PLANE FULL");
    }

    private static final int STATUS_IDLE = 0;
    private static final int STATUS_PREPARING = 1;
    private static final int STATUS_PREPARED = 2;
    private static final int STATUS_STARTED = 3;
    private static final int STATUS_PAUSED = 4;
    private static final int STATUS_STOPPED = 5;
    private int mStatus = STATUS_IDLE;

    protected KSYMediaPlayer mPlayer;
    private IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener(){

        @Override
        public void onPrepared(IMediaPlayer mp) {
            cancelBusy();
            mStatus = STATUS_PREPARED;
            mPlayer.start();

        }
    };
    private IMediaPlayer.OnInfoListener mInfoListener = new IMediaPlayer.OnInfoListener(){

        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // set content view
        setContentView(R.layout.activity_md_using_surface_view);


        // init VR Library
        mVRLibrary = createVRLibrary();

        initMedia();

        SpinnerHelper.with(this)
                .setData(sDisplayMode)
                .setDefault(mVRLibrary.getDisplayMode())
                .setClickHandler(new SpinnerHelper.ClickHandler() {
                    @Override
                    public void onSpinnerClicked(int index, int key, String value) {
                        mVRLibrary.switchDisplayMode(TestVideoActivity.this, key);
                    }
                })
                .init(R.id.spinner_display);

        SpinnerHelper.with(this)
                .setData(sInteractiveMode)
                .setDefault(mVRLibrary.getInteractiveMode())
                .setClickHandler(new SpinnerHelper.ClickHandler() {
                    @Override
                    public void onSpinnerClicked(int index, int key, String value) {
                        mVRLibrary.switchInteractiveMode(TestVideoActivity.this, key);
                    }
                })
                .init(R.id.spinner_interactive);

        SpinnerHelper.with(this)
                .setData(sProjectionMode)
                .setDefault(mVRLibrary.getProjectionMode())
                .setClickHandler(new SpinnerHelper.ClickHandler() {
                    @Override
                    public void onSpinnerClicked(int index, int key, String value) {
                        mVRLibrary.switchProjectionMode(TestVideoActivity.this, key);
                    }
                })
                .init(R.id.spinner_projection);

    }

    private void initMedia() {

        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        mPlayer = new KSYMediaPlayer.Builder(getApplicationContext()).build();
        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnInfoListener(mInfoListener);
        mPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                try{
                    String error = String.format("Play Error what=%d extra=%d",what,extra);
                    Toast.makeText(TestVideoActivity.this, error, Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    e.printStackTrace();
                }

                return true;
            }
        });

        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", KSYMediaPlayer.SDL_FCC_RV32);
        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 60);
        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_PLAYER, "max-fps", 0);
        mPlayer.setOption(KSYMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
        mPlayer.setScreenOnWhilePlaying(true);
        mPlayer.setBufferTimeMax(5);
        try {
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mVRLibrary.handleTouchEvent(event) || super.onTouchEvent(event);
    }

    protected MDVRLibrary createVRLibrary() {
        return MDVRLibrary.with(this)
                .displayMode(MDVRLibrary.DISPLAY_MODE_NORMAL)
                .interactiveMode(MDVRLibrary.INTERACTIVE_MODE_MOTION)
                .asVideo(new MDVRLibrary.IOnSurfaceReadyCallback() {
                    @Override
                    public void onSurfaceReady(Surface surface) {
                        mPlayer.setSurface(surface);
                    }
                })
                .ifNotSupport(new MDVRLibrary.INotSupportCallback() {
                    @Override
                    public void onNotSupport(int mode) {
                        String tip = mode == MDVRLibrary.INTERACTIVE_MODE_MOTION
                                ? "onNotSupport:MOTION" : "onNotSupport:" + String.valueOf(mode);
                        Toast.makeText(TestVideoActivity.this, tip, Toast.LENGTH_SHORT).show();
                    }
                })
                .directorFactory(new DirectorFactory())
                .motionDelay(SensorManager.SENSOR_DELAY_GAME)
                .sensorCallback(new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {

                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                })
                .pinchEnabled(true)
                .gesture(new MDVRLibrary.IGestureListener() {
                    @Override
                    public void onClick(MotionEvent e) {
                        Toast.makeText(TestVideoActivity.this, "onClick!", Toast.LENGTH_SHORT).show();
                    }
                })
                .build(R.id.gl_view);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mVRLibrary.onResume(this);
        mPlayer.start();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mVRLibrary.onPause(this);

        mPlayer.pause();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVRLibrary.onDestroy();
    }


    public void cancelBusy(){
        findViewById(R.id.progress).setVisibility(View.GONE);
    }

    private static class DirectorFactory extends MD360DirectorFactory {
        @Override
        public MD360Director createDirector(int index) {
            switch (index) {
                // setAngle: angle to rotate in degrees
                case 1:
                    return MD360Director.builder().setAngle(20).setEyeX(-2.0f).setLookX(-2.0f).build();
                default:
                    return MD360Director.builder().setAngle(20).build();
            }
        }
    }
}
