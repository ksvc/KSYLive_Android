package com.ksyun.live.demo.player.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.ksyun.live.demo.R;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;

import java.io.IOException;

/**
 * Created by xbc on 2017/12/29.
 */

public class MultiplePlayerActivity extends Activity {

    private SurfaceView mMasterSurfaceView;
    private SurfaceView mSlaveSurfaceView;
    private SurfaceView mSlaveSurfaceView2;
    private TextView mMaster;
    private TextView mSlave;

    private KSYMediaPlayer mMasterPlayer;
    private KSYMediaPlayer mSlavePlayer;
    private KSYMediaPlayer mSlavePlayer2;
    private String mDataSource;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_multiple_player);

        mMasterSurfaceView = (SurfaceView) findViewById(R.id.player_master);
        mSlaveSurfaceView = (SurfaceView) findViewById(R.id.player_slave);
        mSlaveSurfaceView2 = (SurfaceView) findViewById(R.id.player_slave2);
        mMaster = (TextView) findViewById(R.id.control_master);
        mSlave = (TextView) findViewById(R.id.control_slave);
        mDataSource = getIntent().getStringExtra("path");
        initPlayers();
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSlavePlayer2 != null)
            mSlavePlayer2.release();
        mSlavePlayer2 = null;

        if (mSlavePlayer != null)
            mSlavePlayer.release();
        mSlavePlayer = null;

        if (mMasterPlayer != null)
            mMasterPlayer.release();
        mMasterPlayer = null;
    }

    private void initPlayers() {
        mMasterPlayer = new KSYMediaPlayer.Builder(this).build();
        mSlavePlayer = new KSYMediaPlayer.Builder(this).build();
        mSlavePlayer2 = new KSYMediaPlayer.Builder(this).build();


        mMasterPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                if (mMasterPlayer != null)
                    mMasterPlayer.start();
            }
        });
        try {
            mMasterPlayer.shouldAutoPlay(false);
            mMasterPlayer.setDataSource(mDataSource);
            mMasterPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSlavePlayer.shouldAutoPlay(false);
        mSlavePlayer.addMasterClock(mMasterPlayer);
        mSlavePlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                if (mSlavePlayer != null) {
                    mSlavePlayer.start();
                    mSlavePlayer.setPlayerMute(1);
                }
            }
        });
        try {
            mSlavePlayer.setDataSource(mDataSource);
            mSlavePlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSlavePlayer2.shouldAutoPlay(false);
        mSlavePlayer2.addMasterClock(mMasterPlayer);
        mSlavePlayer2.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                if (mMasterPlayer != null)
                    mMasterPlayer.start();
                if (mSlavePlayer != null) {
                    mSlavePlayer.start();
                    mSlavePlayer.setPlayerMute(1);
                }
                if (mSlavePlayer2 != null) {
                    mSlavePlayer2.start();
                    mSlavePlayer2.setPlayerMute(1);
                }
            }
        });
        try {
            mSlavePlayer2.setDataSource(mDataSource);
            mSlavePlayer2.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean mMasterPlaying = true;
    private boolean mSlavePlaying = true;
    private void initViews() {
        mMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMasterPlaying)
                    mMasterPlayer.pause();
                else
                    mMasterPlayer.start();

                mMasterPlaying = !mMasterPlaying;
            }
        });

        mSlave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSlavePlaying)
                    mSlavePlayer.pause();
                else
                    mSlavePlayer.start();

                mSlavePlaying = !mSlavePlaying;
            }
        });

        mMasterSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                if (mMasterPlayer != null)
                    mMasterPlayer.setSurface(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mMasterPlayer != null)
                    mMasterPlayer.setSurface(null);
            }
        });

        mSlaveSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                if (mSlavePlayer != null)
                    mSlavePlayer.setSurface(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mSlavePlayer != null)
                    mSlavePlayer.setSurface(null);
            }
        });

        mSlaveSurfaceView2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                if (mSlavePlayer2 != null)
                    mSlavePlayer2.setSurface(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (mSlavePlayer2 != null)
                    mSlavePlayer2.setSurface(null);
            }
        });
    }
}
