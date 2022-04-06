package com.hcaptcha.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
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

    private final FragmentManager fragmentManager;

    private final HCaptchaDialogFragment hCaptchaDialogFragment;

    private HCaptcha(@NonNull final Context context, @NonNull final HCaptchaConfig hCaptchaConfig) {
        this(((FragmentActivity) context).getSupportFragmentManager(), hCaptchaConfig);
    }

    private HCaptcha(@NonNull final FragmentManager fragmentManager, @NonNull final HCaptchaConfig hCaptchaConfig) {
        this.fragmentManager = fragmentManager;
        this.hCaptchaDialogFragment = HCaptchaDialogFragment.newInstance(hCaptchaConfig, new HCaptchaDialogListener() {
            @Override
            void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
                setResult(hCaptchaTokenResponse);
            }

            @Override
            void onFailure(final HCaptchaException hCaptchaException) {
                setException(hCaptchaException);
            }
        });
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param context The current context
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Context context) {
        String siteKey = null;
        try {
            ApplicationInfo app = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = app.metaData;
            siteKey = bundle.getString(META_SITE_KEY);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(e);
        }

        if (siteKey == null) {
            throw new IllegalStateException("Add missing " + META_SITE_KEY + " meta-data to AndroidManifest.xml"
                    + " or call getClient(context, siteKey) method");
        }

        return getClient(context, siteKey);
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param context The current context
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Context context, @NonNull final String siteKey) {
        final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder().siteKey(siteKey).build();
        return getClient(context, hCaptchaConfig);
    }

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param context The current context
     * @param hCaptchaConfig Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Context context, @NonNull final HCaptchaConfig hCaptchaConfig) {
        return new HCaptcha(context, hCaptchaConfig);
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha() {
        hCaptchaDialogFragment.show(fragmentManager, HCaptchaDialogFragment.TAG);
        return this;
    }

}
