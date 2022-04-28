package com.hcaptcha.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class HCaptchaJSInterfaceTest {

    @Spy
    OnLoadedListener onLoadedListener;

    @Spy
    OnOpenListener onOpenListener;

    @Spy
    OnSuccessListener<HCaptchaTokenResponse> onSuccessListener;

    @Spy
    OnFailureListener onFailureListener;

    @Captor
    ArgumentCaptor<HCaptchaTokenResponse> tokenCaptor;

    @Captor
    ArgumentCaptor<HCaptchaException> exceptionCaptor;


    @Test
    public void full_config_serialization() throws JsonProcessingException, JSONException {
        final String siteKey = "0000-1111-2222-3333";
        final String locale = "ro";
        final HCaptchaSize size = HCaptchaSize.NORMAL;
        final String rqdata = "custom rqdata";
        final String apiEndpoint = "127.0.0.1/api.js";
        final String endpoint = "https://example.com/endpoint";
        final String assethost = "https://example.com/assethost";
        final String imghost = "https://example.com/imghost";
        final String reportapi = "https://example.com/reportapi";
        final String host = "custom-host";
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(siteKey)
                .locale(locale)
                .size(size)
                .theme(HCaptchaTheme.DARK)
                .rqdata(rqdata)
                .apiEndpoint(apiEndpoint)
                .endpoint(endpoint)
                .assethost(assethost)
                .imghost(imghost)
                .reportapi(reportapi)
                .host(host)
                .resetOnTimeout(true)
                .build();
        final HCaptchaJSInterface HCaptchaJsInterface = new HCaptchaJSInterface(config, null, null, null, null);

        JSONObject expected = new JSONObject();
        expected.put("siteKey", siteKey);
        expected.put("sentry", true);
        expected.put("loading", true);
        expected.put("rqdata", rqdata);
        expected.put("apiEndpoint", apiEndpoint);
        expected.put("endpoint", endpoint);
        expected.put("reportapi", reportapi);
        expected.put("assethost", assethost);
        expected.put("imghost", imghost);
        expected.put("locale", locale);
        expected.put("size", size.toString());
        expected.put("theme", HCaptchaTheme.DARK.toString());
        expected.put("customTheme", JSONObject.NULL);
        expected.put("host", host);
        expected.put("resetOnTimeout", true);

        JSONAssert.assertEquals(HCaptchaJsInterface.getConfig(), expected, false);
    }

    @Test
    public void subset_config_serialization() throws JsonProcessingException, JSONException {
        final String siteKey = "0000-1111-2222-3333";
        final String locale = "ro";
        final HCaptchaSize size = HCaptchaSize.NORMAL;
        final String rqdata = "custom rqdata";
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(siteKey)
                .locale(locale)
                .size(size)
                .theme(HCaptchaTheme.DARK)
                .rqdata(rqdata)
                .build();
        final HCaptchaJSInterface HCaptchaJsInterface = new HCaptchaJSInterface(config, null, null, null, null);

        JSONObject expected = new JSONObject();
        expected.put("siteKey", siteKey);
        expected.put("sentry", true);
        expected.put("loading", true);
        expected.put("rqdata", rqdata);
        expected.put("apiEndpoint", "https://js.hcaptcha.com/1/api.js");
        expected.put("endpoint", JSONObject.NULL);
        expected.put("reportapi", JSONObject.NULL);
        expected.put("assethost", JSONObject.NULL);
        expected.put("imghost", JSONObject.NULL);
        expected.put("locale", locale);
        expected.put("size", size.toString());
        expected.put("theme", HCaptchaTheme.DARK.toString());
        expected.put("customTheme", JSONObject.NULL);
        expected.put("host", JSONObject.NULL);
        expected.put("resetOnTimeout", false);

        JSONAssert.assertEquals(HCaptchaJsInterface.getConfig(), expected, false);
    }

    @Test
    public void calls_on_challenge_ready() {
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, onLoadedListener, null, null, null);
        hCaptchaJSInterface.onLoaded();
        verify(onLoadedListener, times(1)).onLoaded();
    }

    @Test
    public void calls_on_challenge_visible_cb() {
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, null, onOpenListener, null, null);
        hCaptchaJSInterface.onOpen();
        verify(onOpenListener, times(1)).onOpen();
    }

    @Test
    public void on_pass_forwards_token_to_listeners() {
        final String token = "mock-token";
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, null, null, onSuccessListener, null);
        hCaptchaJSInterface.onPass(token);
        verify(onSuccessListener, times(1)).onSuccess(tokenCaptor.capture());
        assertEquals(token, tokenCaptor.getValue().getTokenResult());
    }

    @Test
    public void on_error_forwards_error_to_listeners() {
        final HCaptchaError error = HCaptchaError.CHALLENGE_CLOSED;
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, null, null, null, onFailureListener);
        hCaptchaJSInterface.onError(error.getErrorId());
        verify(onFailureListener, times(1)).onFailure(exceptionCaptor.capture());
        assertEquals(error.getMessage(), exceptionCaptor.getValue().getMessage());
        assertNotNull(exceptionCaptor.getValue());
    }
}
