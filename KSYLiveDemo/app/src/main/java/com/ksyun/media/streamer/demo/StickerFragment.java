package com.ksyun.media.streamer.demo;

import com.ksyun.live.demo.R;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.ksyun.media.streamer.demo.sticker.window.StickerWindow;
import com.ksyun.media.streamer.kit.KSYStreamer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Fragment for Sticker add.
 */
public class StickerFragment extends Fragment {

    protected Unbinder mUnbinder;

    @BindView(R.id.cb_sticker)
    protected CheckBox mShowCheckBox;
    @BindView(R.id.tv_sticker)
    protected TextView mStickerButton;
    protected StickerWindow mStickerWindow;
    protected StdCameraActivity mActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sticker_fragment, container, false);
        mUnbinder = ButterKnife.bind(this, view);
        mActivity = (StdCameraActivity) getActivity();
        mStickerWindow = mActivity.mStickerWindow;
        mStickerWindow.bindKSYStreamer(mActivity.mStreamer);
        return view;
    }

    @OnCheckedChanged(R.id.cb_sticker)
    protected void onStickerChecked(boolean isChecked) {
        mStickerButton.setEnabled(isChecked);
        mStickerWindow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        if (isChecked) {
            mStickerWindow.showEditSticker(false);
            mStickerWindow.showSticker();
        } else {
            mStickerWindow.hideSticker();
        }
    }

    @OnClick(R.id.tv_sticker)
    protected void onStickerClick() {
        mStickerWindow.showEditSticker(mShowCheckBox.isChecked());
    }


    public boolean onBackPressed() {
        if (mStickerWindow.isEdit()) {
            mStickerWindow.showEditSticker(false);
            mStickerWindow.showSticker();
            return true;
        }
        return false;
    }

    protected void onViewSizeChanged() {
        if (mShowCheckBox.isChecked()) {
            mStickerWindow.hideSticker();
            mStickerWindow.showSticker();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mStickerWindow != null) {
            mStickerWindow.unBindKSYStreamer();
        }
        mUnbinder.unbind();
    }
}
