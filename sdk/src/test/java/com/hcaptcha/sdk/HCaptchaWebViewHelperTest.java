package com.hcaptcha.sdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HCaptchaWebViewHelperTest {
    private static final String MOCK_HTML = "<html/>";

    @Mock
    Context context;

    @Mock
    HCaptchaConfig config;

    @Mock
    HCaptchaInternalConfig internalConfig;

    @Mock
    IHCaptchaVerifier captchaVerifier;

    @Mock
    HCaptchaWebView webView;

    @Mock
    WebSettings webSettings;

    @Mock
    Handler handler;

    @Mock
    IHCaptchaHtmlProvider htmlProvider;

    MockedStatic<Log> androidLogMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        androidLogMock = mockStatic(Log.class);
        webView = mock(HCaptchaWebView.class);
        webSettings = mock(WebSettings.class);
        htmlProvider = mock(IHCaptchaHtmlProvider.class);
        when(htmlProvider.getHtml()).thenReturn(MOCK_HTML);
        when(webView.getSettings()).thenReturn(webSettings);
        when(internalConfig.getHtmlProvider()).thenReturn(htmlProvider);
    }

    @After
    public void release() {
        androidLogMock.close();
    }

    @Test
    public void test_constructor() {
        new HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier,
                webView);
        verify(webView).loadDataWithBaseURL(null, MOCK_HTML, "text/html", "UTF-8", null);
        verify(webView, times(2)).addJavascriptInterface(any(), anyString());
    }

    @Test
    public void test_destroy() {
        final HCaptchaWebViewHelper webViewHelper = new HCaptchaWebViewHelper(handler, context, config,
                internalConfig, captchaVerifier, webView);
        final ViewGroup viewParent = mock(ViewGroup.class, withSettings().extraInterfaces(ViewParent.class));
        when(webView.getParent()).thenReturn(viewParent);
        webViewHelper.destroy();
        verify(viewParent).removeView(webView);
        verify(webView, times(2)).removeJavascriptInterface(anyString());
    }

    @Test
    @SuppressWarnings("java:S2699") // expect no exception thrown for public API call
    public void test_destroy_webview_parent_null() {
        final HCaptchaWebViewHelper webViewHelper = new HCaptchaWebViewHelper(handler, context, config,
                internalConfig, captchaVerifier, webView);
        webViewHelper.destroy();
    }

    @Test
    public void test_config_host_pased() {
        final String host = "https://my.awesome.host";
        when(config.getHost()).thenReturn(host);
        new HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, webView);
        verify(webView).loadDataWithBaseURL(host, MOCK_HTML, "text/html", "UTF-8", null);
    }
}
