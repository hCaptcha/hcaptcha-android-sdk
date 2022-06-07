package com.hcaptcha.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import com.hcaptcha.sdk.tasks.Task;


/**
 * hCaptcha client which allows invoking of challenge completion and listening for the result.
 *
 * <pre>
 * Usage example:
 * 1. Get a client either using the site key or customize by passing a config:
 *    client = HCaptcha.getClient(this).verifyWithHCaptcha(YOUR_API_SITE_KEY)
 *    client = HCaptcha.getClient(this).verifyWithHCaptcha({@link com.hcaptcha.sdk.HCaptchaConfig})
 * 2. Listen for the result and error events:
 *
 *  client.addOnSuccessListener(new OnSuccessListener{@literal <}HCaptchaTokenResponse{@literal >}() {
 *            {@literal @}Override
 *             public void onSuccess(HCaptchaTokenResponse response) {
 *                 String userResponseToken = response.getTokenResult();
 *                 Log.d(TAG, "hCaptcha token: " + userResponseToken);
 *                 // Validate the user response token using the hCAPTCHA siteverify API
 *             }
 *         })
 *         .addOnFailureListener(new OnFailureListener() {
 *            {@literal @}Override
 *             public void onFailure(HCaptchaException e) {
 *                 Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
 *             }
 *         });
 *  </pre>
 */
public class HCaptcha extends Task<HCaptchaTokenResponse> {
    public static final String META_SITE_KEY = "com.hcaptcha.sdk.site-key";

    @NonNull
    private final FragmentActivity activity;

    @NonNull
    private final FragmentManager fragmentManager;

    @Nullable
    private HCaptchaWebViewProvider hCaptchaWebViewProvider;

    @Nullable
    private HCaptchaConfig hCaptchaConfig;

    private HCaptcha(@NonNull final Context context) {
        this.activity = (FragmentActivity) context;
        this.fragmentManager = activity.getSupportFragmentManager();
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

    /**
     * Prepare the client which allows to display a challenge dialog
     *
     * @return new {@link HCaptcha} object
     */
    public HCaptcha setup() {
        String siteKey = null;
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

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return new {@link HCaptcha} object
     */
    public HCaptcha setup(@NonNull final String siteKey) {
        final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder().siteKey(siteKey).build();
        return setup(hCaptchaConfig);
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param hCaptchaConfig Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return new {@link HCaptcha} object
     */
    public HCaptcha setup(@NonNull final HCaptchaConfig hCaptchaConfig) {
        this.hCaptchaConfig = hCaptchaConfig;

        final HCaptchaDialogListener listener = new HCaptchaDialogListener() {
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

        if (hCaptchaConfig.getShowDialog()) {
            this.hCaptchaWebViewProvider = HCaptchaDialogFragment.newInstance(hCaptchaConfig, listener);
        } else {
            this.hCaptchaWebViewProvider = new HCaptchaHeadlessWebView(activity, hCaptchaConfig, listener);
        }

        return this;
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha() {
        if (hCaptchaConfig == null || hCaptchaWebViewProvider == null) {
            setup();
        }

        hCaptchaWebViewProvider.startVerification(activity);

        return this;
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha(@NonNull final String siteKey) {
        if (hCaptchaConfig == null || !siteKey.equals(hCaptchaConfig.getSiteKey()) || hCaptchaWebViewProvider == null) {
            setup(siteKey);
        }

        hCaptchaWebViewProvider.startVerification(activity);

        return this;
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param hCaptchaConfig Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig hCaptchaConfig) {
        if (!hCaptchaConfig.equals(this.hCaptchaConfig) || hCaptchaWebViewProvider == null) {
            setup(hCaptchaConfig);
        }

        hCaptchaWebViewProvider.startVerification(activity);

        return this;
    }
}
