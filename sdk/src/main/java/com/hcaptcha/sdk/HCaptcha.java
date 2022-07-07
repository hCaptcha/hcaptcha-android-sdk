package com.hcaptcha.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.Task;
import lombok.NonNull;

public final class HCaptcha extends Task<HCaptchaTokenResponse> implements IHCaptcha {
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
            final String packageName = activity.getPackageName();
            final ApplicationInfo app = activity.getPackageManager().getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA);
            final Bundle bundle = app.metaData;
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
    public HCaptcha setup(@NonNull final HCaptchaConfig inputConfig) {
        final HCaptchaStateListener listener = new HCaptchaStateListener() {
            @Override
            void onOpen() {
                captchaOpened();
            }

            @Override
            void onClose() {
                captchaClosed();
            }

            @Override
            void onChallengeExpired() {
                captchaChallengeExpired();
            }

            @Override
            void onSuccess(final String token) {
                scheduleCaptchaExpired(HCaptcha.this.config.getExpirationTimeout());
                setResult(new HCaptchaTokenResponse(token, HCaptcha.this.handler));
            }

            @Override
            void onFailure(final HCaptchaException exception) {
                setException(exception);
            }
        };
        if (inputConfig.getHideDialog()) {
            // Overwrite certain config values in case the dialog is hidden to avoid behavior collision
            this.config = inputConfig.toBuilder()
                    .size(HCaptchaSize.INVISIBLE)
                    .loading(false)
                    .build();
            captchaVerifier = new HCaptchaHeadlessWebView(activity, this.config, listener);
        } else {
            captchaVerifier = HCaptchaDialogFragment.newInstance(inputConfig, listener);
            this.config = inputConfig;
        }
        return this;
    }

    @Override
    public HCaptcha verifyWithHCaptcha() {
        if (captchaVerifier == null) {
            // Cold start at verification time.
            setup();
        }
        assert captchaVerifier != null;
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
        assert captchaVerifier != null;
        captchaVerifier.startVerification(activity);
        return this;
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig inputConfig) {
        if (captchaVerifier == null || !inputConfig.equals(this.config)) {
            // Cold start at verification time.
            // Or new config detected, thus new setup is needed.
            setup(inputConfig);
        }
        assert captchaVerifier != null;
        captchaVerifier.startVerification(activity);
        return this;
    }
}
