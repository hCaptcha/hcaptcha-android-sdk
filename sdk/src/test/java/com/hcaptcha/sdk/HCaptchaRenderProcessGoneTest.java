package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// A dead renderer must not take the host app down with it (#214).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaRenderProcessGoneTest {
    private static final String MOCK_HTML = "<html/>";

    private final HCaptchaConfig config = mock(HCaptchaConfig.class);
    private final HCaptchaInternalConfig internalConfig = mock(HCaptchaInternalConfig.class);
    private final IHCaptchaVerifier captchaVerifier = mock(IHCaptchaVerifier.class);
    private final HCaptchaWebView webView = mock(HCaptchaWebView.class);
    private final Handler handler = mock(Handler.class);

    private WebViewClient webViewClient;

    @Before
    public void init() {
        final IHCaptchaHtmlProvider htmlProvider = mock(IHCaptchaHtmlProvider.class);
        when(htmlProvider.getHtml()).thenReturn(MOCK_HTML);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        when(internalConfig.getHtmlProvider()).thenReturn(htmlProvider);

        final Context context = mock(Context.class);
        new HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, webView);

        final ArgumentCaptor<WebViewClient> captor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(webView).setWebViewClient(captor.capture());
        webViewClient = captor.getValue();
    }

    private static RenderProcessGoneDetail rendererGone(final boolean didCrash) {
        final RenderProcessGoneDetail detail = mock(RenderProcessGoneDetail.class);
        when(detail.didCrash()).thenReturn(didCrash);
        return detail;
    }

    // Returning false would make the framework kill the host app.
    @Test
    public void crashedRendererIsHandled() {
        assertTrue(webViewClient.onRenderProcessGone(mock(WebView.class), rendererGone(true)));
    }

    // The system also reclaims renderers under memory pressure, without a crash.
    @Test
    public void reclaimedRendererIsHandled() {
        assertTrue(webViewClient.onRenderProcessGone(mock(WebView.class), rendererGone(false)));
    }

    // Not onFailure: that path may retry on the dead WebView.
    @Test
    public void verifierGetsGenericError() {
        webViewClient.onRenderProcessGone(mock(WebView.class), rendererGone(true));

        final ArgumentCaptor<HCaptchaException> captor = ArgumentCaptor.forClass(HCaptchaException.class);
        verify(captchaVerifier).onWebViewGone(captor.capture());
        verify(captchaVerifier, never()).onFailure(any());
        assertEquals(HCaptchaError.ERROR, captor.getValue().getHCaptchaError());
        assertEquals("WebView render process did terminate", captor.getValue().getMessage());
    }
}
