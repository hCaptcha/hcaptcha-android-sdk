package com.hcaptcha.example;

import android.app.Application;
import android.os.StrictMode;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (com.hcaptcha.sdk.BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                    new StrictMode.ThreadPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );
            StrictMode.setVmPolicy(
                    new StrictMode.VmPolicy.Builder()
                            .detectAll()
                            .penaltyLog()
                            .build()
            );
        }
    }
}
