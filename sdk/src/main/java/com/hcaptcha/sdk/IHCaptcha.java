package com.hcaptcha.sdk;

import lombok.NonNull;

/**
 * hCaptcha client which allows invoking of challenge completion and listening for the result.
 *
 * <pre>
 * Usage example:
 * 1. Get a client either using the site key or customize by passing a config:
 *    client = HCaptcha.getClient(this).verifyWithHCaptcha(YOUR_API_SITE_KEY)
 *    client = HCaptcha.getClient(this).verifyWithHCaptcha({@link com.hcaptcha.sdk.HCaptchaConfig})
 *
 *    // Improve cold start by setting up the hCaptcha client
 *    client = HCaptcha.getClient(this).setup({@link com.hcaptcha.sdk.HCaptchaConfig})
 *    // ui rendering...
 *    // user form fill up...
 *    // ready for human verification
 *    client.verifyWithHCaptcha();
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
public interface IHCaptcha {

    /**
     * Prepare the client which allows to display a challenge dialog
     *
     * @return new {@link HCaptcha} object
     */
    HCaptcha setup();

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return new {@link HCaptcha} object
     */
    HCaptcha setup(@NonNull String siteKey);

    /**
     * Constructs a new client which allows to display a challenge dialog
     *
     * @param config Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return new {@link HCaptcha} object
     */
    HCaptcha setup(@NonNull HCaptchaConfig config);

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @return {@link HCaptcha}
     */
    HCaptcha verifyWithHCaptcha();

    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param siteKey The hCaptcha site-key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     * @return {@link HCaptcha}
     */
    HCaptcha verifyWithHCaptcha(@NonNull String siteKey);


    /**
     * Shows a captcha challenge dialog to be completed by the user
     *
     * @param config Config to customize: size, theme, locale, endpoint, rqdata, etc.
     * @return {@link HCaptcha}
     */
    HCaptcha verifyWithHCaptcha(@NonNull HCaptchaConfig config);

    /**
     * Force stop verification and release resources.
     */
    void clear();
}
