package com.ksyun.media.streamer.demo.sticker;

import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * sticker help box for delete and rotate
 */

public class StickerHelpBoxInfo {
    public Bitmap deleteBit;  //删除本涂层贴纸按钮
    public Bitmap rotateBit;  //旋转本涂层贴纸按钮
    /**
     * set help box paint<br/>
     * for example(http://blog.csdn.net/abcdef314159/article/details/51720686):<br/>
     * setColor(Color.BLACK);   颜色<br/>
     * setStyle(Paint.Style.STROKE);  描边<br/>
     * helpBoxPaint.setAntiAlias(true);  抗锯齿<br/>
     * helpBoxPaint.setStrokeWidth(4);  描边宽度<br/>
     */
    public Paint helpBoxPaint;

    public StickerHelpBoxInfo() {
    }
}
