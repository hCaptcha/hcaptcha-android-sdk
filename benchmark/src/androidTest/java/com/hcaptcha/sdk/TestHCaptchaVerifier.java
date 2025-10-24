package com.hcaptcha.sdk;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TestHCaptchaVerifier implements IHCaptchaVerifier {

    @Override
    public void startVerification(@NonNull Activity activity, @Nullable HCaptchaVerifyParams verifyParams) {
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

    @Override
    public void destroy() {
        // no implementation need for performance measurement
    }
}
