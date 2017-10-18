package com.ksyun.media.streamer.demo;

import com.ksyun.live.demo.R;
/**
 * Float streaming demo fragment.
 */

public class FloatDemoFragment extends BaseDemoFragment {
    private static final String TAG = "FloatDemoFragment";

    @Override
    public void start(String url) {
        BaseCameraActivity.BaseStreamConfig config = new BaseCameraActivity.BaseStreamConfig();
        loadParams(config, url);
        FloatCameraActivity.startActivity(getActivity(), config, FloatCameraActivity.class);
    }
}
