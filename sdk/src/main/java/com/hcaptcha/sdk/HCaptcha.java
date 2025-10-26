package com.hcaptcha.sdk;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.Task;

public final class HCaptcha extends Task<HCaptchaTokenResponse> implements IHCaptcha {
    public static final String META_SITE_KEY = "com.hcaptcha.sdk.site-key";

    @NonNull
    private final Activity activity;

    @Nullable
    private IHCaptchaVerifier captchaVerifier;

    @Nullable
    private HCaptchaConfig config;

    @NonNull
    private final HCaptchaInternalConfig internalConfig;

    private HCaptcha(@NonNull final Activity activity, @NonNull final HCaptchaInternalConfig internalConfig) {
        this.activity = activity;
        this.internalConfig = internalConfig;
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param activity FragmentActivity instance for a visual challenge verification,
     *                or any Activity in case of passive siteKey
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Activity activity) {
        return new HCaptcha(activity, HCaptchaInternalConfig.builder().build());
    }

    static HCaptcha getClient(@NonNull final Activity activity,
                              @NonNull HCaptchaInternalConfig internalConfig) {
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
            throw new IllegalStateException("The site-key is missing. You can pass it by adding "
                    + META_SITE_KEY + " as meta-data to AndroidManifest.xml "
                    + "or as an argument for setup/verifyWithHCaptcha methods.");
        }
        return setup(siteKey);
    }

    @Override
    public HCaptcha setup(@NonNull final String siteKey) {
        return setup(HCaptchaConfig.builder().siteKey(siteKey).build());
    }

    @Override
    @SuppressWarnings("java:S2696")  // allow set sDiagnosticsLogEnabled
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
                scheduleCaptchaExpired(inputConfig.getTokenExpiration());
                setResult(new HCaptchaTokenResponse(token, HCaptcha.this.handler));
            }

            @Override
            void onFailure(final HCaptchaException exception) {
                HCaptchaLog.d("HCaptcha.onFailure");
                setException(exception);
            }
        };
        try {
            if (Boolean.TRUE.equals(inputConfig.getHideDialog())) {
                // Overwrite certain config values in case the dialog is hidden to avoid behavior collision
                this.config = inputConfig.toBuilder()
                        .size(HCaptchaSize.INVISIBLE)
                        .loading(false)
                        .build();
                captchaVerifier = new HCaptchaHeadlessWebView(activity, this.config, internalConfig, listener);
            } else if (this.activity instanceof FragmentActivity) {
                captchaVerifier = HCaptchaDialogFragment.newInstance(activity, inputConfig, internalConfig, listener);
                this.config = inputConfig;
            } else {
                throw new IllegalStateException("Visual hCaptcha challenge verification requires FragmentActivity.");
            }
        } catch (AndroidRuntimeException e) {
            listener.onFailure(new HCaptchaException(HCaptchaError.ERROR));
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
    public HCaptcha verifyWithHCaptcha(@Nullable final HCaptchaVerifyParams verifyParams) {
        if (captchaVerifier == null) {
            // Cold start at verification time.
            setup();
        }

        return startVerification(verifyParams);
    }

    @Override
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig inputConfig,
                                       @Nullable final HCaptchaVerifyParams verifyParams) {
        if (captchaVerifier == null || !inputConfig.equals(this.config)) {
            // Cold start at verification time.
            // Or new config detected, thus new setup is needed.
            setup(inputConfig);
        }

        return startVerification(verifyParams);
    }

    @Override
    public void reset() {
        if (captchaVerifier != null) {
            captchaVerifier.reset();
            captchaVerifier = null;
        }
    }

    @Override
    public void destroy() {
        if (captchaVerifier != null) {
            captchaVerifier.destroy();
            captchaVerifier = null;
        }
    }

    private HCaptcha startVerification() {
        HCaptchaLog.d("HCaptcha.startVerification");
        return startVerification(null);
    }

    private HCaptcha startVerification(@Nullable final HCaptchaVerifyParams verifyParams) {
        HCaptchaLog.d("HCaptcha.startVerification" + (verifyParams != null ? " with params" : ""));
        handler.removeCallbacksAndMessages(null);
        if (captchaVerifier == null) {
            setException(new HCaptchaException(HCaptchaError.ERROR));
        } else {
            captchaVerifier.startVerification(activity, verifyParams);
        }
        return this;
    }
}
