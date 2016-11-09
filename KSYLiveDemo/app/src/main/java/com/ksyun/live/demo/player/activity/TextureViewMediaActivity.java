package com.ksyun.live.demo.player.activity;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.ksyun.live.demo.R;
import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;

import java.io.IOException;

public class TextureViewMediaActivity extends Activity implements TextureView.SurfaceTextureListener,
        IMediaPlayer.OnPreparedListener{
    private static final String TAG = "GLRender";
    private  String videoPath= "rtmp://live.hkstv.hk.lxdns.com/live/hks";
    private TextureView textureView;
    private KSYMediaPlayer mediaPlayer;

    private TextureSurfaceRenderer videoRenderer;
    private SurfaceTexture mSurfaceTexture;
    private int surfaceWidth;
    private int surfaceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.glrender_view_layout);

        videoPath = getIntent().getStringExtra("path");

        textureView = (TextureView) findViewById(R.id.id_textureview);
        textureView.setSurfaceTextureListener(this);

    }

    private void playVideo(SurfaceTexture surfaceTexture) {
        videoRenderer = new VideoTextureSurfaceRenderer(this, surfaceTexture, surfaceWidth, surfaceHeight);
        initMediaPlayer( surfaceTexture);
    }

    private void initMediaPlayer(SurfaceTexture surfaceTexture) {
        try {
            this.mediaPlayer = new KSYMediaPlayer.Builder(getApplicationContext()).build();

            while (videoRenderer.getVideoTexture() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Surface surface = new Surface(videoRenderer.getVideoTexture());
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setScreenOnWhilePlaying(true);

            mediaPlayer.setDecodeMode(KSYMediaPlayer.KSYDecodeMode.KSY_DECODE_MODE_SOFTWARE);

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setLooping(true);
        } catch (IllegalArgumentException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (SecurityException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
    @Override
    public void onPrepared(IMediaPlayer mp) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.setVideoScalingMode(KSYMediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                mediaPlayer.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        Log.e(TAG, "GLViewMediaActivity::onResume()");
        super.onResume();
        if (mediaPlayer != null) {
            mediaPlayer.start();
            if(videoRenderer == null)
            {
                playVideo(mSurfaceTexture);
            }
        }
    }


    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoRenderer != null) {
            videoRenderer.onPause();
            videoRenderer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer =null;
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        ///Log.e( TAG, "GLViewMediaActivity::onSurfaceTextureAvailable()"+ " tName:" + Thread.currentThread().getName() + "  tid:");
        surfaceWidth = width;
        surfaceHeight = height;
        playVideo(surface);
        mSurfaceTexture = surface;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //videoRenderer.getVideoTexture().setDefaultBufferSize(width,height);
        videoRenderer.resize(width,height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


}
