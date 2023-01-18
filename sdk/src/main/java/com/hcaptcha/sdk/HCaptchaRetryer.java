package com.hcaptcha.sdk;

import androidx.annotation.Nullable;

/**
 * Retryer encapsulate logic to decide when the challenge should be retried after failure
 */
class HCaptchaRetryer {
    boolean shouldRetry;

    HCaptchaRetryer(@Nullable HCaptchaConfig config, @Nullable HCaptchaError error) {
        shouldRetry = config != null && config.getResetOnTimeout()
                && error == HCaptchaError.SESSION_TIMEOUT;
    }

    /**
     * Ask to do retry regardless of other conditions
     */
    void retry() {
        shouldRetry = true;
    }

    /**
     * @return true if the challenge should be retried
     */
    boolean shouldRetry() {
        return shouldRetry;
    }
}
