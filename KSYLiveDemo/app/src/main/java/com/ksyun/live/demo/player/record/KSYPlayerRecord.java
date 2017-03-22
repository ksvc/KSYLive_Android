package com.ksyun.live.demo.player.record;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.ksyun.media.streamer.encoder.AVCodecAudioEncoder;
import com.ksyun.media.streamer.encoder.AVCodecVideoEncoder;
import com.ksyun.media.streamer.encoder.AudioEncodeFormat;
import com.ksyun.media.streamer.encoder.AudioEncoderMgt;
import com.ksyun.media.streamer.encoder.Encoder;
import com.ksyun.media.streamer.encoder.MediaCodecAudioEncoder;
import com.ksyun.media.streamer.encoder.VideoEncodeFormat;
import com.ksyun.media.streamer.filter.audio.AudioResampleFilter;
import com.ksyun.media.streamer.framework.AVConst;
import com.ksyun.media.streamer.framework.AudioBufFormat;
import com.ksyun.media.streamer.framework.ImgBufFormat;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.publisher.FilePublisher;
import com.ksyun.media.streamer.publisher.Publisher;
import com.ksyun.media.streamer.publisher.PublisherMgt;
import com.ksyun.media.streamer.util.gles.GLRender;

/**
 * Created by liubohua on 2017/2/20.
 */
public class KSYPlayerRecord {
    private static final String TAG = "KSYPlayerRecord";

    private Context mContext;
    private int mScreenRenderWidth = 0;
    private int mScreenRenderHeight = 0;
    private int mTargetResolution = StreamerConstants.VIDEO_RESOLUTION_360P;
    private int mTargetWidth = 0;
    private int mTargetHeight = 0;
    private float mTargetFps = 0;
    private float mIFrameInterval = 3.0f;
    private int mVideoCodecId = AVConst.CODEC_ID_AVC;

    private int mMaxVideoBitrate = 800 * 1000;
    private int mInitVideoBitrate = 600 * 1000;
    private int mMinVideoBitrate = 200 * 1000;
    private int mEncodeScene = VideoEncodeFormat.ENCODE_SCENE_SHOWSELF;
    private int mEncodeProfile = VideoEncodeFormat.ENCODE_PROFILE_LOW_POWER;
    private boolean mAutoAdjustVideoBitrate = true;
    private int mAudioBitrate = 48 * 1000;
    private int mAudioSampleRate = 44100;
    private int mAudioChannels = 1;

    private OnInfoListener mOnInfoListener;
    private OnErrorListener mOnErrorListener;

    private boolean mIsRecording = false;
    private boolean mIsAudioOnly = false;
    private boolean mEnableDebugLog = false;
    private boolean mIsFileRecording = false;

    private boolean mIsCaptureStarted = false;

    private FilePublisher mFilePublisher;

    private AVCodecVideoEncoder mVideoEncoder;
    private AudioEncoderMgt mAudioEncoderMgt;
    private AudioResampleFilter mAudioResampleFilter;
    private PlayerCapture mPlayerCapture;

    private Handler mMainHandler;


    public KSYPlayerRecord(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null!");
        }
        mContext = context.getApplicationContext();
        mMainHandler = new Handler(Looper.getMainLooper());
        initModules();
    }

    private void initModules() {

        mPlayerCapture = new PlayerCapture(mContext);
        mAudioResampleFilter = new AudioResampleFilter();

        // encoder
        mVideoEncoder = new AVCodecVideoEncoder();
        mAudioEncoderMgt = new AudioEncoderMgt();

        mPlayerCapture.getAudioSrcPin().connect(mAudioResampleFilter.getSinkPin());
        mAudioResampleFilter.getSrcPin().connect(mAudioEncoderMgt.getSinkPin());
        mPlayerCapture.getVideoSrcPin().connect(mVideoEncoder.mSinkPin);

        mFilePublisher = new FilePublisher();
        mAudioEncoderMgt.getSrcPin().connect(mFilePublisher.getAudioSink());
        mVideoEncoder.mSrcPin.connect(mFilePublisher.getVideoSink());

        // set listeners

        Encoder.EncoderListener encoderListener = new Encoder.EncoderListener() {
            @Override
            public void onError(Encoder encoder, int err) {
                if (err != 0) {
                    //stopRecord();
                }

                boolean isVideo = true;
                if (encoder instanceof MediaCodecAudioEncoder ||
                        encoder instanceof AVCodecAudioEncoder) {
                    isVideo = false;
                }

                int what;
                switch (err) {
                    case Encoder.ENCODER_ERROR_UNSUPPORTED:
                        what = isVideo ?
                                StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED :
                                StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED;
                        break;
                    case Encoder.ENCODER_ERROR_UNKNOWN:
                    default:
                        what = isVideo ?
                                StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN :
                                StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN;
                        break;
                }
                if (mOnErrorListener != null) {
                    mOnErrorListener.onError(what, 0, 0);
                }
            }
        };
        mVideoEncoder.setEncoderListener(encoderListener);
        mAudioEncoderMgt.setEncoderListener(encoderListener);

        mFilePublisher.setPubListener(new Publisher.PubListener() {

            @Override
            public void onInfo(int type, long msg) {
                switch (type) {
                    case FilePublisher.INFO_OPENED:
                        //start audio encoder first
                        if (!mAudioEncoderMgt.getEncoder().isEncoding()) {
                            mAudioEncoderMgt.getEncoder().start();
                        }
                        if (mOnInfoListener != null) {
                            mOnInfoListener.onInfo(
                                    StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS, 0, 0);
                        }
                        break;
                    case FilePublisher.INFO_AUDIO_HEADER_GOT:
                        if (!mIsAudioOnly) {
                            // start video encoder after audio header got
                            if (!mVideoEncoder.isEncoding()) {
                                mVideoEncoder.start();
                            }
                            mVideoEncoder.forceKeyFrame();
                        }
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onError(int err, long msg) {
                Log.e(TAG, "FilePublisher err=" + err);
                if (err != 0) {
                    stopRecord();
                }

                if (mOnErrorListener != null) {
                    int status;
                    switch (err) {
                        case FilePublisher.FILE_PUBLISHER_ERROR_OPEN_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_OPEN_FAILED;
                            break;
                        case FilePublisher.FILE_PUBLISHER_ERROR_WRITE_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_WRITE_FAILED;
                            break;
                        case FilePublisher.FILE_PUBLISHER_ERROR_CLOSE_FAILED:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_CLOSE_FAILED;
                            break;
                        default:
                            status = StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_ERROR_UNKNOWN;
                            break;
                    }
                    mOnErrorListener.onError(status, (int) msg, 0);
                }
                //do not need to restart
            }
        });
    }

    public AVCodecVideoEncoder getVideoEncoderMgt() {
        return mVideoEncoder;
    }

    public AudioEncoderMgt getAudioEncoderMgt() {
        return mAudioEncoderMgt;
    }

    public void setTargetResolution(int width, int height) throws IllegalArgumentException {
        if (width < 0 || height < 0 || (width == 0 && height == 0)) {
            throw new IllegalArgumentException("Invalid resolution");
        }
        mTargetWidth = width;
        mTargetHeight = height;

        if (mScreenRenderWidth != 0 && mScreenRenderHeight != 0) {
            calResolution();
        }
    }

    private void calResolution() {
        if (mTargetWidth == 0 && mTargetHeight == 0) {
            int val = getShortEdgeLength(mTargetResolution);
            if (mScreenRenderWidth > mScreenRenderHeight) {
                mTargetHeight = val;
            } else {
                mTargetWidth = val;
            }
        }

        if (mTargetWidth == 0) {
            mTargetWidth = mTargetHeight * mScreenRenderWidth / mScreenRenderHeight;
        } else if (mTargetHeight == 0) {
            mTargetHeight = mTargetWidth * mScreenRenderHeight / mScreenRenderWidth;
        }
        mTargetWidth = align(mTargetWidth, 8);
        mTargetHeight = align(mTargetHeight, 8);
    }

    private int getShortEdgeLength(int resolution) {
        switch (resolution) {
            case StreamerConstants.VIDEO_RESOLUTION_360P:
                return 360;
            case StreamerConstants.VIDEO_RESOLUTION_480P:
                return 480;
            case StreamerConstants.VIDEO_RESOLUTION_540P:
                return 540;
            case StreamerConstants.VIDEO_RESOLUTION_720P:
                return 720;
            default:
                return 720;
        }
    }

    private int align(int val, int align) {
        return (val + align - 1) / align * align;
    }

    public boolean startRecord(String recordUrl) {
        if (mIsRecording) {
            return false;
        }
        mIsRecording = true;
        startCapture();
        mFilePublisher.startRecording(recordUrl);
        return true;
    }

    private void startCapture() {
        if (mIsCaptureStarted) {
            return;
        }
        mIsCaptureStarted = true;

        setAudioParams();
        setRecordingParams();
        mPlayerCapture.start();
    }

    public boolean stopRecord() {
        if (!mIsRecording) {
            return false;
        }
        mIsRecording = false;

        mFilePublisher.stop();
        stopCapture();
        return true;
    }

    private void stopCapture() {
        if (!mIsCaptureStarted) {
            return;
        }
        if (mIsRecording || mIsFileRecording) {
            return;
        }
        mIsCaptureStarted = false;

        mPlayerCapture.stop();
        mVideoEncoder.stop();
        mAudioEncoderMgt.getEncoder().stop();
    }

    private void setAudioParams() {
        mAudioResampleFilter.setOutFormat(new AudioBufFormat(AVConst.AV_SAMPLE_FMT_S16,
                mAudioSampleRate, mAudioChannels));
    }

    private void setRecordingParams() {
        VideoEncodeFormat videoEncodeFormat = new VideoEncodeFormat(mVideoCodecId,
                mTargetWidth, mTargetHeight, mInitVideoBitrate);
        videoEncodeFormat.setFramerate(mTargetFps);
        videoEncodeFormat.setIframeinterval(mIFrameInterval);
        videoEncodeFormat.setScene(mEncodeScene);
        videoEncodeFormat.setProfile(mEncodeProfile);
        videoEncodeFormat.setPixFmt(ImgBufFormat.FMT_YV12);
        mVideoEncoder.configure(videoEncodeFormat);

        AudioEncodeFormat audioEncodeFormat = new AudioEncodeFormat(AudioEncodeFormat.MIME_AAC,
                AVConst.AV_SAMPLE_FMT_S16, mAudioSampleRate, mAudioChannels, mAudioBitrate);
        mAudioEncoderMgt.setEncodeFormat(audioEncodeFormat);
    }


    public void setEncodeMethod(int encodeMethod) throws IllegalStateException {
        setVideoEncodeMethod(encodeMethod);
        setAudioEncodeMethod(encodeMethod);
    }

    public void setAudioEncodeMethod(int encodeMethod) throws IllegalStateException {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }
        mAudioEncoderMgt.setEncodeMethod(encodeMethod);
    }

    public void setVideoEncodeMethod(int encodeMethod) throws IllegalStateException {
        if (mIsRecording) {
            throw new IllegalStateException("Cannot set encode method while recording");
        }
//        mVideoEncoder.setEncodeMethod(encodeMethod);
    }


    public void setTargetFps(float fps) throws IllegalArgumentException {
        if (fps <= 0) {
            throw new IllegalArgumentException("the fps must > 0");
        }
        mTargetFps = fps;
    }

    public float getTargetFps() {
        return mTargetFps;
    }

    public void setVideoBitrate(int bitrate) throws IllegalArgumentException {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("the VideoBitrate must > 0");
        }
        mInitVideoBitrate = bitrate;
        mAutoAdjustVideoBitrate = false;
    }

    public void setVideoKBitrate(int kBitrate) throws IllegalArgumentException {
        setVideoBitrate(kBitrate * 1024);
    }

    public void setVideoBitrate(int initVideoBitrate, int maxVideoBitrate, int minVideoBitrate)
            throws IllegalArgumentException {
        if (initVideoBitrate <= 0 || maxVideoBitrate <= 0 || minVideoBitrate <= 0) {
            throw new IllegalArgumentException("the VideoBitrate must > 0");
        }

        mInitVideoBitrate = initVideoBitrate;
        mMaxVideoBitrate = maxVideoBitrate;
        mMinVideoBitrate = minVideoBitrate;
        mAutoAdjustVideoBitrate = true;
    }

    public void setVideoKBitrate(int initVideoKBitrate,
                                 int maxVideoKBitrate,
                                 int minVideoKBitrate)
            throws IllegalArgumentException {
        setVideoBitrate(initVideoKBitrate * 1024,
                maxVideoKBitrate * 1024,
                minVideoKBitrate * 1024);
    }

    public int getInitVideoBitrate() {
        return mInitVideoBitrate;
    }

    public int getMinVideoBitrate() {
        return mMinVideoBitrate;
    }

    public int getMaxVideoBitrate() {
        return mMaxVideoBitrate;
    }

    public boolean isAutoAdjustVideoBitrate() {
        return mAutoAdjustVideoBitrate;
    }

    public void setVideoCodecId(int codecId) {
        mVideoCodecId = codecId;
    }

    public int getVideoCodecId() {
        return mVideoCodecId;
    }

    public void setVideoEncodeScene(int scene) {
        mEncodeScene = scene;
    }

    public int getVideoEncodeScene() {
        return mEncodeScene;
    }

    public void setVideoEncodeProfile(int profile) {
        mEncodeProfile = profile;
    }

    public int getVideoEncodeProfile() {
        return mEncodeProfile;
    }

    public void setAudioChannels(int channels) throws IllegalArgumentException {
        if (channels != 1 && channels != 2) {
            throw new IllegalArgumentException("the AudioChannels must be mono or stereo");
        }

        mAudioChannels = channels;
    }

    public void setAudioBitrate(int bitrate) throws IllegalArgumentException {
        if (bitrate <= 0) {
            throw new IllegalArgumentException("the AudioBitrate must >0");
        }

        mAudioBitrate = bitrate;
    }

    public void setAudioKBitrate(int kBitrate) throws IllegalArgumentException {
        setAudioBitrate(kBitrate * 1024);
    }

    public int getAudioBitrate() {
        return mAudioBitrate;
    }

    public int getAudioChannels() {
        return mAudioChannels;
    }

    public boolean isFileRecording() {
        return mIsFileRecording;
    }

    public interface OnInfoListener {
        void onInfo(int what, int msg1, int msg2);
    }

    public interface OnErrorListener {
        void onError(int what, int msg1, int msg2);
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        if (mIsRecording && !mIsAudioOnly) {
            getVideoEncoderMgt().stopRepeatLastFrame();
        }
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        if (mIsRecording && !mIsAudioOnly) {
            getVideoEncoderMgt().startRepeatLastFrame();
        }
    }

    public void enableDebugLog(boolean enableDebugLog) {
        mEnableDebugLog = enableDebugLog;
        StatsLogReport.getInstance().setEnableDebugLog(mEnableDebugLog);
    }

    public long getEncodedFrames() {
        return mVideoEncoder.getFrameEncoded();
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mOnInfoListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnLogEventListener(StatsLogReport.OnLogEventListener listener) {
        StatsLogReport.getInstance().setOnLogEventListener(listener);
    }


    public void release() {
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        mPlayerCapture.release();
        setOnLogEventListener(null);
    }

    public PlayerCapture getPlayerCapture() {
        return mPlayerCapture;
    }
}
