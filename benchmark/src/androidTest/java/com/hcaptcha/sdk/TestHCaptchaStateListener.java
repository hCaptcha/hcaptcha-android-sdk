package com.hcaptcha.sdk;

public class TestHCaptchaStateListener extends HCaptchaStateListener {
    @Override
    void onSuccess(String token) {
        // no implementation need for performance measurement
    }

    @Override
    void onFailure(HCaptchaException exception) {
        // no implementation need for performance measurement
    }

    @Override
    void onOpen() {
        // no implementation need for performance measurement
    }

    @Override
    boolean shouldRetry(HCaptchaError error) {
        return false;
    }
}
