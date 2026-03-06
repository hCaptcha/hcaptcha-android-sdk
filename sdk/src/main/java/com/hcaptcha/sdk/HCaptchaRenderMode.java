package com.hcaptcha.sdk;

/**
 * Controls where visual hCaptcha content is rendered.
 */
public enum HCaptchaRenderMode {
    /**
     * Render visual challenge in SDK dialog flow.
     */
    DIALOG,
    /**
     * Render visual challenge embedded in host container provided by integrator.
     */
    EMBEDDED,
    /**
     * Run headless flow (no visual dialog), intended for passive sitekeys.
     */
    HEADLESS
}
