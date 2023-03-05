package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;

public class HCaptchaConfigTest {

    public static final String MOCK_SITE_KEY = "mocked-site-key";

    @Test
    public void custom_locale() {
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey(MOCK_SITE_KEY).locale("ro").build();
        assertEquals("ro", config.getLocale());
    }

    @Test
    public void default_confis() {
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey(MOCK_SITE_KEY).build();
        assertEquals(MOCK_SITE_KEY, config.getSiteKey());
        assertEquals(true, config.getSentry());
        assertEquals(HCaptchaSize.INVISIBLE, config.getSize());
        assertEquals(HCaptchaTheme.LIGHT, config.getTheme());
        assertEquals(Locale.getDefault().getLanguage(), config.getLocale());
        assertEquals("https://js.hcaptcha.com/1/api.js", config.getJsSrc());
        assertEquals(null, config.getCustomTheme());
        assertNull(config.getRqdata());
    }

    @Test
    public void custom_config() {
        final HCaptchaSize hCaptchaSize = HCaptchaSize.COMPACT;
        final HCaptchaTheme hCaptchaTheme = HCaptchaTheme.DARK;
        final String customRqdata = "custom rqdata value";
        final String customEndpoint = "https://local/api.js";
        final String customLocale = "ro";
        final Boolean sentry = false;
        final String customTheme = "{ \"palette\": {"
                + "\"mode\": \"light\", \"primary\": { \"main\": \"#F16622\" },"
                + "\"warn\": {  \"main\": \"#F16622\" },"
                + "\"text\": { \"heading\": \"#F16622\", \"body\": \"#F16622\" } } }";

        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(MOCK_SITE_KEY)
                .jsSrc(customEndpoint)
                .locale(customLocale)
                .rqdata(customRqdata)
                .sentry(sentry)
                .theme(hCaptchaTheme)
                .size(hCaptchaSize)
                .customTheme(customTheme)
                .build();
        assertEquals(MOCK_SITE_KEY, config.getSiteKey());
        assertEquals(sentry, config.getSentry());
        assertEquals(hCaptchaSize, config.getSize());
        assertEquals(hCaptchaTheme, config.getTheme());
        assertEquals(customLocale, config.getLocale());
        assertEquals(customRqdata, config.getRqdata());
        assertEquals(customEndpoint, config.getJsSrc());
        assertEquals(customTheme, config.getCustomTheme());
    }

    @Test
    public void serialization() throws Exception {
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey(MOCK_SITE_KEY).build();

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(config);
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final HCaptchaConfig deserializedObject = (HCaptchaConfig) ois.readObject();

        assertEquals(config.toBuilder().retryPredicate(null).build(),
                deserializedObject.toBuilder().retryPredicate(null).build());
    }

}
