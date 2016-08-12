package com.ksyun.live.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.ksy.recordlib.service.core.KSYStreamerConfig;
import com.ksy.recordlib.service.streamer.RecorderConstants;

public class StreamActivity extends AppCompatActivity implements View.OnClickListener,RadioGroup.OnCheckedChangeListener{
    private Spinner spinner_resolution;
    private String[] strs = {Settings.RESOLUTION360,Settings.RESOLUTION480,Settings.RESOLUTION540,Settings.RESOLUTION720};

    private Spinner spinner_audiorateinhz;

    private String[] strs2 = {Settings.AUDIORATEINHZ44100,Settings.AUDIORATEINHZ32000,Settings.AUDIORATEINHZ22050};

    private EditText edit_rtmpurl;
    private EditText edit_frameRatePicker;
    private EditText edit_videoBitratePicker;
    private EditText edit_audioBitratePicker;

    private Button btn_videoBitrate;
    private Button btn_audioBitrate;
    private Button btn_start;
    private Button btn_beauty;
    private Button btn_pip;
    private Button btn_audio;
    private Button btn_water;
    private Button btn_mix;
    private Button btn_other;

    private RadioGroup group_orientation;
    private RadioGroup group_encode;
    private RadioButton radio_orientationbutton1;
    private RadioButton radio_orientationbutton2;
    private RadioButton radio_encode_hw;
    private RadioButton radio_encode_sw;

    private String resolution;
    private String rtmpurl;
    private String frameRatePicker;
    private String videoBitratePicker;
    private String audioBitratePicker;
    private String encode;
    private String orientation;
    private String audiorateinhz;


    private SharedPreferences settings;
    private SharedPreferences.Editor editor;


    private int[] location = new int[2];
    private PopupWindow pop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.popupwindow,null);
        pop = new PopupWindow(view, ActionBar.LayoutParams.MATCH_PARENT, 600,false);

        ColorDrawable cd = new ColorDrawable(0xb0000000);
        pop.setBackgroundDrawable(cd);
        pop.setFocusable(true);

        spinner_resolution = (Spinner)findViewById(R.id.spinner_resolution);
        spinner_audiorateinhz = (Spinner)findViewById(R.id.spinner_audiorateinhz);

        edit_audioBitratePicker = (EditText)findViewById(R.id.edit_audioBitratePicker);
        edit_rtmpurl = (EditText)findViewById(R.id.edit_rtmpurl);
        edit_frameRatePicker = (EditText)findViewById(R.id.edit_frameRatePicker);
        edit_videoBitratePicker = (EditText) findViewById(R.id.edit_videoBitratePicker);

        btn_videoBitrate = (Button)findViewById(R.id.btn_videoBitrate);
        btn_audioBitrate = (Button)findViewById(R.id.btn_audioBitrate);
        btn_start = (Button)findViewById(R.id.btn_start);
        btn_beauty = (Button)view.findViewById(R.id.btn_beauty);
        btn_pip = (Button)view.findViewById(R.id.btn_pip);
        btn_audio = (Button)view.findViewById(R.id.btn_audio);
        btn_water = (Button)view.findViewById(R.id.btn_water);
        btn_mix = (Button)view.findViewById(R.id.btn_mix);
        btn_other = (Button)view.findViewById(R.id.btn_other);


        group_encode = (RadioGroup)findViewById(R.id.group_encode);
        group_orientation = (RadioGroup)findViewById(R.id.group_orientation);
        radio_encode_hw = (RadioButton)findViewById(R.id.radio_encode_hw);
        radio_encode_sw = (RadioButton)findViewById(R.id.radio_encode_sw);
        radio_orientationbutton1 = (RadioButton)findViewById(R.id.radio_orientationbutton1);
        radio_orientationbutton2 = (RadioButton)findViewById(R.id.radio_orientationbutton2);


        settings = getSharedPreferences("SETTINGS", Context.MODE_PRIVATE);
        editor = settings.edit();

        resolution = settings.getString("resolution","信息为空");
        rtmpurl = settings.getString("rtmpurl","信息为空");
        frameRatePicker = settings.getString("frameRatePicker","信息为空");
        videoBitratePicker = settings.getString("videoBitratePicker","信息为空");
        audioBitratePicker = settings.getString("audioBitratePicker","信息为空");
        encode = settings.getString("encode","信息为空");
        orientation = settings.getString("orientation","信息为空");
        audiorateinhz = settings.getString("audiorateinhz","信息为空");


        btn_audioBitrate.setOnClickListener(this);
        btn_videoBitrate.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_beauty.setOnClickListener(this);
        btn_pip.setOnClickListener(this);
        btn_audio.setOnClickListener(this);
        btn_mix.setOnClickListener(this);
        btn_water.setOnClickListener(this);
        btn_other.setOnClickListener(this);

        group_encode.setOnCheckedChangeListener(this);
        group_orientation.setOnCheckedChangeListener(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item,strs);
        spinner_resolution.setAdapter(adapter);
        spinner_resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                resolution = strs[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item,strs2);
        spinner_audiorateinhz.setAdapter(adapter2);
        spinner_audiorateinhz.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                audiorateinhz = strs2[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        initSetting(resolution,rtmpurl,frameRatePicker,videoBitratePicker,audioBitratePicker,encode,orientation,audiorateinhz);
    }

    @Override
    protected void onPause() {
        super.onPause();
        rtmpurl = edit_rtmpurl.getText().toString();
        frameRatePicker = edit_frameRatePicker.getText().toString();
        videoBitratePicker = edit_videoBitratePicker.getText().toString();
        audioBitratePicker = edit_audioBitratePicker.getText().toString();

        editor.putString("rtmpurl",rtmpurl);
        editor.putString("resolution",resolution);
        editor.putString("frameRatePicker",frameRatePicker);
        editor.putString("videoBitratePicker",videoBitratePicker);
        editor.putString("audioBitratePicker",audioBitratePicker);
        editor.putString("encode",encode);
        editor.putString("orientation",orientation);
        editor.putString("audiorateinhz",audiorateinhz);
        editor.commit();
    }

    private void initSetting(String resolution, String rtmpurl, String frameRatePicker, String videoBitratePicker,
                             String audioBitratePicker, String encode, String orientation, String audiorateinhz) {
        if (resolution.equals(Settings.RESOLUTION360)){
            spinner_resolution.setSelection(0);
        }else if(resolution.equals(Settings.RESOLUTION480)){
            spinner_resolution.setSelection(1);
        }else if(resolution.equals(Settings.RESOLUTION540)){
            spinner_resolution.setSelection(2);
        }else if(resolution.equals(Settings.RESOLUTION720)){
            spinner_resolution.setSelection(3);
        }else{
            spinner_resolution.setSelection(3);
            this.resolution = Settings.RESOLUTION720;
        }

        if (audiorateinhz.equals(Settings.AUDIORATEINHZ44100)){
            spinner_audiorateinhz.setSelection(0);
        }else if(audiorateinhz.equals(Settings.AUDIORATEINHZ32000)){
            spinner_audiorateinhz.setSelection(1);
        }else if(audiorateinhz.equals(Settings.AUDIORATEINHZ22050)){
            spinner_audiorateinhz.setSelection(2);
        }else{
            spinner_audiorateinhz.setSelection(0);
            this.audiorateinhz = Settings.AUDIORATEINHZ44100;
        }

        if(!rtmpurl.equals("信息为空")){
            edit_rtmpurl.setText(rtmpurl);
        }
        if(!frameRatePicker.equals("信息为空")){
            edit_frameRatePicker.setText(frameRatePicker);
        }
        if(!videoBitratePicker.equals("信息为空")){
            edit_videoBitratePicker.setText(videoBitratePicker);
        }
        if(!audioBitratePicker.equals("信息为空")){
            edit_audioBitratePicker.setText(audioBitratePicker);
        }

        if(encode.equals(Settings.ENCODE_HW)){
            group_encode.check(radio_encode_hw.getId());
        }else if(encode.equals(Settings.ENCODE_SW)){
            group_encode.check(radio_encode_sw.getId());
        }else{
            group_encode.check(radio_encode_hw.getId());
            this.encode = Settings.ENCODE_HW;
            editor.putString("encode",encode);
        }

        if(orientation.equals(Settings.ORIENTATION1)){
            group_orientation.check(radio_orientationbutton1.getId());
        }else if(encode.equals(Settings.ORIENTATION2)){
            group_orientation.check(radio_orientationbutton2.getId());
        }else{
            group_orientation.check(radio_orientationbutton2.getId());
            this.orientation = Settings.ORIENTATION2;
            editor.putString("orientation",orientation);
        }
    }
    @Override
    public void onClick(View view) {

        int frameRate = 0;
        int videoBitRate = 0;
        int audioBitRate = 0;
        int videoResolution = 0;
        int audiohz = 0;
        boolean encodeWithHEVC = false;
        boolean landscape = false;
        boolean startAuto = false;
        boolean isFrontCameraMirror = false;
        KSYStreamerConfig.ENCODE_METHOD encode_method = KSYStreamerConfig.ENCODE_METHOD.SOFTWARE;
        if (!TextUtils.isEmpty(edit_rtmpurl.getText())
                && edit_rtmpurl.getText().toString().startsWith("rtmp")) {
            if (!TextUtils.isEmpty(edit_frameRatePicker.getText().toString())) {
                frameRate = Integer.parseInt(edit_frameRatePicker.getText().toString());
            }

            if (!TextUtils.isEmpty(edit_videoBitratePicker.getText().toString())) {
                videoBitRate = Integer.parseInt(edit_videoBitratePicker.getText().toString());
            }

            if (!TextUtils.isEmpty(edit_audioBitratePicker.getText().toString())) {
                audioBitRate = Integer.parseInt(edit_audioBitratePicker.getText().toString());
            }

            if (resolution.equals(Settings.RESOLUTION360)) {
                videoResolution = RecorderConstants.VIDEO_RESOLUTION_360P;
            } else if (resolution.equals(Settings.RESOLUTION480)) {
                videoResolution = RecorderConstants.VIDEO_RESOLUTION_480P;
            } else if (resolution.equals(Settings.RESOLUTION540)) {
                videoResolution = RecorderConstants.VIDEO_RESOLUTION_540P;
            } else {
                videoResolution = RecorderConstants.VIDEO_RESOLUTION_720P;
            }
            encodeWithHEVC = false;
            if (encode.equals(Settings.ENCODE_HW)) {
                encode_method = KSYStreamerConfig.ENCODE_METHOD.HARDWARE;
            } else {
                encode_method = KSYStreamerConfig.ENCODE_METHOD.SOFTWARE;
            }

            if (orientation.equals(Settings.ORIENTATION1)) {
                landscape = true;
            } else {
                landscape = false;
            }
            audiohz = Integer.parseInt(audiorateinhz);

        }


        switch (view.getId()){
            case R.id.btn_videoBitrate:
                if(resolution.equals(Settings.RESOLUTION360)){
                    edit_videoBitratePicker.setText("400");
                }else if(resolution.equals(Settings.RESOLUTION480)){
                    edit_videoBitratePicker.setText("600");
                }else if(resolution.equals(Settings.RESOLUTION540)){
                    edit_videoBitratePicker.setText("700");
                }else if(resolution.equals(Settings.RESOLUTION720)){
                    edit_videoBitratePicker.setText("1000");
                }
                break;
            case R.id.btn_audioBitrate:
                if(encode.equals(Settings.ENCODE_HW)){
                    edit_audioBitratePicker.setText("48");
                }else if(encode.equals(Settings.ENCODE_SW)){
                    if (audiorateinhz.equals(Settings.AUDIORATEINHZ44100)){
                        edit_audioBitratePicker.setText("32");
                    }else if(audiorateinhz.equals(Settings.AUDIORATEINHZ32000)){
                        edit_audioBitratePicker.setText("24");
                    }else if(audiorateinhz.equals(Settings.AUDIORATEINHZ22050)){
                        edit_audioBitratePicker.setText("16");
                    }
                }
                break;
            case R.id.btn_start:
                rtmpurl = edit_rtmpurl.getText().toString();
                frameRatePicker = edit_frameRatePicker.getText().toString();
                videoBitratePicker = edit_videoBitratePicker.getText().toString();
                audioBitratePicker = edit_audioBitratePicker.getText().toString();
                editor.putString("rtmpurl",rtmpurl);
                editor.putString("resolution",resolution);
                editor.putString("frameRatePicker",frameRatePicker);
                editor.putString("videoBitratePicker",videoBitratePicker);
                editor.putString("audioBitratePicker",audioBitratePicker);
                editor.putString("encode",encode);
                editor.putString("orientation",orientation);
                editor.putString("audiorateinhz",audiorateinhz);
                editor.commit();
                Log.v("MainSettings",rtmpurl);
                Log.v("MainSettings",resolution);
                Log.v("MainSettings",frameRatePicker);
                Log.v("MainSettings",videoBitratePicker);
                Log.v("MainSettings",encode);
                Log.v("MainSettings",audioBitratePicker);
                Log.v("MainSettings",orientation);
                Log.v("MainSettings",audiorateinhz);

                if(pop.isShowing()){
                    pop.dismiss();
                }else{
                    view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int popupWidth = view.getMeasuredWidth();
                    view.getLocationOnScreen(location);
                    pop.showAtLocation(view, Gravity.NO_GRAVITY,(location[0] + view.getWidth() / 2) - popupWidth / 2, location[1] - 600);
                }
                btn_start.setText("直播");
                break;
            case R.id.btn_beauty:
                CameraActivityBeauty.startActivity(getApplicationContext(), 0, edit_rtmpurl.getText().toString(),
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto, true, true,audiohz);
                break;
            case R.id.btn_pip:
                CameraActivityPip.startActivity(getApplicationContext(), 0, edit_rtmpurl.getText().toString(),
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto, true, true,audiohz);
                break;
            case R.id.btn_audio:
                CameraActivityAudioEffect.startActivity(getApplicationContext(), 0, edit_rtmpurl.getText().toString(),
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto, true, true,audiohz);
                break;
            case R.id.btn_water:
                CameraActivityWaterLogo.startActivity(getApplicationContext(), 0, rtmpurl,
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto,true, true,audiohz);
                break;
            case R.id.btn_mix:
                CameraActivityAudioMix.startActivity(getApplicationContext(), 0, edit_rtmpurl.getText().toString(),
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto, true, true,audiohz);

                break;
            case R.id.btn_other:
                CameraActivityOther.startActivity(getApplicationContext(), 0, edit_rtmpurl.getText().toString(),
                        frameRate, videoBitRate, audioBitRate, videoResolution, landscape, encode_method, startAuto,true, true,audiohz);
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch(i){
            case R.id.radio_encode_hw:
                encode = Settings.ENCODE_HW;
                spinner_audiorateinhz.setSelection(0);
                spinner_audiorateinhz.setEnabled(false);
                break;
            case R.id.radio_encode_sw:
                spinner_audiorateinhz.setEnabled(true);
                encode = Settings.ENCODE_SW;
                break;
            case R.id.radio_orientationbutton1:
                orientation = Settings.ORIENTATION1;
                break;
            case R.id.radio_orientationbutton2:
                orientation = Settings.ORIENTATION2;
                break;
            default:
                break;
        }
    }
}
