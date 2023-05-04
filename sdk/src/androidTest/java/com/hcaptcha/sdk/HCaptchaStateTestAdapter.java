package com.hcaptcha.sdk;

public class HCaptchaStateTestAdapter implements HCaptchaStateListener {
    @Override
    public void onOpen() {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    public void onSuccess(String response) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    public void onFailure(HCaptchaException exception) {
        // empty default implementation to reduce amount of boilerplate code in tests
    }
}
