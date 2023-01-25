package com.hcaptcha.sdk;

/**
 * A on failure retry decider class
 */
@FunctionalInterface
public interface IHCaptchaRetryPredicate {

    /**
     * Called when the challenge is failed but can be retried
     *
     * @param config the hCaptcha config
     * @param exception the hCaptcha exception
     */
    boolean shouldRetry(HCaptchaConfig config, HCaptchaException exception);
}
