package com.ksyun.live.demo.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;
import android.view.TextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by liubohua on 16/8/1.
 */
public class ScreenThread extends Thread {
    private TextureView view;

    public ScreenThread(TextureView view){
        this.view = view;
    }

    @Override
    public void run() {
        super.run();
        while(true){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Bitmap content = view.getBitmap();

            try{
                Bitmap screenshot = Bitmap.createBitmap(content.getWidth(), content.getHeight(), Bitmap.Config.ARGB_4444);
                Canvas canvas = new Canvas(screenshot);
                canvas.drawBitmap(content, 0, 0, new Paint());
                canvas.save();
                canvas.restore();

                savebitmap(screenshot);

                Log.e("TestDemo","截图成功");
            }catch (Exception e){
                break;
            }

            // 把两部分拼起来，先把视频截图绘制到上下左右居中的位置，再把播放器的布局元素绘制上去。


        }

    }



    public void savebitmap(Bitmap bitmap){
        File appDir = new File(Environment.getExternalStorageDirectory(),"com.ksy.recordlib.demo.demo");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
