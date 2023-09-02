package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.failAsNonReachable;

public class HCaptchaStateTestAdapter extends HCaptchaStateListener {
    @Override
    void onOpen() {
        // empty default implementation to reduce amount of boilerplate code in tests
    }

    @Override
    void onSuccess(String response) {
        failAsNonReachable();
    }

    @Override
    void onFailure(HCaptchaException exception) {
        failAsNonReachable();
    }
}
