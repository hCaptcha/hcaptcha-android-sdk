package com.hcaptcha.sdk;

/**
 * On failure retry decider class
 */
@FunctionalInterface
public interface IHCaptchaRetryPredicate {

    /**
     * Allows retrying in case of verification errors.
     *
     * @param config the hCaptcha config
     * @param exception the hCaptcha exception
     */
    boolean shouldRetry(HCaptchaConfig config, HCaptchaException exception);

}
