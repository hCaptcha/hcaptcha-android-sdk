package com.hcaptcha.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

/**
 * Internal class not pollute code with SDK_INT >= VERSION_CODES.X checks
 */
final class HCaptchaCompat {
    private HCaptchaCompat() {
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    static <T extends Serializable> T getSerializable(Bundle bundle, @Nullable String key, Class<T> clazz) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getSerializable(key, clazz);
        } else {
            return (T) bundle.getSerializable(key);
        }
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    static <T extends Parcelable> T getParcelable(Bundle bundle, @Nullable String key, Class<T> clazz) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return bundle.getParcelable(key, clazz);
        } else {
            return (T) bundle.getParcelable(key);
        }
    }

    @SuppressWarnings({ "deprecation" })
    static ApplicationInfo getApplicationInfo(@NonNull Context context)
            throws PackageManager.NameNotFoundException {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.ApplicationInfoFlags.of(
                            PackageManager.GET_META_DATA));
        } else {
            return context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        }
    }
}
