package com.hcaptcha.sdk.tasks;


import com.hcaptcha.sdk.HCaptchaError;

/**
 * A on failure retry decider class
 */
public interface OnFailureRetryDecider {

    /**
     * Called when the challenge is failed but can be retried
     *
     * @param error the hCaptcha error
     */
    boolean shouldRetry(HCaptchaError error);

}
