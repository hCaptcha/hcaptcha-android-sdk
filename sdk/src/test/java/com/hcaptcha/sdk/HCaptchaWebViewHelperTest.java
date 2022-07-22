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
import android.webkit.WebView;
import androidx.annotation.NonNull;

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
    IHCaptchaVerifier captchaVerifier;

    @Mock
    HCaptchaStateListener stateListener;

    @Mock
    WebView webView;

    @Mock
    WebSettings webSettings;

    @NonNull
    HCaptchaWebViewHelper webViewHelper;

    @Mock
    Handler handler;

    @Mock
    IHCaptchaHtmlProvider htmlProvider;

    MockedStatic<Log> androidLogMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        androidLogMock = mockStatic(Log.class);
        stateListener = mock(HCaptchaStateListener.class);
        webView = mock(WebView.class);
        webSettings = mock(WebSettings.class);
        htmlProvider = mock(IHCaptchaHtmlProvider.class);
        when(htmlProvider.getHtml()).thenReturn(MOCK_HTML);
        when(webView.getSettings()).thenReturn(webSettings);
    }

    @After
    public void release() {
        androidLogMock.close();
    }

    @Test
    public void test_constructor() {
        webViewHelper = new HCaptchaWebViewHelper(handler, context, config, captchaVerifier,
                stateListener, webView, htmlProvider);
        verify(webView).loadDataWithBaseURL(null, MOCK_HTML, "text/html", "UTF-8", null);
        verify(webView, times(2)).addJavascriptInterface(any(), anyString());
    }

    @Test
    public void test_destroy() {
        webViewHelper = new HCaptchaWebViewHelper(handler, context, config, captchaVerifier,
                stateListener, webView, htmlProvider);
        final ViewGroup viewParent = mock(ViewGroup.class, withSettings().extraInterfaces(ViewParent.class));
        when(webView.getParent()).thenReturn(viewParent);
        webViewHelper.destroy();
        verify(viewParent).removeView(webView);
        verify(webView, times(2)).removeJavascriptInterface(anyString());
    }

    @Test
    public void test_destroy_webview_parent_null() {
        webViewHelper = new HCaptchaWebViewHelper(handler, context, config, captchaVerifier,
                stateListener, webView, htmlProvider);
        webViewHelper.destroy();
    }

    @Test
    public void test_config_host_pased() {
        final String host = "https://my.awesome.host";
        when(config.getHost()).thenReturn(host);
        webViewHelper = new HCaptchaWebViewHelper(handler, context, config, captchaVerifier,
                stateListener, webView, htmlProvider);
        verify(webView).loadDataWithBaseURL(host, MOCK_HTML, "text/html", "UTF-8", null);
    }
}
