package com.hcaptcha.sdk;

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

    @NonNull
    private final HCaptchaInternalConfig internalConfig;

    private HCaptcha(@NonNull final FragmentActivity activity, @NonNull final HCaptchaInternalConfig internalConfig) {
        this.activity = activity;
        this.internalConfig = internalConfig;
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param activity The current activity
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final FragmentActivity activity) {
        return getClient(activity, HCaptchaInternalConfig.builder().build());
    }

    static HCaptcha getClient(@NonNull final FragmentActivity activity, @NonNull HCaptchaInternalConfig internalConfig) {
        return new HCaptcha(activity, internalConfig);
    }

    @Override
    public HCaptcha setup() {
        final String siteKey;
        try {
            final ApplicationInfo app = HCaptchaCompat.getApplicationInfo(activity);
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
        HCaptchaLog.sDiagnosticsLogEnabled = inputConfig.getDiagnosticLog();
        HCaptchaLog.d("HCaptcha.setup");

        final HCaptchaStateListener listener = new HCaptchaStateListener() {
            @Override
            void onOpen() {
                captchaOpened();
            }

            @Override
            void onSuccess(final String token) {
                HCaptchaLog.d("HCaptcha.onSuccess");
                scheduleCaptchaExpired(HCaptcha.this.config.getTokenExpiration());
                setResult(new HCaptchaTokenResponse(token, HCaptcha.this.handler));
            }

            @Override
            void onFailure(final HCaptchaException exception) {
                HCaptchaLog.d("HCaptcha.onFailure");
                setException(exception);
            }
        };
        if (inputConfig.getHideDialog()) {
            // Overwrite certain config values in case the dialog is hidden to avoid behavior collision
            this.config = inputConfig.toBuilder()
                    .size(HCaptchaSize.INVISIBLE)
                    .loading(false)
                    .build();
            captchaVerifier = new HCaptchaHeadlessWebView(activity, this.config, internalConfig, listener);
        } else {
            captchaVerifier = HCaptchaDialogFragment.newInstance(inputConfig, internalConfig, listener);
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

        return startVerification();
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final String siteKey) {
        if (captchaVerifier == null || this.config == null || !siteKey.equals(this.config.getSiteKey())) {
            // Cold start at verification time.
            // Or new sitekey detected, thus new setup is needed.
            setup(siteKey);
        }

        return startVerification();
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig inputConfig) {
        if (captchaVerifier == null || !inputConfig.equals(this.config)) {
            // Cold start at verification time.
            // Or new config detected, thus new setup is needed.
            setup(inputConfig);
        }

        return startVerification();
    }

    @Override
    public void reset() {
        if (captchaVerifier != null) {
            captchaVerifier.reset();
            captchaVerifier = null;
        }
    }

    private HCaptcha startVerification() {
        HCaptchaLog.d("HCaptcha.startVerification");
        handler.removeCallbacksAndMessages(null);
        assert captchaVerifier != null;
        captchaVerifier.startVerification(activity);
        return this;
    }
}
