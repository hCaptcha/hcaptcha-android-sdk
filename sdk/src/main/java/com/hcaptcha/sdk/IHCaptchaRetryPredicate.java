package com.hcaptcha.sdk;

import java.io.Serializable;

/**
 * On failure retry decider class
 */
@FunctionalInterface
public interface IHCaptchaRetryPredicate extends Serializable {

    /**
     * Allows retrying in case of verification errors.
     *
     * @param config the hCaptcha config
     * @param exception the hCaptcha exception
     */
    boolean shouldRetry(HCaptchaConfig config, HCaptchaException exception);

}
