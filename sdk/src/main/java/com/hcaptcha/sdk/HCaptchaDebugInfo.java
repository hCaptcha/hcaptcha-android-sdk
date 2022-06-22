package com.hcaptcha.sdk;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import dalvik.system.DexFile;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * Debug info for CI.
 */
@Data
@RequiredArgsConstructor
class HCaptchaDebugInfo implements Serializable {

    public static final String JS_INTERFACE_TAG = "JSDI";

    private static String sCache;

    @NonNull
    private final Context context;

    @JavascriptInterface
    public String getDebugInfo() {
        if (sCache != null) {
            return sCache;
        }
        synchronized (this) {
            if (sCache != null) {
                return sCache;
            }

            try {
                List<String> debugInfo = debugInfo(context.getPackageName(), context.getPackageCodePath());
                final ObjectMapper objectMapper = new ObjectMapper();
                sCache = objectMapper.writeValueAsString(debugInfo);
            } catch (IOException | NoSuchAlgorithmException | CloneNotSupportedException e) {
                Log.d(JS_INTERFACE_TAG, "Cannot build debugInfo");
                return "[]";
            }
            return sCache;
        }
    }

    private List<String> debugInfo(String packageName, String packageCode)
            throws IOException, NoSuchAlgorithmException, CloneNotSupportedException {
        List<String> result = new ArrayList<>(512);
        DexFile dexFile = new DexFile(packageCode);
        Enumeration<String> classes = dexFile.entries();
        MessageDigest androidMd5 = MessageDigest.getInstance("MD5");
        MessageDigest appMd5 = MessageDigest.getInstance("MD5");
        MessageDigest depsMd5 = MessageDigest.getInstance("MD5");
        while (classes.hasMoreElements()) {
            String cls = classes.nextElement();
            if (cls.startsWith("com.google.android.") || cls.startsWith("android.")) {
                androidMd5.update(cls.getBytes("UTF-8"));
            } else if (cls.startsWith(packageName)) {
                appMd5.update(cls.getBytes("UTF-8"));
            } else {
                depsMd5.update(cls.getBytes("UTF-8"));
            }
        }
        result.add("sys_" + String.format("%032x", new BigInteger(1, androidMd5.digest())));
        result.add("deps_" + String.format("%032x", new BigInteger(1, depsMd5.digest())));
        result.add("app_" + String.format("%032x", new BigInteger(1, appMd5.digest())));
        result.add("aver_" + Build.VERSION.RELEASE);
        result.add("sdk_" + BuildConfig.VERSION_NAME.replace('.', '_'));
        return result;
    }
}
