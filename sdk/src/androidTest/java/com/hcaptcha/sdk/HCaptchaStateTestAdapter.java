package com.hcaptcha.sdk;

public class HCaptchaStateTestAdapter extends HCaptchaStateListener {
    @Override
    void onEvent(HCaptchaEvent event) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onSuccess(HCaptchaTokenResponse response) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onFailure(HCaptchaException exception) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }
}
