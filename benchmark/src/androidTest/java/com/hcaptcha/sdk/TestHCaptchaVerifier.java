package com.hcaptcha.sdk;

import androidx.fragment.app.FragmentActivity;

public class TestHCaptchaVerifier implements IHCaptchaVerifier {

    @Override
    public void startVerification(FragmentActivity activity) {
        // no implementation need for performance measurement
    }

    public void onFailure(HCaptchaException e) {
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
}
