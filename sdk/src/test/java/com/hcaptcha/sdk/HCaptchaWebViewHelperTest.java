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

    @NonNull
    HCaptchaWebViewHelper subject;

    MockedStatic<Log> androidLogMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        androidLogMock = mockStatic(Log.class);

        subject = new HCaptchaWebViewHelper(context, config, webViewProvider);
    }

    @After
    public void release() {
        androidLogMock.close();
    }

    @Test
    public void test_setup() {
        WebView webView = mock(WebView.class);
        when(webViewProvider.getWebView()).thenReturn(webView);
        WebSettings webSettings = mock(WebSettings.class);
        when(webView.getSettings()).thenReturn(webSettings);

        subject.setup();

        verify(webView).loadUrl(HCaptchaWebViewHelper.HCAPTCHA_URL);
        verify(webView, times(2)).addJavascriptInterface(any(), anyString());
    }

    @Test
    public void test_setup_not_fail_for_null_webview() {
        subject.setup();

        verify(webViewProvider).getWebView();
    }

    @Test
    public void test_cleanup() {
        WebView webView = mock(WebView.class);
        when(webViewProvider.getWebView()).thenReturn(webView);
        ViewGroup viewParent = mock(ViewGroup.class, withSettings().extraInterfaces(ViewParent.class));
        when(webView.getParent()).thenReturn(viewParent);

        subject.cleanup();

        verify(viewParent).removeView(webView);
        verify(webView, times(2)).removeJavascriptInterface(anyString());
    }

    @Test
    public void test_cleanup_not_fail_for_null_webview() {
        subject.cleanup();

        verify(webViewProvider).getWebView();
    }

    @Test
    public void test_cleanup_webview_parent_null() {
        WebView webView = mock(WebView.class);
        when(webViewProvider.getWebView()).thenReturn(webView);

        subject.cleanup();

        verify(webViewProvider).getWebView();
    }
}
