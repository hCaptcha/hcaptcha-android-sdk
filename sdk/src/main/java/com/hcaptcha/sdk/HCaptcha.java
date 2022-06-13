package com.hcaptcha.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import lombok.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import com.hcaptcha.sdk.tasks.Task;


public class HCaptcha extends Task<HCaptchaTokenResponse> implements IHCaptcha {
    public static final String META_SITE_KEY = "com.hcaptcha.sdk.site-key";
    public static final String TAG = "hCaptcha";

    @NonNull
    private final FragmentActivity activity;

    @Nullable
    private IHCaptchaVerifier captchaVerifier;

    @Nullable
    private HCaptchaConfig config;

    private HCaptcha(@NonNull final Context context) {
        this.activity = (FragmentActivity) context;
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param context The current context
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Context context) {
        return new HCaptcha(context);
    }

    @Override
    public HCaptcha setup() {
        final String siteKey;
        try {
            String packageName = activity.getPackageName();
            ApplicationInfo app = activity.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            siteKey = bundle.getString(META_SITE_KEY);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }
        if (siteKey == null) {
            throw new IllegalStateException("Add missing " + META_SITE_KEY + " meta-data to AndroidManifest.xml"
                    + " or call getClient(context, siteKey) method");
        }
        return setup(siteKey);
    }

    @Override
    public HCaptcha setup(@NonNull final String siteKey) {
        return setup(HCaptchaConfig.builder().siteKey(siteKey).build());
    }

    @Override
    public HCaptcha setup(@NonNull HCaptchaConfig config) {
        final HCaptchaStateListener listener = new HCaptchaStateListener() {
            @Override
            void onOpen() {
                captchaOpened();
            }
            @Override
            void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
                setResult(hCaptchaTokenResponse);
            }
            @Override
            void onFailure(final HCaptchaException hCaptchaException) {
                setException(hCaptchaException);
            }
        };
        if (config.getFullInvisible()) {
            // Overwrite certain config values in case of full invisible to avoid behavior collision
            config = config.toBuilder()
                    .size(HCaptchaSize.INVISIBLE)
                    .loading(false)
                    .build();
            captchaVerifier = new HCaptchaHeadlessWebView(activity, config, listener);
        } else {
            captchaVerifier = HCaptchaDialogFragment.newInstance(config, listener);
        }
        this.config = config;
        return this;
    }

    @Override
    public HCaptcha verifyWithHCaptcha() {
        if (captchaVerifier == null) {
            // Cold start at verification time.
            setup();
        }
        captchaVerifier.startVerification(activity);
        return this;
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final String siteKey) {
        if (captchaVerifier == null || this.config == null || !siteKey.equals(this.config.getSiteKey())) {
            // Cold start at verification time.
            // Or new sitekey detected, thus new setup is needed.
            setup(siteKey);
        }
        captchaVerifier.startVerification(activity);
        return this;
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig config) {
        if (captchaVerifier == null || !config.equals(this.config)) {
            // Cold start at verification time.
            // Or new config detected, thus new setup is needed.
            setup(config);
        }
        captchaVerifier.startVerification(activity);
        return this;
    }
}
