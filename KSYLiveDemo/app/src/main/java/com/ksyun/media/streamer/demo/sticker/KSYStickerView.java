package com.ksyun.media.streamer.demo.sticker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 贴纸 View
 */

public class KSYStickerView extends View {
    private static String TAG = "KSYStickerView";

    private static final int STATUS_IDLE = 0;//正常
    private static final int STATUS_MOVE = 1;//移动模式
    private static final int STATUS_ROTATE = 2;//旋转模式
    private static final int STATUS_DELETE = 3;//删除模式
    private static final int STATUS_SCALE = 4;  //多点缩放状态

    private float mOldX = 0;
    private float mOldY = 0;
    private float mMultiTouchOldDist = 0;

    private int mCurrentTouchStatus = STATUS_IDLE;
    private int mTouchMode = 0;

    private boolean mNeedNotifySelected = true;

    private KSYStickerItem mCurrentItem;// 当前操作的贴图
    private int mCurrentIndex = 0;
    private LinkedHashMap<Integer, KSYStickerItem> mStickers =
            new LinkedHashMap<Integer, KSYStickerItem>();// 存贮每层贴图数据
    private int mStickerCount;

    private int mUsingStickerState = 0;

    private OnStickerStateChanged mStickerStateChanged;

    private Handler mMainHandler;
    private boolean mPreviewPaused = false;

    private boolean mIsEnableTouch = false;

    private Bitmap mResultBitmap;
    private Canvas mResultCanvas;


    public KSYStickerView(Context context) {
        super(context);
        mMainHandler = new Handler();
    }

    public KSYStickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMainHandler = new Handler();
    }

    public KSYStickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMainHandler = new Handler();
    }

    /**
     * @param bitmap
     * @param helpBoxInfo
     */
    public synchronized int addSticker(Bitmap bitmap, final StickerHelpBoxInfo helpBoxInfo) {
        KSYStickerItem item = new KSYStickerItem(helpBoxInfo);
        item.init(bitmap, this);
        //停止绘制当前的sticker的辅助区域
        if (mCurrentItem != null) {
            mCurrentItem.setDrawHelpTool(false);
        }
        //把新添加的作为当前item来处理
        mCurrentItem = item;
        mCurrentItem.setDrawHelpTool(true);
        mStickers.put(++mStickerCount, item);
        mCurrentIndex = mStickerCount;

        this.invalidate();// 重绘视图
        return mStickerCount;
    }

    public void setOnStickerSelected(OnStickerStateChanged stickerSelected) {
        mStickerStateChanged = stickerSelected;
    }

    /**
     * 隐藏当前编辑态的贴纸的辅助区域绘制
     *
     * @param drawHelpTool
     */
    public void setDrawHelpTool(boolean drawHelpTool) {
        if (mCurrentItem != null) {
            mCurrentItem.setDrawHelpTool(drawHelpTool);
        }
        this.invalidate();
    }

    /**
     * 修改贴纸信息，本接口不保留旋转、缩放、移动信息
     *
     * @param bitmap
     */
    public void updateStickerInfo(Bitmap bitmap) {
        if (mCurrentItem != null) {
            mCurrentItem.updateStickerInfo(bitmap, this);
            invalidate();
        }
    }

    /**
     * 贴纸使用状态（日志上报使用）
     *
     * @return
     * @hide
     */
    public int getStickerUsingState() {
        return mUsingStickerState;
    }

    public void release() {
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        clear();
    }

    public synchronized void removeBitmapStickers() {
        Iterator<LinkedHashMap.Entry<Integer, KSYStickerItem>> it = mStickers.entrySet().iterator();
        List<Integer> deleteIds = new ArrayList<>();
        while (it.hasNext()) {
            LinkedHashMap.Entry<Integer, KSYStickerItem> entry = it.next();
            if (entry.getKey() == mCurrentIndex) {
                mCurrentIndex = 0;
                mCurrentItem = null;
            }
            deleteIds.add(entry.getKey());
            it.remove();
        }
        this.invalidate();
        if (mStickerStateChanged != null) {
            mStickerStateChanged.deleted(deleteIds, "");
        }
    }

    public synchronized void removeSticker(int index) {
        if (mStickers.containsKey(index)) {
            String text = null;
            if (mCurrentIndex == index) {
                mCurrentIndex = 0;
                mCurrentItem = null;
            }
            mStickers.remove(index);
            if (mStickerStateChanged != null) {
                List<Integer> deleteIds = new ArrayList<>(1);
                deleteIds.add(index);
                mStickerStateChanged.deleted(deleteIds, text);
            }
            mCurrentTouchStatus = STATUS_IDLE;// 返回空闲状态
            invalidate();

            if (mStickers.size() <= 0) {
                mStickerCount = 0;
                mStickers.clear();
            }
        }
    }

    private void clear() {
        mStickerCount = 0;
        mStickers.clear();
        this.invalidate();
    }

    /**
     * draw
     *
     * @hide
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mPreviewPaused) {
            return;
        }
        for (Integer id : mStickers.keySet()) {
            KSYStickerItem item = mStickers.get(id);
            item.draw(canvas);
        }

    }

    public void draw() {
        this.invalidate();
    }

    @Override
    /**
     * @hide
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void enableTouch(boolean isEnableTouch) {
        mIsEnableTouch = isEnableTouch;
        if (mCurrentItem != null) {
            mCurrentItem.setDrawHelpTool(false);
            mCurrentItem = null;
            mCurrentIndex = 0;
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);// 是否向下传递事件标志 true为消耗
        if (!mIsEnableTouch) {
            return false;
        }

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                int deleteId = -1;
                for (Integer id : mStickers.keySet()) {
                    KSYStickerItem item = mStickers.get(id);
                    if (!item.getDraw()) {
                        continue;
                    }
                    //touch点是否在功能区域
                    if (item.getDeleteRect().contains(x, y)) {// 在删除区域，进入删除模式
                        deleteId = id;
                        mCurrentTouchStatus = STATUS_DELETE;
                    } else if (item.getRotateRect().contains(x, y)) {// 在旋转区域，进入旋转模式
                        ret = true;
                        if (mCurrentItem != null) {
                            mCurrentItem.setDrawHelpTool(false);
                        }
                        mCurrentItem = item;
                        mCurrentItem.setDrawHelpTool(true);
                        mCurrentIndex = id;
                        mCurrentTouchStatus = STATUS_ROTATE;
                        mOldX = mCurrentItem.getRotateRect().centerX();
                        mOldY = mCurrentItem.getRotateRect().centerY();
                    } else if (item.getBitRect().contains(x, y)) {// 否则进入移动模式
                        // 被选中一张贴图
                        ret = true;
                        if (mCurrentItem != null) {
                            mCurrentItem.setDrawHelpTool(false);
                        }
                        mCurrentItem = item;
                        mCurrentItem.setDrawHelpTool(true);
                        mCurrentIndex = id;
                        mCurrentTouchStatus = STATUS_MOVE;
                        mOldX = x;
                        mOldY = y;
                    }
                }

                if (!ret && mCurrentItem != null && mCurrentTouchStatus == STATUS_IDLE) {// 没有贴图被选择
                    mCurrentItem.setDrawHelpTool(false);
                    mCurrentItem = null;
                    mCurrentIndex = 0;
                    invalidate();
                }

                if (deleteId > 0 && mCurrentTouchStatus == STATUS_DELETE) {// 删除选定贴图
                    removeSticker(deleteId);
                    ret = true;
                }

                if (mCurrentItem != null && mCurrentTouchStatus == STATUS_MOVE) {
                    mTouchMode = 1;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mTouchMode += 1;
                if (mTouchMode >= 2 && mCurrentTouchStatus == STATUS_MOVE) {
                    mCurrentTouchStatus = STATUS_SCALE;
                    mMultiTouchOldDist = spacing(event);
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchMode -= 1;
                if (mTouchMode < 2 && mCurrentTouchStatus == STATUS_SCALE) {
                    mCurrentTouchStatus = STATUS_MOVE;
                    mOldX = x;
                    mOldY = y;
                    mMultiTouchOldDist = 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                ret = true;
                if (mCurrentTouchStatus == STATUS_MOVE) {// 移动贴图
                    float dx = x - mOldX;
                    float dy = y - mOldY;
                    if (mCurrentItem != null) {
                        mCurrentItem.updatePos(dx, dy);
                        invalidate();
                    }
                    mOldX = x;
                    mOldY = y;
                } else if (mCurrentTouchStatus == STATUS_ROTATE) {// 旋转 缩放图片操作
                    float dx = x - mOldX;
                    float dy = y - mOldY;
                    if (mCurrentItem != null) {
                        mCurrentItem.updateRotateAndScale(dx, dy);// 旋转
                        invalidate();
                    }
                    mOldX = x;
                    mOldY = y;
                } else if (mTouchMode >= 2 && mCurrentTouchStatus == STATUS_SCALE) { //缩放图片
                    float newDist = spacing(event);
                    boolean needScale = false;
                    if (newDist > mMultiTouchOldDist + 1) {
                        needScale = true;
                    }

                    if (newDist < mMultiTouchOldDist - 1) {
                        needScale = true;
                    }
                    if (needScale && mCurrentItem != null) {
                        mCurrentItem.updateScale(newDist / mMultiTouchOldDist);
                        mMultiTouchOldDist = newDist;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ret = false;
                //除删除模式以外，其它模式都需要通知上层某一个贴纸被编辑了
                if (mNeedNotifySelected && mCurrentItem != null) {
                    if (mStickerStateChanged != null) {
                        mStickerStateChanged.selected(mCurrentIndex, "");
                    }
                }
                mTouchMode = 0;
                mNeedNotifySelected = true;
                mCurrentTouchStatus = STATUS_IDLE;
                break;
        }
        return ret;
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    public Bitmap getBitmap() {
        Collection<KSYStickerItem> items = mStickers.values();
        if (mResultBitmap == null || mResultCanvas == null|| mResultBitmap.getWidth() != this.getWidth()
                || mResultBitmap.getHeight() != this.getHeight() || mResultBitmap.isRecycled()) {
            if(mResultBitmap != null && !mResultBitmap.isRecycled()){
                mResultBitmap.recycle();
            }
            mResultBitmap = Bitmap.createBitmap(this.getWidth(), this.getHeight(), Bitmap.Config.ARGB_8888);
            mResultCanvas = new Canvas(mResultBitmap);
        }
        mResultCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        for (KSYStickerItem item : items) {
            item.drawSticker(mResultCanvas);
        }
        return mResultBitmap;
    }


    private boolean saveBitmap(Bitmap bm, String filePath) {
        File f = new File(filePath);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @hide
     */
    public interface OnStickerStateChanged {
        void selected(int index, String text);

        void deleted(List<Integer> index, String text);
    }
}
