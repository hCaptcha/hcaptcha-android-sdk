package com.hcaptcha.sdk.tasks;

import com.hcaptcha.sdk.HCaptchaEvent;

public interface OnEventListener {
    void onEvent(HCaptchaEvent event);
}
