package com.hcaptcha.sdk;

import android.app.Activity;
import android.content.Context;
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

    private final FragmentManager fragmentManager;

    private HCaptcha(@NonNull final Activity activity) {
        this((Context) activity);
    }

    private HCaptcha(@NonNull final Context context) {
        this(((FragmentActivity) context).getSupportFragmentManager());
    }

    private HCaptcha(@NonNull final FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
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
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param activity The current activity
     * @return new {@link HCaptcha} object
     */
    public static HCaptcha getClient(@NonNull final Activity activity) {
        return new HCaptcha(activity);
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha(@NonNull final String siteKey) {
        final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder().siteKey(siteKey).build();
        return verifyWithHCaptcha(hCaptchaConfig);
    }

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param hCaptchaConfig Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return {@link HCaptcha}
     */
    public HCaptcha verifyWithHCaptcha(@NonNull final HCaptchaConfig hCaptchaConfig) {
        final HCaptchaDialogFragment hCaptchaDialogFragment = HCaptchaDialogFragment.newInstance(hCaptchaConfig, new HCaptchaDialogListener() {
            @Override
            void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
                setResult(hCaptchaTokenResponse);
            }

            @Override
            void onFailure(final HCaptchaException hCaptchaException) {
                setException(hCaptchaException);
            }
        });
        hCaptchaDialogFragment.show(fragmentManager, HCaptchaDialogFragment.TAG);
        return this;
    }

}
