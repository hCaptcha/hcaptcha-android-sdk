package com.hcaptcha.sdk;

import java.io.Serializable;

interface HCaptchaStateListener extends Serializable {

    void onSuccess(String token);

    void onFailure(HCaptchaException exception);

    void onOpen();
}
