package com.hcaptcha.sdk;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * hCaptcha internal config keep internal configuration, which should not be accessible by end user
 */
@Data
@Builder(toBuilder = true)
class HCaptchaInternalConfig implements Serializable {

    /**
     * HTML Provider
     */
    @Builder.Default
    private IHCaptchaHtmlProvider htmlProvider = new HCaptchaHtml();
}
