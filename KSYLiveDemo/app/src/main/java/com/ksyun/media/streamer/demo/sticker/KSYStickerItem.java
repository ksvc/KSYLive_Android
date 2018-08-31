package com.ksyun.media.streamer.demo.sticker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/**
 * for sticker draw
 */

public class KSYStickerItem {
    private static String TAG = "KSYStickerItem";
    private static final int HELP_BOX_PAD = 25;
    private static final int BUTTON_WIDTH = 30;

    private View mParentView;

    private Paint mHelpPaint;
    private RectF mHelpBoxRect = new RectF();  //辅助区域的矩形信息
    private RectF mDeleteRect = new RectF();//删除按钮位置
    private RectF mRotateRect = new RectF();//旋转按钮位置
    private Rect mHelpToolsRect;  //辅助按钮的大小信息
    private RectF mDeleteDstRect = new RectF();
    private RectF mRotateDstRect = new RectF();

    private Bitmap mDeleteBitmap;
    private Bitmap mRotateBitmap;

    private float mRotateAngle = 0;
    private float mScale = 1;
    private boolean mIsDrawHelpBox = true;
    private boolean mIsDraw = true;  // 当前贴纸是否需要绘制

    // for image sticker
    private Bitmap mSrcBitmap;   //图片
    private RectF mSrcRect;
    private RectF mInitSrcRect;
    private Matrix mSrcMatrix;   //图片的Matrix信息

    public KSYStickerItem(final StickerHelpBoxInfo helpBoxInfo) {
        if (helpBoxInfo != null) {
            mDeleteBitmap = helpBoxInfo.deleteBit;
            mRotateBitmap = helpBoxInfo.rotateBit;

            mHelpPaint = helpBoxInfo.helpBoxPaint;
        }

        if (mHelpPaint == null) {
            mHelpPaint = new Paint();
            mHelpPaint.setColor(Color.BLACK);
            mHelpPaint.setStyle(Paint.Style.STROKE);
            mHelpPaint.setAntiAlias(true);
            mHelpPaint.setStrokeWidth(4);
        }
    }

    private void init(Bitmap bitmap) {
        intStaticImage(bitmap);
    }

    /**
     * 初始化静态图片贴纸的必要信息
     *
     * @param bitmap
     */
    private void intStaticImage(Bitmap bitmap) {
        mSrcBitmap = bitmap;
        mSrcMatrix = new Matrix();
    }

    private void initHelpBoxTool() {
        //辅助区域初始化
        mHelpToolsRect = new Rect(0, 0, mDeleteBitmap.getWidth(),
                mDeleteBitmap.getHeight());

        //辅助区域删除按钮
        if (mDeleteBitmap != null) {
            mDeleteRect = new RectF(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH, mHelpBoxRect.left + BUTTON_WIDTH, mHelpBoxRect.top
                    + BUTTON_WIDTH);
            mDeleteDstRect = new RectF(mDeleteRect);
        }
        //辅助区域旋转按钮
        if (mRotateBitmap != null) {
            mRotateRect = new RectF(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH, mHelpBoxRect.right + BUTTON_WIDTH, mHelpBoxRect.bottom
                    + BUTTON_WIDTH);
            mRotateDstRect = new RectF(mRotateRect);
        }
    }

    private void initDrawInfo() {
        //初始化位置信息
        mRotateAngle = 0;
        mScale = 1;
        if (mSrcBitmap != null) {
            //图片的大小需要小于预览的一半
            int bitWidth = Math.min(mSrcBitmap.getWidth(), mParentView.getMeasuredWidth() >> 1);
            int bitHeight = bitWidth * mSrcBitmap.getHeight() / mSrcBitmap.getWidth();
            //图片位于预览的最中间位置
            int left = (mParentView.getMeasuredWidth() >> 1) - (bitWidth >> 1);
            int top = (mParentView.getMeasuredHeight() >> 1) - (bitHeight >> 1);
            //图片的初始显示矩形
            mSrcRect = new RectF(left, top, left + bitWidth, top + bitHeight);
            mInitSrcRect = new RectF(mSrcRect);
            mSrcMatrix.postTranslate(this.mSrcRect.left, this.mSrcRect.top);
            //存储图片的缩放信息
            mSrcMatrix.postScale((float) bitWidth / mSrcBitmap.getWidth(),
                    (float) bitHeight / mSrcBitmap.getHeight(), mSrcRect.left,
                    mSrcRect.top);
            //有图片时辅助区域以图片为准
            mHelpBoxRect = new RectF(mSrcRect);
            updateHelpBoxRect();

            mHelpToolsRect = new Rect(0, 0, mDeleteBitmap.getWidth(),
                    mDeleteBitmap.getHeight());

            //辅助区域删除按钮
            if (mDeleteBitmap != null) {
                mDeleteRect = new RectF(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                        - BUTTON_WIDTH, mHelpBoxRect.left + BUTTON_WIDTH, mHelpBoxRect.top
                        + BUTTON_WIDTH);
                mDeleteDstRect = new RectF(mDeleteRect);
            }

            //辅助区域旋转按钮
            if (mRotateBitmap != null) {
                mRotateRect = new RectF(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                        - BUTTON_WIDTH, mHelpBoxRect.right + BUTTON_WIDTH, mHelpBoxRect.bottom
                        + BUTTON_WIDTH);
                mRotateDstRect = new RectF(mRotateRect);
            }
        } else {
            //初始位置在屏幕中心点
            int left = (mParentView.getMeasuredWidth() >> 1);
            int top = (mParentView.getMeasuredHeight() >> 1);

            mHelpBoxRect = new RectF(left, top, 0, 0);
        }

        initHelpBoxTool();
    }

    public void init(Bitmap bitmap, View parentView) {
        mParentView = parentView;
        init(bitmap);
        initDrawInfo();
    }

    public void setDrawHelpTool(boolean draw) {
        mIsDrawHelpBox = draw;
    }

    public void setDraw(boolean draw) {
        mIsDraw = draw;
    }

    public boolean getDraw() {
        return mIsDraw;
    }

    /**
     * draw sticker
     *
     * @param canvas
     */
    public void draw(Canvas canvas) {
        if (!mIsDraw) {
            return;
        }
        drawContent(canvas);
    }

    /**
     * update pos
     *
     * @param dx
     * @param dy
     */
    public void updatePos(float dx, float dy) {
        if (mSrcBitmap != null) {
            mSrcMatrix.postTranslate(dx, dy);// 记录到矩阵中
            mSrcRect.offset(dx, dy);
            mInitSrcRect.offset(dx, dy);
        }

        // 工具按钮随之移动
        mHelpBoxRect.offset(dx, dy);
        mDeleteRect.offset(dx, dy);
        mRotateRect.offset(dx, dy);

        mDeleteDstRect.offset(dx, dy);
        mRotateDstRect.offset(dx, dy);
    }

    /**
     * 旋转 缩放 更新
     *
     * @param dx
     * @param dy
     */
    public void updateRotateAndScale(final float dx, final float dy) {
        float c_x = mHelpBoxRect.centerX();
        float c_y = mHelpBoxRect.centerY();

        float x = mRotateDstRect.centerX();
        float y = mRotateDstRect.centerY();

        float n_x = x + dx;
        float n_y = y + dy;

        float xa = x - c_x;
        float ya = y - c_y;

        float xb = n_x - c_x;
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        float scale = curLen / srcLen;// 计算缩放比

        mScale *= scale;
        float newWidth = mHelpBoxRect.width() * scale;

        if (newWidth < 70) {
            mScale /= scale;
            return;
        }

        if (mSrcBitmap != null) {
            mSrcMatrix.postScale(scale, scale, mSrcRect.centerX(),
                    mSrcRect.centerY());// 存入scale矩阵
            RectUtil.scaleRect(mSrcRect, scale);// 缩放目标矩形

            mHelpBoxRect.set(mSrcRect);
            updateHelpBoxRect();// 重新计算
            mRotateRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            mRotateDstRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteDstRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            double cos = (xa * xb + ya * yb) / (srcLen * curLen);
            if (cos > 1 || cos < -1)
                return;
            float angle = (float) Math.toDegrees(Math.acos(cos));
            float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

            int flag = calMatrix > 0 ? 1 : -1;
            angle = flag * angle;

            mRotateAngle += angle;

            mSrcMatrix.postRotate(angle, mSrcRect.centerX(),
                    mSrcRect.centerY());

            RectUtil.rotateRect(mDeleteDstRect, mSrcRect.centerX(),
                    mSrcRect.centerY(), mRotateAngle);
            RectUtil.rotateRect(mRotateDstRect, mSrcRect.centerX(),
                    mSrcRect.centerY(), mRotateAngle);
        } else {
            RectUtil.scaleRect(mHelpBoxRect, scale);// 缩放目标矩形
            mRotateRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            mRotateDstRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteDstRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            double cos = (xa * xb + ya * yb) / (srcLen * curLen);
            if (cos > 1 || cos < -1)
                return;
            float angle = (float) Math.toDegrees(Math.acos(cos));
            float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

            int flag = calMatrix > 0 ? 1 : -1;
            angle = flag * angle;

            mRotateAngle += angle;

            RectUtil.rotateRect(mDeleteDstRect, mHelpBoxRect.centerX(),
                    mHelpBoxRect.centerY(), mRotateAngle);
            RectUtil.rotateRect(mRotateDstRect, mHelpBoxRect.centerX(),
                    mHelpBoxRect.centerY(), mRotateAngle);

        }
    }

    public void drawSticker(Canvas canvas) {
        drawSticker(canvas, mScale, mRotateAngle);
    }

    public void updateScale(float scale) {
        mScale *= scale;
        float newWidth = mHelpBoxRect.width() * scale;

        if (newWidth < 70) {
            mScale /= scale;
        }

        if (mSrcBitmap != null) {
            mSrcMatrix.postScale(scale, scale, this.mSrcRect.centerX(),
                    this.mSrcRect.centerY());// 存入scale矩阵
            RectUtil.scaleRect(this.mSrcRect, scale);// 缩放目标矩形

            mHelpBoxRect.set(mSrcRect);
            updateHelpBoxRect();// 重新计算
            mRotateRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            mRotateDstRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteDstRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            RectUtil.rotateRect(this.mDeleteDstRect, this.mSrcRect.centerX(),
                    this.mSrcRect.centerY(), mRotateAngle);
            RectUtil.rotateRect(this.mRotateDstRect, this.mSrcRect.centerX(),
                    this.mSrcRect.centerY(), mRotateAngle);
        } else {
            RectUtil.scaleRect(mHelpBoxRect, scale);
            mRotateRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            mRotateDstRect.offsetTo(mHelpBoxRect.right - BUTTON_WIDTH, mHelpBoxRect.bottom
                    - BUTTON_WIDTH);
            mDeleteDstRect.offsetTo(mHelpBoxRect.left - BUTTON_WIDTH, mHelpBoxRect.top
                    - BUTTON_WIDTH);

            RectUtil.rotateRect(this.mDeleteDstRect, this.mHelpBoxRect.centerX(),
                    this.mHelpBoxRect.centerY(), mRotateAngle);
            RectUtil.rotateRect(this.mRotateDstRect, this.mHelpBoxRect.centerX(),
                    this.mHelpBoxRect.centerY(), mRotateAngle);
        }
    }

    public RectF getRotateRect() {
        return mRotateDstRect;
    }

    public RectF getDeleteRect() {
        return mDeleteDstRect;
    }

    public RectF getBitRect() {
        return mHelpBoxRect;
    }

    /**
     * 会对本贴纸做重新初始化，之前的旋转缩放移动等信息会失效
     *
     * @param bitmap
     * @param parentView
     */
    public void updateStickerInfo(Bitmap bitmap, View parentView) {
        init(bitmap, parentView);
    }

    private void updateHelpBoxRect() {
        mHelpBoxRect.left -= HELP_BOX_PAD;
        mHelpBoxRect.right += HELP_BOX_PAD;
        mHelpBoxRect.top -= HELP_BOX_PAD;
        mHelpBoxRect.bottom += HELP_BOX_PAD;
    }

    private void drawContent(Canvas canvas) {
        drawSticker(canvas);

        if (!mIsDrawHelpBox) {
            return;
        }
        //绘制辅助区域
        canvas.save();
        canvas.rotate(mRotateAngle, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());
        canvas.drawRoundRect(mHelpBoxRect, 10, 10, mHelpPaint);
        canvas.restore();


        canvas.drawBitmap(mDeleteBitmap, mHelpToolsRect, mDeleteDstRect, null);
        canvas.drawBitmap(mRotateBitmap, mHelpToolsRect, mRotateDstRect, null);
    }

    private void drawSticker(Canvas canvas, float scale, float rotate) {
        //draw bitmap
        if (mSrcBitmap != null) {
            canvas.drawBitmap(mSrcBitmap, mSrcMatrix, null);
        }
        canvas.save();
        canvas.scale(scale, scale, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());
        canvas.rotate(rotate, mHelpBoxRect.centerX(), mHelpBoxRect.centerY());
        canvas.restore();
    }
}
