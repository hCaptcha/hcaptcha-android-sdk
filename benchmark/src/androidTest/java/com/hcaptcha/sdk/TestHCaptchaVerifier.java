package com.hcaptcha.sdk;

import android.app.Activity;

public class TestHCaptchaVerifier implements IHCaptchaVerifier {

    @Override
    public void startVerification(Activity activity) {
        // no implementation need for performance measurement
    }

    @Override
    public void onFailure(HCaptchaException exception) {
        // no implementation need for performance measurement
    }

    @Override
    public void onLoaded() {
        // no implementation need for performance measurement
    }

    @Override
    public void onOpen() {
        // no implementation need for performance measurement
    }

    @Override
    public void onSuccess(String s) {
        // no implementation need for performance measurement
    }

    @Override
    public void reset() {
        // no implementation need for performance measurement
    }
}
