package com.hcaptcha.sdk;

public class HCaptchaStateTestAdapter extends HCaptchaStateListener {
    @Override
    void onOpen() {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onChallengeExpired() {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onClose() {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onSuccess(String response) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onFailure(HCaptchaException exception) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }
}
