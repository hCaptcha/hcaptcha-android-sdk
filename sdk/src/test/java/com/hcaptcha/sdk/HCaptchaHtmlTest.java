package com.hcaptcha.sdk;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class HCaptchaHtmlTest {

    @Test
    public void html_not_empty() {
        assertFalse(new HCaptchaHtml().getHtml().isEmpty());
    }
}
