package com.ksyun.live.demo.util;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
       // CrashReport.initCrashReport(getApplicationContext(), "900029865", false);
    }
}

