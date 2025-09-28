package com.hcaptcha.sdk;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Parameters supplied at verification time.
 */
@Data
@Builder(toBuilder = true)
public class HCaptchaVerifyParams implements Serializable {

    /**
     * Optional phone country calling code (without '+'), e.g., "44".
     * Used in MFA flows.
     */
    private String phonePrefix;

    /**
     * Optional full phone number in E.164 or local format as expected by JS.
     * When provided, JS will call hcaptcha.setPhoneNumber(value) if available.
     */
    private String phoneNumber;
}
