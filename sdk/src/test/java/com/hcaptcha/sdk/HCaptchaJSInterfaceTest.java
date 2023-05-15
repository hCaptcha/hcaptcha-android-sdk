package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(MockitoJUnitRunner.class)
public class HCaptchaJSInterfaceTest {

    @Mock
    Handler handler;

    @Spy
    IHCaptchaVerifier captchaVerifier;

    @Captor
    ArgumentCaptor<String> tokenCaptor;

    @Captor
    ArgumentCaptor<HCaptchaException> exceptionCaptor;

    HCaptchaConfig testConfig = HCaptchaConfig.builder().siteKey("0000-1111-2222-3333").build();

    @Before
    public void init() {
        when(handler.post(any(Runnable.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        });
    }

    @Test
    public void full_config_serialization() throws JSONException {
        final String siteKey = "0000-1111-2222-3333";
        final String locale = "ro";
        final HCaptchaOrientation orientation = HCaptchaOrientation.PORTRAIT;
        final HCaptchaSize size = HCaptchaSize.NORMAL;
        final String rqdata = "custom rqdata";
        final String jsSrc = "127.0.0.1/api.js";
        final String endpoint = "https://example.com/endpoint";
        final String assethost = "https://example.com/assethost";
        final String imghost = "https://example.com/imghost";
        final String reportapi = "https://example.com/reportapi";
        final String host = "custom-host";
        final long timeout = 60;
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(siteKey)
                .locale(locale)
                .size(size)
                .orientation(orientation)
                .theme(HCaptchaTheme.DARK)
                .rqdata(rqdata)
                .jsSrc(jsSrc)
                .endpoint(endpoint)
                .assethost(assethost)
                .imghost(imghost)
                .reportapi(reportapi)
                .host(host)
                .resetOnTimeout(true)
                .hideDialog(true)
                .tokenExpiration(timeout)
                .diagnosticLog(true)
                .disableHardwareAcceleration(false)
                .build();
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, config, captchaVerifier);

        final JSONObject expected = new JSONObject();
        expected.put("siteKey", siteKey);
        expected.put("sentry", true);
        expected.put("loading", true);
        expected.put("rqdata", rqdata);
        expected.put("jsSrc", jsSrc);
        expected.put("endpoint", endpoint);
        expected.put("reportapi", reportapi);
        expected.put("assethost", assethost);
        expected.put("imghost", imghost);
        expected.put("locale", locale);
        expected.put("size", size.toString());
        expected.put("orientation", orientation.toString());
        expected.put("theme", HCaptchaTheme.DARK.toString());
        expected.put("customTheme", JSONObject.NULL);
        expected.put("host", host);
        expected.put("resetOnTimeout", true);
        expected.put("hideDialog", true);
        expected.put("tokenExpiration", timeout);
        expected.put("diagnosticLog", true);
        expected.put("disableHardwareAcceleration", false);

        JSONAssert.assertEquals(jsInterface.getConfig(), expected, false);
    }

    @Test
    public void subset_config_serialization() throws JSONException {
        final String siteKey = "0000-1111-2222-3333";
        final String locale = "ro";
        final HCaptchaSize size = HCaptchaSize.NORMAL;
        final HCaptchaOrientation orientation = HCaptchaOrientation.LANDSCAPE;
        final String rqdata = "custom rqdata";
        final long defaultTimeout = 120;
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(siteKey)
                .locale(locale)
                .size(size)
                .orientation(orientation)
                .theme(HCaptchaTheme.DARK)
                .rqdata(rqdata)
                .build();
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, config, captchaVerifier);

        final JSONObject expected = new JSONObject();
        expected.put("siteKey", siteKey);
        expected.put("sentry", true);
        expected.put("loading", true);
        expected.put("rqdata", rqdata);
        expected.put("jsSrc", "https://js.hcaptcha.com/1/api.js");
        expected.put("endpoint", JSONObject.NULL);
        expected.put("reportapi", JSONObject.NULL);
        expected.put("assethost", JSONObject.NULL);
        expected.put("imghost", JSONObject.NULL);
        expected.put("locale", locale);
        expected.put("size", size.toString());
        expected.put("orientation", orientation.toString());
        expected.put("theme", HCaptchaTheme.DARK.toString());
        expected.put("customTheme", JSONObject.NULL);
        expected.put("host", JSONObject.NULL);
        expected.put("resetOnTimeout", false);
        expected.put("hideDialog", false);
        expected.put("tokenExpiration", defaultTimeout);
        expected.put("diagnosticLog", false);
        expected.put("disableHardwareAcceleration", true);

        JSONAssert.assertEquals(jsInterface.getConfig(), expected, false);
    }

    @Test
    public void calls_on_challenge_ready() {
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, testConfig, captchaVerifier);
        jsInterface.onLoaded();
        verify(captchaVerifier, times(1)).onLoaded();
    }

    @Test
    public void calls_on_challenge_visible_cb() {
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, testConfig, captchaVerifier);
        jsInterface.onOpen();
        verify(captchaVerifier, times(1)).onOpen();
    }

    @Test
    public void on_pass_forwards_token_to_listeners() {
        final String token = "mock-token";
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, testConfig, captchaVerifier);
        jsInterface.onPass(token);
        verify(captchaVerifier, times(1)).onSuccess(tokenCaptor.capture());
        assertEquals(token, tokenCaptor.getValue());
    }

    @Test
    public void on_error_forwards_error_to_listeners() {
        final HCaptchaError error = HCaptchaError.CHALLENGE_CLOSED;
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, testConfig, captchaVerifier);
        jsInterface.onError(error.getErrorId());
        verify(captchaVerifier, times(1)).onFailure(exceptionCaptor.capture());
        assertEquals(error.getMessage(), exceptionCaptor.getValue().getMessage());
        assertNotNull(exceptionCaptor.getValue());
    }
}
