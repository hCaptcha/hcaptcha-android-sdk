package com.hcaptcha.sdk;

import java.io.Serializable;

abstract class HCaptchaStateListener implements Serializable {

    abstract void onSuccess(String token);

    abstract void onFailure(HCaptchaException exception);

    abstract void onOpen();
}
