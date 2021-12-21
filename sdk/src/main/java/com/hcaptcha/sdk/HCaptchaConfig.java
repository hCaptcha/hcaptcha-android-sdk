package com.hcaptcha.sdk;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Locale;


/**
 * hCaptcha config builder which allows further customization of UI and other logic.
 * {@link #siteKey} is the only mandatory property.
 */
@Data
@Builder(toBuilder = true)
public class HCaptchaConfig implements Serializable {

    /**
     * The site key. Get one here <a href="https://www.hcaptcha.com">hcaptcha.com</a>
     */
    @NonNull
    private String siteKey;

    /**
     * Enable / Disable sentry error reporting.
     */
    @Builder.Default
    private Boolean sentry = true;

    /**
     * Show / Hide loading dialog.
     */
    @Builder.Default
    private Boolean loading = true;

    /**
     * Custom supplied challenge data.
     */
    private String rqdata;

    /**
     * The url of api.js
     * Default: https://js.hcaptcha.com/1/api.js (Override only if using first-party hosting feature.)
     */
    @Builder.Default
    private String apiEndpoint = "https://js.hcaptcha.com/1/api.js";

    /**
     * Point hCaptcha JS Ajax Requests to alternative API Endpoint.
     * Default: https://api.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    private String endpoint;

    /**
     * Point hCaptcha Bug Reporting Request to alternative API Endpoint.
     * Default: https://accounts.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    private String reportapi;

    /**
     * Points loaded hCaptcha assets to a user defined asset location, used for proxies.
     * Default: https://newassets.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    private String assethost;

    /**
     * Points loaded hCaptcha challenge images to a user defined image location, used for proxies.
     * Default: https://imgs.hcaptcha.com (Override only if using first-party hosting feature.)
     */
    private String imghost;

    /**
     * The locale: 2 characters language code iso 639-1
     * Default: current default locale for this instance of the JVM.
     */
    @Builder.Default
    private String locale = Locale.getDefault().getLanguage();

    /**
     * The size of the checkbox. Default is {@link HCaptchaSize#INVISIBLE}.
     */
    @Builder.Default
    private HCaptchaSize size = HCaptchaSize.INVISIBLE;

    /**
     * The theme. Default is {@link HCaptchaTheme#LIGHT}.
     */
    @Builder.Default
    private HCaptchaTheme theme = HCaptchaTheme.LIGHT;

}
