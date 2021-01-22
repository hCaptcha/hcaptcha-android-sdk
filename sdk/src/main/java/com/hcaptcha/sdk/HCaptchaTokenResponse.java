package com.hcaptcha.sdk;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Token response which contains the token string to be verified
 */
@Data
@AllArgsConstructor
public class HCaptchaTokenResponse {

    private final String tokenResult;

}
