package com.hcaptcha.sdk;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * hCaptcha settings keep internal settings, which should not be accessible by user
 */
@Data
@Builder(toBuilder = true)
class HCaptchaSettings implements Serializable {

    /**
     * HTML Provider
     */
    @Builder.Default
    private IHCaptchaHtmlProvider htmlProvider = new HCaptchaHtml();
}
