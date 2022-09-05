package com.hcaptcha.sdk;

import android.util.Log;

import java.util.Locale;

/**
 * Logger
 */
public final class HCaptchaLog {
    private static final String TAG = "hcaptcha";

    static boolean sDiagnosticsLogEnabled = false;

    private HCaptchaLog() {
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void d(String message) {
        if (sDiagnosticsLogEnabled) {
            Log.d(TAG, message);
        }
    }

    public static void d(String message, Object... args) {
        if (sDiagnosticsLogEnabled) {
            Log.d(TAG, String.format(Locale.getDefault(), message, args));
        }
    }
}
