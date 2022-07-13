package com.hcaptcha.sdk;

import android.os.Handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Token response which contains the token string to be verified
 */
@AllArgsConstructor
public class HCaptchaTokenResponse {

    @Getter
    private final String tokenResult;

    private final Handler handler;

    /**
     * This method will signal SDK to not fire {@link HCaptchaError#TOKEN_TIMEOUT}
     */
    public void markUsed() {
        handler.removeCallbacksAndMessages(null);
    }

}
