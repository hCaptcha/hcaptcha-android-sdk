package com.hcaptcha.sdk.tasks;

/**
 * A hCaptcha challenge expired listener class
 */
public interface OnChallengeExpiredListener {

    /**
     * Called when the hCaptcha challenge is expired
     */
    void onChallengeExpired();
}
