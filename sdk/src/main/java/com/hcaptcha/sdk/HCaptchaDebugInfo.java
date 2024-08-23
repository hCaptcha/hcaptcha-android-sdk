package com.hcaptcha.sdk;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import dalvik.system.DexFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Debug info for CI.
 */
@Data
@AllArgsConstructor
final class HCaptchaDebugInfo implements Serializable {
    public static final String JS_INTERFACE_TAG = "JSDI";
    private static final String GET_PROP_BIN = "/system/bin/getprop";
    @SuppressWarnings("java:S4719") // minSdkVersion is 16 and StandardCharsets.UTF_8 is available from 19
    private static final String CHARSET_NAME = "UTF-8";

    private static final int MAX_ENTRIES = 512;

    private static String sDebugInfo = "[]";
    private static String sSysDebug;

    @NonNull
    private final transient Context context;

    @JavascriptInterface
    public String getDebugInfo() {
        if (sDebugInfo != null) {
            return sDebugInfo;
        }
        synchronized (this) {
            if (sDebugInfo != null) {
                return sDebugInfo;
            }

            try {
                final List<String> debugInfo = debugInfo(context.getPackageName(), context.getPackageCodePath());
                final ObjectMapper objectMapper = new ObjectMapper();
                sDebugInfo = objectMapper.writeValueAsString(debugInfo);
            } catch (IOException | NoSuchAlgorithmException e) {
                Log.d(JS_INTERFACE_TAG, "Cannot build debugInfo");
                return "[]";
            }
            return sDebugInfo;
        }
    }

    @JavascriptInterface
    public String getSysDebug() {
        if (sSysDebug != null) {
            return sSysDebug;
        }
        synchronized (this) {
            if (sSysDebug != null) {
                return sSysDebug;
            }

            try {
                final Map<String, String> props = roBuildProps();
                final ObjectMapper objectMapper = new ObjectMapper();
                sSysDebug = objectMapper.writeValueAsString(props);
            } catch (IOException e) {
                Log.d(JS_INTERFACE_TAG, "Cannot build sysDebug");
                return "{}";
            }
            return sSysDebug;
        }
    }

    List<String> debugInfo(String packageName, String packageCode)
            throws IOException, NoSuchAlgorithmException {
        final List<String> result = new ArrayList<>(8);
        final MessageDigest androidMd5 = MessageDigest.getInstance("MD5");
        final MessageDigest appMd5 = MessageDigest.getInstance("MD5");
        final MessageDigest depsMd5 = MessageDigest.getInstance("MD5");
        final DexFile dexFile = new DexFile(packageCode);
        try {
            int entryCount = 0;
            final Enumeration<String> classes = dexFile.entries();
            while (classes.hasMoreElements()) {
                final String cls = classes.nextElement();
                if (cls.startsWith("com.google.android.") || cls.startsWith("android.")) {
                    androidMd5.update(cls.getBytes(CHARSET_NAME));
                } else if (cls.startsWith(packageName)) {
                    appMd5.update(cls.getBytes(CHARSET_NAME));
                } else {
                    depsMd5.update(cls.getBytes(CHARSET_NAME));
                }

                if (++entryCount > MAX_ENTRIES) {
                    break;
                }
            }
        } finally {
            dexFile.close();
        }
        final String hexFormat = "%032x";
        result.add("sys_" + String.format(hexFormat, new BigInteger(1, androidMd5.digest())));
        result.add("deps_" + String.format(hexFormat, new BigInteger(1, depsMd5.digest())));
        result.add("app_" + String.format(hexFormat, new BigInteger(1, appMd5.digest())));
        result.add("aver_" + Build.VERSION.RELEASE);
        result.add("sdk_" + BuildConfig.VERSION_NAME.replace('.', '_'));
        return result;
    }

    Map<String, String> roBuildProps() throws IOException {
        Process getpropProcess = null;
        final Map<String, String> props = new HashMap<>();
        try {
            getpropProcess = new ProcessBuilder(GET_PROP_BIN).start();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(getpropProcess.getInputStream(), CHARSET_NAME))) {
                String line;
                final StringBuilder entry = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    if (line.endsWith("]")) {
                        entry.replace(0, entry.length() == 0 ? 0 : entry.length() - 1, line);
                    } else {
                        entry.append(line);
                        continue;
                    }
                    final String[] parsedLine = entry.toString().split("]: \\[");
                    final String key = parsedLine[0].substring(1); // strip first [
                    if (key.startsWith("ro")) {
                        final String value = parsedLine[1].substring(0, parsedLine[1].length() - 2); // strip last ]
                        props.put(key, value);
                    }
                }
            }
        } finally {
            if (getpropProcess != null) {
                getpropProcess.destroy();
            }
        }
        return props;
    }
}
