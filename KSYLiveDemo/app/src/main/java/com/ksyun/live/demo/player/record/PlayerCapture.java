package com.ksyun.live.demo.player.record;

import android.content.Context;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.player.KSYMediaPlayer;
import com.ksyun.media.player.KSYTextureView;
import com.ksyun.media.streamer.encoder.ColorFormatConvert;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.AudioBufFrame;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.framework.ImgBufFrame;
import com.ksyun.media.streamer.framework.SrcPin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by liubohua on 2017/2/20.
 */
public class PlayerCapture implements KSYMediaPlayer.OnVideoRawDataListener, KSYMediaPlayer.OnAudioPCMListener {

    public SrcPin<AudioBufFrame> mAudioSrcPin;
    public SrcPin<ImgBufFrame> mVideoSrcPin;

    public SrcPin<AudioBufFrame> getAudioSrcPin() {
        return mAudioSrcPin;
    }

    public SrcPin<ImgBufFrame> getVideoSrcPin() {
        return mVideoSrcPin;
    }

    private Context mContext;
    private AudioBufFormat mAudioFormat;
    private ByteBuffer mAudioBuffer;
    private ByteBuffer mVideoBuffer;
    private ImgBufFormat mVideoFormat;
    private ByteBuffer mVideoOutBuffer;
    private int frameNum = 0;
    private long audioBufferSize = 0;
    private boolean gotFirstAudioBuffer = false;
    private long firstAudioBufferTime = 0;

    private boolean mStarted = false;

    public PlayerCapture(Context context) {
        mContext = context;
        mAudioSrcPin = new SrcPin<>();
        mVideoSrcPin = new SrcPin<>();
    }



    public void start() {
        mStarted = true;

        audioBufferSize = 0;
        gotFirstAudioBuffer = false;
        frameNum = 0;
    }

    public void stop() {
        mStarted = false;
    }


    public void release() {
        mAudioSrcPin.disconnect(true);
        mVideoSrcPin.disconnect(true);
        mAudioBuffer = null;
        mVideoBuffer = null;
    }

    @Override
    public void onVideoRawDataAvailable(IMediaPlayer iMediaPlayer, byte[] bytes, int size, int width, int height, int format, long pts) {
        if (iMediaPlayer == null)
            return ;

        if (mStarted) {

            if (mVideoFormat == null) {
                mVideoFormat = new ImgBufFormat(ImgBufFormat.FMT_YV12, width, height, 0);
                mVideoSrcPin.onFormatChanged(mVideoFormat);
            }

            if (mVideoOutBuffer == null || mVideoOutBuffer.capacity() < size) {
                mVideoOutBuffer = ByteBuffer.allocateDirect(size);
                mVideoOutBuffer.order(ByteOrder.nativeOrder());
            }
            mVideoOutBuffer.clear();
            mVideoOutBuffer.put(bytes, 0, size);
            mVideoOutBuffer.flip();

            ImgBufFrame frame = new ImgBufFrame(mVideoFormat, mVideoOutBuffer, pts);
            if (mVideoSrcPin.isConnected()) {
                mVideoSrcPin.onFrameAvailable(frame);
            }
        }
        KSYMediaPlayer ksyMediaPlayer = (KSYMediaPlayer)iMediaPlayer;
        ksyMediaPlayer.addVideoRawBuffer(bytes);
    }

    @Override
    public void onAudioPCMAvailable(IMediaPlayer iMediaPlayer, ByteBuffer byteBuffer, long timestamp, int channels, int samplerate, int samplefmt) {
        if (iMediaPlayer == null)
            return ;

        if (!mStarted)
            return;

        if (mAudioFormat == null) {
            mAudioFormat = new AudioBufFormat(samplefmt, samplerate, channels);
            mAudioSrcPin.onFormatChanged(mAudioFormat);
        }
        if (byteBuffer == null) {
            return;
        }

        ByteBuffer pcmBuffer = byteBuffer;
        int msBufferSize = 1 * samplerate * channels * 2 / 1000;//1 ms
        int len = byteBuffer.limit();
        audioBufferSize += len;
        long bufferTime = audioBufferSize / msBufferSize;
        //Log.e("onAudioPCMAvailable", "audio bufferd " + bufferTime + " ms");

        if (!gotFirstAudioBuffer) {
            firstAudioBufferTime = System.nanoTime() / 1000 / 1000;
            gotFirstAudioBuffer = true;
        }

        if (mAudioBuffer == null || mAudioBuffer.capacity() < len) {
            mAudioBuffer = ByteBuffer.allocateDirect( len * 20 );
            mAudioBuffer.order(ByteOrder.nativeOrder());
            mAudioBuffer.clear();
        }
        mAudioBuffer.put(byteBuffer);

        if (frameNum >= 7) {
            mAudioBuffer.flip();
            pcmBuffer = mAudioBuffer;
            //Log.e("onAudioPCMAvailable", "pts: " + pts);
            AudioBufFrame frame = new AudioBufFrame(mAudioFormat, pcmBuffer, timestamp);
            if (mAudioSrcPin.isConnected()) {
                mAudioSrcPin.onFrameAvailable(frame);
            }

            frameNum = 0;
            mAudioBuffer.clear();
        } else {
            frameNum++;
        }

    }
}
