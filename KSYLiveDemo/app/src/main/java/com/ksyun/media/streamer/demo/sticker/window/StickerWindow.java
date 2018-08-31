package com.ksyun.media.streamer.demo.sticker.window;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import com.ksyun.live.demo.R;
import com.ksyun.media.streamer.capture.WaterMarkCapture;
import com.ksyun.media.streamer.demo.sticker.KSYStickerView;
import com.ksyun.media.streamer.demo.sticker.StickerHelpBoxInfo;
import com.ksyun.media.streamer.kit.KSYStreamer;

import java.io.IOException;
import java.io.InputStream;


/**
 * @Author: [xiaoqiang]
 * @Description: [贴纸展示window ]
 * @CreateDate: [2018/3/20]
 * @UpdateDate: [2018/3/20]
 * @UpdateUser: [xiaoqiang]
 * @UpdateRemark: []
 */

public class StickerWindow extends FrameLayout {

    private static final String TAG = "StickerWindow";

    public final static String STATIC_STICKER = "Stickers";  //贴纸加载地址默认在Assets目录，如果修改加载地址需要修改StickerAdapter的图片加载

    private RecyclerView mStickerList;// 图片贴纸素材列表
    private StickerAdapter mStickerAdapter;// 图片贴纸列表适配器
    private KSYStickerView mKSYStickerView;  //贴纸预览区域（图片贴纸和字幕贴纸公用）
    private Bitmap mStickerDeleteBitmap;  //贴纸辅助区域的删除按钮（图片贴纸和字幕贴纸公用）
    private Bitmap mStickerRotateBitmap;  //贴纸辅助区域的旋转按钮（图片贴纸和字幕贴纸公用）
    private StickerHelpBoxInfo mStickerHelpBoxInfo;  //贴纸辅助区域的画笔（图片贴纸和字幕贴纸公用）
    private View mStickerRoot;

    private Context mContext;

    private WaterMarkCapture mStickerCapture;
    private int mIdxSticker = 5;  // 0-2 SDK内部使用，3-4 水印和附加水印(WaterMarkFragment)

    private Bitmap mLogoBitmap;

    private KSYStreamer mKSYStream;

    public StickerWindow(@NonNull Context context) {
        this(context, null);
    }

    public StickerWindow(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickerWindow(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        addView(loadView());
        initData();
    }


    private View loadView() {
        View view = View.inflate(mContext, R.layout.sticker_window, null);
        mStickerList = (RecyclerView) view.findViewById(R.id.stickers_list);
        mKSYStickerView = view.findViewById(R.id.sticker_view);
        mStickerRoot = view.findViewById(R.id.sticker_choose);
        mStickerRoot.setVisibility(GONE);
        mStickerRoot.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mKSYStickerView.enableTouch(false);
        LinearLayoutManager stickerListLayoutManager = new LinearLayoutManager(mContext);
        stickerListLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mStickerList.setLayoutManager(stickerListLayoutManager);
        mStickerAdapter = new StickerAdapter(mContext);
        mStickerList.setAdapter(mStickerAdapter);
        return view;
    }

    private void initData() {
        //Adapter中设置贴纸的路径，默认支持的是assets目录下面的，其它目录需要自行修改Window
        mStickerAdapter.addStickerImages(STATIC_STICKER);
        mStickerAdapter.setOnStickerItemClick(mOnStickerItemClick);
    }


    public void bindKSYStreamer(KSYStreamer streamer) {
        this.mKSYStream = streamer;
        mStickerCapture = new WaterMarkCapture(streamer.getGLRender());
        mStickerCapture.getLogoTexSrcPin().connect(mKSYStream.getImgTexMixer().getSinkPin(mIdxSticker));
        mStickerCapture.setTargetSize(mKSYStream.getTargetWidth(), mKSYStream.getTargetHeight());
        mStickerCapture.setPreviewSize(mKSYStream.getPreviewWidth(), mKSYStream.getPreviewHeight());

    }

    public void showSticker() {
        mLogoBitmap = mKSYStickerView.getBitmap();
        if (mStickerCapture != null && mLogoBitmap != null) {
            mStickerCapture.setTargetSize(mKSYStream.getTargetWidth(), mKSYStream.getTargetHeight());
            mStickerCapture.setPreviewSize(mKSYStream.getPreviewWidth(), mKSYStream.getPreviewHeight());
            mStickerCapture.showLogo(mLogoBitmap, 1, 1);
        }else{
            hideSticker();
        }
    }

    public void hideSticker() {
        if (mStickerCapture != null) {
            mStickerCapture.hideLogo();
        }
    }

    public void showEditSticker(boolean isEdit) {
        mStickerRoot.setVisibility(isEdit?VISIBLE:GONE);
        mKSYStickerView.enableTouch(isEdit);
    }

    public boolean isEdit() {
        return mStickerRoot.getVisibility() == VISIBLE;
    }

    public void unBindKSYStreamer() {
        mKSYStream = null;
        if (mStickerCapture != null) {
            mStickerCapture.release();
        }
        mStickerCapture = null;
    }

    private StickerAdapter.OnStickerItemClick mOnStickerItemClick = new StickerAdapter.OnStickerItemClick() {
        @Override
        public void selectedStickerItem(String path) {
            if (path.contains("0")) {
                //删除所有图片贴纸
                mKSYStickerView.removeBitmapStickers();
                showEditSticker(false);
                showSticker();
                return;
            }
            initStickerHelpBox();
            //添加一个贴纸
            mKSYStickerView.addSticker(getImageFromAssetsFile(path), mStickerHelpBoxInfo);
        }
    };

    /**
     * 贴纸的辅助区域
     */
    private void initStickerHelpBox() {
        if (mStickerDeleteBitmap == null) {
            mStickerDeleteBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.sticker_delete);
        }

        if (mStickerRotateBitmap == null) {
            mStickerRotateBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.sticker_rotate);
        }

        if (mStickerHelpBoxInfo == null) {
            mStickerHelpBoxInfo = new StickerHelpBoxInfo();
            mStickerHelpBoxInfo.deleteBit = mStickerDeleteBitmap;
            mStickerHelpBoxInfo.rotateBit = mStickerRotateBitmap;
            Paint helpBoxPaint = new Paint();
            helpBoxPaint.setColor(Color.BLACK);
            helpBoxPaint.setStyle(Paint.Style.STROKE);
            helpBoxPaint.setAntiAlias(true);
            helpBoxPaint.setStrokeWidth(4);
            mStickerHelpBoxInfo.helpBoxPaint = helpBoxPaint;
        }
    }

    /**
     * 从Assert文件夹中读取位图数据
     *
     * @param fileName
     * @return
     */
    private Bitmap getImageFromAssetsFile(String fileName) {
        Bitmap image = null;
        AssetManager am = mContext.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
