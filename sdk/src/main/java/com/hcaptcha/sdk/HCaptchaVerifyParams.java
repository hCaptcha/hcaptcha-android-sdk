package com.hcaptcha.sdk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Parameters supplied at verification time.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HCaptchaVerifyParams implements Serializable {

    /**
     * Optional phone country calling code (without '+'), e.g., "44".
     * Used in MFA flows.
     */
    @JsonProperty("mfa_phoneprefix")
    private String phonePrefix;

    /**
     * Optional full phone number in E.164 format ("+44123..."), for use in MFA.
     */
    @JsonProperty("mfa_phone")
    private String phoneNumber;

    /**
     * Optional request data string to be passed to hCaptcha.
     * When provided, JS will call hcaptcha.setData({rqdata: value}) if available.
     */
    private String rqdata;
}
