package com.ksyun.live.demo.player.activity;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ksyun.live.demo.R;
import com.ksyun.media.player.IMediaController;
import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;


public class MediaController extends FrameLayout implements IMediaController {
    private static final String TAG = "MediaController";

    private IMediaController.MediaPlayerControl  mPlayer;
    private Context             mContext;
    private View                mAnchor;
    private View                mRoot;
    private PopupWindow         mWindow;
    private SeekBar             mProgress;
    private long                mSeekposition =0;
    private TextView            mEndTime, mCurrentTime;
    private boolean             mShowing;
    private boolean             mDragging;
    private static final int    sDefaultTimeout = 3000;
    private static final int    FADE_OUT = 1;
    private static final int    SHOW_PROGRESS = 2;
    private boolean             mUseFastForward;
    private boolean             mListenersSet;
    private OnClickListener mNextListener, mPrevListener;
    StringBuilder               mFormatBuilder;
    Formatter                   mFormatter;
    private ImageButton         mPauseButton;
    private ImageButton         mFfwdButton;
    private ImageButton         mRewButton;
    private ImageButton         mNextButton;
    private ImageButton         mPrevButton;

    private Handler             mHandler = new MessageHandler(this);

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        mContext = context;
        mUseFastForward = true;
        initFloatingWindow();
        Log.i(TAG, TAG);
    }

    public MediaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        initFloatingWindow();
        Log.i(TAG, TAG);
    }

    public MediaController(Context context) {
        this(context, true);
        Log.i(TAG, TAG);
    }

    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    public void setMediaPlayer(IMediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    private void initFloatingWindow() {
        mWindow = new PopupWindow(mContext);
        mWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        mWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mWindow.setAnimationStyle(0);
    }
        /**
         * Set the view that acts as the anchor for the control view.
         * This can for example be a VideoView, or your Activity's main view.
         * @param view The view to which to anchor the controller when it is visible.
         */
    public void setAnchorView(View view) {
        mAnchor = view;

        removeAllViews();
        View v = makeControllerView();
        if (mWindow != null){
            mWindow.setContentView(v);
            mWindow.setWidth(LayoutParams.MATCH_PARENT);
            mWindow.setHeight(LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);

        }

        mRewButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);

        }

        // By default these are hidden. They will be enabled when setPrevNextListeners() is called
        mNextButton = (ImageButton) v.findViewById(R.id.next);
        if (mNextButton != null  && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) v.findViewById(R.id.prev);
        if (mPrevButton != null && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        installPrevNextListeners();
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        if (mPlayer == null) {
            return;
        }

        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            if (mWindow != null) {
                int[] location = new int[2];
                mAnchor.getLocationOnScreen(location);
                Rect anchorRect = new Rect(location[0], location[1], location[0] + mAnchor.getWidth(), location[1] + mAnchor.getHeight());

                mWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY, anchorRect.left, anchorRect.bottom);
                //mWindow.showAsDropDown(mAnchor,0,0);
                //mWindow.showAtLocation(mAnchor, Gravity.BOTTOM, 0, 0);
            }

            mShowing = true;
        }
        //updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null) {
            return;
        }

        try {
            if (mWindow != null)
                mWindow.dismiss();

            mHandler.removeMessages(SHOW_PROGRESS);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        mShowing = false;
    }

    private String stringForTime(long timeMs) {
        long totalSeconds = timeMs / 1000;

        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours   = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress( (int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return (int)position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(sDefaultTimeout);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode ==  KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                //updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                //updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private OnClickListener mFullscreenListener = new OnClickListener() {
        public void onClick(View v) {
            show(sDefaultTimeout);
        }
    };

    public void updatePausePlay() {
        if (mRoot == null || mPauseButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.ic_media_play);
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mSeekposition = newposition;
            //mPlayer.seekTo( newposition );
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime(newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            mPlayer.seekTo(mSeekposition);
            setProgress();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            if (!enabled)
                mProgress.setProgress(0);

            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onPause() {
        mPauseButton.setImageResource(R.drawable.ic_media_play);
    }

    @Override
    public void onStart() {
        mPauseButton.setImageResource(R.drawable.ic_media_pause);
    }

    private OnClickListener mRewListener = new OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            long pos = mPlayer.getCurrentPosition();
            if (pos > 5000)
                pos -= 5000; // milliseconds
            else pos = 0;
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private OnClickListener mFfwdListener = new OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            long pos = mPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            if (pos > mPlayer.getDuration())
                pos = mPlayer.getDuration();
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    public void setPrevNextListeners(OnClickListener next, OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        if (mRoot != null) {
            installPrevNextListeners();

            if (mNextButton != null) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null) {
                mPrevButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<MediaController> mView;

        MessageHandler(MediaController view) {
            mView = new WeakReference<MediaController>(view);
        }
        @Override
        public void handleMessage(Message msg) {
            MediaController view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.mDragging && view.mShowing /*&& view.mPlayer.isPlaying()*/) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }

}