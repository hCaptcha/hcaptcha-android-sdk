package com.hcaptcha.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class HCaptchaJSInterfaceTest {

    @Spy
    OnLoadedListener onLoadedListener;

    @Spy
    OnSuccessListener<HCaptchaTokenResponse> onSuccessListener;

    @Spy
    OnFailureListener onFailureListener;

    @Captor
    ArgumentCaptor<HCaptchaTokenResponse> tokenCaptor;

    @Captor
    ArgumentCaptor<HCaptchaException> exceptionCaptor;


    @Test
    public void full_config_serialization() throws JsonProcessingException {
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey("0000-1111-2222-3333")
                .locale("ro")
                .size(HCaptchaSize.NORMAL)
                .theme(HCaptchaTheme.DARK)
                .rqdata("custom rqdata")
                .apiEndpoint("127.0.0.1/api.js")
                .endpoint("https://example.com/endpoint")
                .assethost("https://example.com/assethost")
                .imghost("https://example.com/imghost")
                .reportapi("https://example.com/reportapi")
                .host("custom-host")
                .build();
        final HCaptchaJSInterface HCaptchaJsInterface = new HCaptchaJSInterface(config, null, null, null);
        assertEquals("{\"siteKey\":\"0000-1111-2222-3333\",\"sentry\":true,\"loading\":true,\"rqdata\":\"custom rqdata\",\"apiEndpoint\":\"127.0.0.1/api.js\",\"endpoint\":\"https://example.com/endpoint\",\"reportapi\":\"https://example.com/reportapi\",\"assethost\":\"https://example.com/assethost\",\"imghost\":\"https://example.com/imghost\",\"locale\":\"ro\",\"size\":\"normal\",\"theme\":\"dark\",\"host\":\"custom-host\"}",
                HCaptchaJsInterface.getConfig());
    }

    @Test
    public void subset_config_serialization() throws JsonProcessingException {
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey("0000-1111-2222-3333")
                .locale("ro")
                .size(HCaptchaSize.NORMAL)
                .theme(HCaptchaTheme.DARK)
                .rqdata("custom rqdata")
                .build();
        final HCaptchaJSInterface HCaptchaJsInterface = new HCaptchaJSInterface(config, null, null, null);
        assertEquals("{\"siteKey\":\"0000-1111-2222-3333\",\"sentry\":true,\"loading\":true,\"rqdata\":\"custom rqdata\",\"apiEndpoint\":\"https://js.hcaptcha.com/1/api.js\",\"endpoint\":null,\"reportapi\":null,\"assethost\":null,\"imghost\":null,\"locale\":\"ro\",\"size\":\"normal\",\"theme\":\"dark\",\"host\":null}",
                HCaptchaJsInterface.getConfig());
    }

    @Test
    public void calls_on_challenge_visible_cb() {
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, onLoadedListener, null, null);
        hCaptchaJSInterface.onLoaded();
        verify(onLoadedListener, times(1)).onLoaded();
    }

    @Test
    public void on_pass_forwards_token_to_listeners() {
        final String token = "mock-token";
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, null, onSuccessListener, null);
        hCaptchaJSInterface.onPass(token);
        verify(onSuccessListener, times(1)).onSuccess(tokenCaptor.capture());
        assertEquals(token, tokenCaptor.getValue().getTokenResult());
    }

    @Test
    public void on_error_forwards_error_to_listeners() {
        final HCaptchaError error = HCaptchaError.CHALLENGE_CLOSED;
        final HCaptchaJSInterface hCaptchaJSInterface = new HCaptchaJSInterface(null, null, null, onFailureListener);
        hCaptchaJSInterface.onError(error.getErrorId());
        verify(onFailureListener, times(1)).onFailure(exceptionCaptor.capture());
        assertEquals(error.getMessage(), exceptionCaptor.getValue().getMessage());
        assertNotNull(exceptionCaptor.getValue());
    }

}
