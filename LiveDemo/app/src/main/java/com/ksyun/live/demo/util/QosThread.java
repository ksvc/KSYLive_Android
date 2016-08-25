package com.ksyun.live.demo.util;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Handler;

import com.ksyun.live.demo.activity.VideoPlayerActivity;


/**
 * Created by QianYi-Xin on 2015/6/1.
 */
public class QosThread extends Thread {

    private Context mContext;
    private Handler mHandler;
    private Cpu mCpuStats;
    private ActivityManager mActivityManager;
    private Debug.MemoryInfo mi;
    private QosObject mQosObject;

    private boolean mRunning;

    public QosThread(ActivityManager manager, Handler handler,Context mContext) {
        mHandler = handler;
        mCpuStats = new Cpu();
        mActivityManager = manager;
        mi = new Debug.MemoryInfo();
        mRunning = true;
        mQosObject = new QosObject();
        if(mContext!=null){
            this.mContext = mContext;
        }
    }

    @Override
    public void run() {
        while(mRunning) {
            mCpuStats.parseTopResults(mContext.getPackageName());

            Debug.getMemoryInfo(mi);

            if(mHandler != null) {
                mQosObject.cpuUsage = mCpuStats.getProcessCpuUsage();
                mQosObject.pss = mi.getTotalPss();
                mQosObject.vss = mi.getTotalPrivateDirty();
                mHandler.obtainMessage(VideoPlayerActivity.UPDATE_QOSMESS, mQosObject).sendToTarget();
            }
            try {
                sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopThread() {
        mRunning = false;
    }
}
