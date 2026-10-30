package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Exercise real client/verifier lifecycles; mocking the verifier hides dead-WebView reuse.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaRenderProcessRecoveryTest {
    private final HCaptchaRenderMode renderMode;
    private final boolean didCrash;
    private final List<HCaptchaException> failures = new ArrayList<>();

    private MockedConstruction<HCaptchaWebView> webViews;
    private HCaptcha client;
    private HCaptchaConfig config;
    private ViewGroup rootView;
    private WebViewClient webViewClient;
    private HCaptchaJSInterface jsInterface;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}, didCrash={1}")
    public static Collection<Object[]> renderModesAndTerminationReasons() {
        return Arrays.asList(new Object[][] {
                {HCaptchaRenderMode.HEADLESS, true},
                {HCaptchaRenderMode.HEADLESS, false},
                {HCaptchaRenderMode.EMBEDDED, true},
                {HCaptchaRenderMode.EMBEDDED, false},
                {HCaptchaRenderMode.DIALOG, true},
                {HCaptchaRenderMode.DIALOG, false}
        });
    }

    public HCaptchaRenderProcessRecoveryTest(HCaptchaRenderMode mode, boolean crashed) {
        this.renderMode = mode;
        this.didCrash = crashed;
    }

    @Before
    public void init() {
        webViews = mockConstruction(HCaptchaWebView.class,
                (view, context) -> when(view.getSettings()).thenReturn(mock(WebSettings.class)));

        final FragmentActivity activity = mock(FragmentActivity.class);
        final Window window = mock(Window.class);
        rootView = mock(ViewGroup.class);
        when(activity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(rootView);
        when(rootView.getRootView()).thenReturn(rootView);

        final FragmentManager fragmentManager = mock(FragmentManager.class);
        when(activity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(fragmentManager.beginTransaction()).thenReturn(mock(FragmentTransaction.class, RETURNS_SELF));

        config = HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .renderMode(renderMode)
                .size(HCaptchaSize.INVISIBLE)
                .loading(false)
                .retryPredicate((currentConfig, exception) -> exception.getHCaptchaError() == HCaptchaError.ERROR)
                .build();
        final IHCaptchaHtmlProvider htmlProvider = mock(IHCaptchaHtmlProvider.class);
        when(htmlProvider.getHtml()).thenReturn("<html/>");
        final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
                .htmlProvider(htmlProvider)
                .build();
        client = HCaptcha.getClient(activity, internalConfig).setEmbeddedContainer(rootView);
        client.addOnFailureListener(failures::add);
    }

    @After
    public void tearDown() {
        try {
            if (client != null) {
                client.destroy();
            }
        } finally {
            if (webViews != null) {
                webViews.close();
            }
        }
    }

    @Test
    public void nextVerificationMustNotReuseDestroyedWebView() {
        // Ordinary failure handling must leave a reusable client, without a manual reset().
        config = config.toBuilder().retryPredicate((currentConfig, exception) -> false).build();
        final HCaptchaWebView deadWebView = setupLoadedClient();
        terminateRenderer(deadWebView);
        assertEquals(1, failures.size());

        client.verifyWithHCaptcha(config);

        assertEquals("The next verification must create a fresh WebView", 2, webViews.constructed().size());
        verify(deadWebView, never()).loadUrl(anyString());
    }

    @Test
    public void retryPredicateMustNotSwallowRendererTermination() {
        final HCaptchaWebView deadWebView = setupLoadedClient();
        terminateRenderer(deadWebView);

        // Either terminal notification or recovery on a NEW WebView is valid; resetting
        // JavaScript on the destroyed instance and suppressing the listener is not.
        assertTrue("Renderer termination must be reported or retried on a fresh WebView",
                failures.size() == 1 || webViews.constructed().size() > 1);
        verify(deadWebView, never()).loadUrl(anyString());
        if (!failures.isEmpty()) {
            assertEquals(HCaptchaError.ERROR, failures.get(0).getHCaptchaError());
        }
    }

    @Test
    public void ordinaryErrorsStillHonorRetryPredicate() {
        final HCaptchaWebView webView = setupLoadedClient();

        jsInterface.onError(HCaptchaError.ERROR.getErrorId());
        shadowOf(Looper.getMainLooper()).idle();

        verify(webView).loadUrl("javascript:reset();");
        verify(webView).loadUrl("javascript:execute();");
        verify(webView, never()).destroy();
        assertTrue("Retryable JavaScript errors should remain silent", failures.isEmpty());
        assertEquals(1, webViews.constructed().size());
    }

    private HCaptchaWebView setupLoadedClient() {
        client.setup(config);
        final HCaptchaWebView webView = webViews.constructed().get(0);
        final ArgumentCaptor<WebViewClient> clientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(webView).setWebViewClient(clientCaptor.capture());
        webViewClient = clientCaptor.getValue();
        final ArgumentCaptor<HCaptchaJSInterface> bridgeCaptor = ArgumentCaptor.forClass(HCaptchaJSInterface.class);
        verify(webView).addJavascriptInterface(bridgeCaptor.capture(), eq(HCaptchaJSInterface.JS_INTERFACE_TAG));
        jsInterface = bridgeCaptor.getValue();
        jsInterface.onLoaded();
        shadowOf(Looper.getMainLooper()).idle();
        clearInvocations(webView);
        return webView;
    }

    private void terminateRenderer(HCaptchaWebView webView) {
        final RenderProcessGoneDetail detail = mock(RenderProcessGoneDetail.class);
        when(detail.didCrash()).thenReturn(didCrash);
        when(webView.getParent()).thenReturn(rootView);

        assertTrue(webViewClient.onRenderProcessGone(webView, detail));

        final InOrder cleanup = inOrder(rootView, webView);
        cleanup.verify(rootView).removeView(webView);
        cleanup.verify(webView).destroy();
    }
}
