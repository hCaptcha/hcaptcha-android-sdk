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

    @Mock
    Context context;

    @Mock
    HCaptchaConfig config;

    @Mock
    HCaptchaWebViewProvider webViewProvider;

    @Mock
    HCaptchaStateListener hCaptchaStateListener;

    @Mock
    WebView webView;

    @Mock
    WebSettings webSettings;

    @NonNull
    HCaptchaWebViewHelper webViewHelper;

    MockedStatic<Log> androidLogMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        androidLogMock = mockStatic(Log.class);
        hCaptchaStateListener = mock(HCaptchaStateListener.class);
        webView = mock(WebView.class);
        webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);
        webViewHelper = new HCaptchaWebViewHelper(context, config, webViewProvider, hCaptchaStateListener, webView);
    }

    @After
    public void release() {
        androidLogMock.close();
    }

    @Test
    public void test_constructor() {
        verify(webView).loadUrl(HCaptchaWebViewHelper.HCAPTCHA_URL);
        verify(webView, times(2)).addJavascriptInterface(any(), anyString());
    }

    @Test
    public void test_destroy() {
        ViewGroup viewParent = mock(ViewGroup.class, withSettings().extraInterfaces(ViewParent.class));
        when(webView.getParent()).thenReturn(viewParent);
        webViewHelper.destroy();
        verify(viewParent).removeView(webView);
        verify(webView, times(2)).removeJavascriptInterface(anyString());
    }

    @Test
    public void test_destroy_webview_parent_null() {
        webViewHelper.destroy();
    }
}
