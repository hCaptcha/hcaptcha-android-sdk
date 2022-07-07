package com.hcaptcha.sdk;

import android.os.Handler;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Token response which contains the token string to be verified
 */
@Data
@AllArgsConstructor
public class HCaptchaTokenResponse {

    private final String tokenResult;

    private final Handler handler;

    /**
     * This method will signal SDK to not fire {@link HCaptchaError#SESSION_TIMEOUT}
     */
    public void markUsed() {
        handler.removeCallbacksAndMessages(null);
    }

}
