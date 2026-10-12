package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

// How an sms: link from either WebView callback becomes a compose intent, and the fallbacks.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaSmsHandoffTest {
    private static final String MOCK_HTML = "<html/>";
    private static final String SMS_BODY_EXTRA = "sms_body";
    private static final String NUMBER = "+46769432675";
    private static final String BODY = "Return to the app and press Confirm after sending this "
            + "message. Do not edit or share the code: mcsp-u3oz-s4du";
    private static final String LIVE_LINK = "sms:+46769432675?&body=Return%20to%20the%20app%20and"
            + "%20press%20Confirm%20after%20sending%20this%20message.%20Do%20not%20edit%20or%20"
            + "share%20the%20code%3A%20mcsp-u3oz-s4du";

    private final HCaptchaConfig config = mock(HCaptchaConfig.class);
    private final HCaptchaInternalConfig internalConfig = mock(HCaptchaInternalConfig.class);
    private final IHCaptchaVerifier captchaVerifier = mock(IHCaptchaVerifier.class);
    private final HCaptchaWebView webView = mock(HCaptchaWebView.class);
    private final Handler handler = mock(Handler.class);

    private WebViewClient webViewClient;
    private WebChromeClient chromeClient;

    @Before
    public void init() {
        final IHCaptchaHtmlProvider htmlProvider = mock(IHCaptchaHtmlProvider.class);
        when(htmlProvider.getHtml()).thenReturn(MOCK_HTML);
        when(webView.getSettings()).thenReturn(mock(WebSettings.class));
        when(internalConfig.getHtmlProvider()).thenReturn(htmlProvider);
    }

    private void createHelper(final Context context) {
        new HCaptchaWebViewHelper(handler, context, config, internalConfig, captchaVerifier, webView);

        final ArgumentCaptor<WebViewClient> webClientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(webView).setWebViewClient(webClientCaptor.capture());
        webViewClient = webClientCaptor.getValue();

        final ArgumentCaptor<WebChromeClient> chromeCaptor = ArgumentCaptor.forClass(WebChromeClient.class);
        verify(webView).setWebChromeClient(chromeCaptor.capture());
        chromeClient = chromeCaptor.getValue();
    }

    private static WebResourceRequest navigationTo(final String url) {
        final WebResourceRequest request = mock(WebResourceRequest.class);
        when(request.getUrl()).thenReturn(Uri.parse(url));
        return request;
    }

    // A target="_blank" anchor reaches the chrome client with the link only in the hit-test result.
    private static WebView anchorHitOn(final String url) {
        final WebView.HitTestResult hit = mock(WebView.HitTestResult.class);
        when(hit.getType()).thenReturn(WebView.HitTestResult.SRC_ANCHOR_TYPE);
        when(hit.getExtra()).thenReturn(url);
        final WebView opener = mock(WebView.class);
        when(opener.getHitTestResult()).thenReturn(hit);
        return opener;
    }

    private static List<Intent> startedActivities(final Context context, final int count) {
        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(count)).startActivity(captor.capture());
        return captor.getAllValues();
    }

    private static void assertComposeIntent(final Intent intent) {
        assertEquals(Intent.ACTION_SENDTO, intent.getAction());
        assertEquals(Uri.parse("smsto:" + NUMBER), intent.getData());
        assertEquals(BODY, intent.getStringExtra(SMS_BODY_EXTRA));
        assertEquals(BODY, intent.getStringExtra(Intent.EXTRA_TEXT));
    }

    private static boolean hasNewTaskFlag(final Intent intent) {
        return (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
    }

    @Test
    public void navigationToSmsLinkOpensComposerInsideTheActivityTask() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        final Intent intent = startedActivities(activity, 1).get(0);
        assertComposeIntent(intent);
        assertFalse("an Activity context must not add FLAG_ACTIVITY_NEW_TASK", hasNewTaskFlag(intent));
        verify(captchaVerifier, never()).onFailure(any());
    }

    @Test
    public void wrappedActivityIsUnwrappedBeforeDecidingOnNewTask() {
        // Hosts routinely pass a themed/Compose/Hilt ContextWrapper around their Activity.
        final Activity activity = mock(Activity.class);
        final Context wrapped = new ContextWrapper(new ContextWrapper(activity));
        createHelper(wrapped);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        final Intent intent = startedActivities(activity, 1).get(0);
        assertComposeIntent(intent);
        assertFalse("a wrapped Activity must not add FLAG_ACTIVITY_NEW_TASK", hasNewTaskFlag(intent));
    }

    @Test
    public void nonActivityContextAddsNewTaskFlag() {
        final Context application = mock(Context.class);
        createHelper(application);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        final Intent intent = startedActivities(application, 1).get(0);
        assertComposeIntent(intent);
        assertTrue("startActivity from a non-Activity context needs FLAG_ACTIVITY_NEW_TASK",
                hasNewTaskFlag(intent));
    }

    @Test
    public void wrapperAroundNonActivityContextStillAddsNewTaskFlag() {
        final Context application = mock(Context.class);
        createHelper(new ContextWrapper(application));

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        assertTrue(hasNewTaskFlag(startedActivities(application, 1).get(0)));
    }

    @Test
    public void newWindowSmsLinkOpensComposerAndReportsNoWindowCreated() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        // No child WebView is created, so `true` would leave the opener's window.open() pending.
        final boolean created = chromeClient.onCreateWindow(anchorHitOn(LIVE_LINK), false, true, null);

        assertFalse(created);
        final Intent intent = startedActivities(activity, 1).get(0);
        assertComposeIntent(intent);
        assertFalse(hasNewTaskFlag(intent));
        verify(captchaVerifier, never()).onFailure(any());
    }

    @Test
    public void newWindowNonSmsLinkOpensBrowserAndReportsNoWindowCreated() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        final boolean created = chromeClient.onCreateWindow(
                anchorHitOn("https://www.hcaptcha.com/privacy"), false, true, null);

        assertFalse(created);
        final Intent intent = startedActivities(activity, 1).get(0);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(Uri.parse("https://www.hcaptcha.com/privacy"), intent.getData());
        verify(captchaVerifier, never()).onFailure(any());
    }

    @Test
    public void newWindowWithoutUserGestureIsIgnored() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        assertFalse(chromeClient.onCreateWindow(anchorHitOn(LIVE_LINK), false, false, null));

        verify(activity, never()).startActivity(any());
    }

    @Test
    public void fallsBackToRawLinkWhenNoAppTakesTheComposeIntent() {
        final Activity activity = mock(Activity.class);
        // The first launch (ACTION_SENDTO) fails, the second (ACTION_VIEW on the raw link) works.
        doThrow(new ActivityNotFoundException("no smsto handler"))
                .doNothing()
                .when(activity).startActivity(any());
        createHelper(activity);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        final List<Intent> intents = startedActivities(activity, 2);
        assertComposeIntent(intents.get(0));
        assertEquals(Intent.ACTION_VIEW, intents.get(1).getAction());
        assertEquals(Uri.parse(LIVE_LINK), intents.get(1).getData());
        assertFalse(hasNewTaskFlag(intents.get(1)));
        verify(captchaVerifier, never()).onFailure(any());
    }

    @Test
    public void reportsExactlyOneInternalErrorWhenBothLaunchesFail() {
        final Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException("no messaging app")).when(activity).startActivity(any());
        createHelper(activity);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo(LIVE_LINK)));

        final List<Intent> intents = startedActivities(activity, 2);
        assertEquals(Intent.ACTION_SENDTO, intents.get(0).getAction());
        assertEquals(Intent.ACTION_VIEW, intents.get(1).getAction());

        final ArgumentCaptor<HCaptchaException> failure = ArgumentCaptor.forClass(HCaptchaException.class);
        verify(captchaVerifier, times(1)).onFailure(failure.capture());
        assertEquals(HCaptchaError.INTERNAL_ERROR, failure.getValue().getHCaptchaError());
    }

    @Test
    public void newWindowReportsExactlyOneInternalErrorWhenBothLaunchesFail() {
        final Activity activity = mock(Activity.class);
        doThrow(new ActivityNotFoundException("no messaging app")).when(activity).startActivity(any());
        createHelper(activity);

        assertFalse(chromeClient.onCreateWindow(anchorHitOn(LIVE_LINK), false, true, null));

        startedActivities(activity, 2);
        verify(captchaVerifier, times(1)).onFailure(any());
    }

    @Test
    public void linkWithoutRecipientSkipsComposeIntentAndUsesRawLink() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView, navigationTo("sms:?body=code")));

        final Intent intent = startedActivities(activity, 1).get(0);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertNotEquals(Intent.ACTION_SENDTO, intent.getAction());
    }

    @Test
    public void literalFragmentDoesNotLeakIntoTheRecipient() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        assertTrue(webViewClient.shouldOverrideUrlLoading(webView,
                navigationTo("sms:" + NUMBER + "#123?body=code")));

        assertEquals(Uri.parse("smsto:" + NUMBER), startedActivities(activity, 1).get(0).getData());
    }

    @Test
    public void nonSmsNavigationIsLeftToTheWebView() {
        final Activity activity = mock(Activity.class);
        createHelper(activity);

        assertFalse(webViewClient.shouldOverrideUrlLoading(webView,
                navigationTo("https://newassets.hcaptcha.com/captcha/v1/index.html")));

        verify(activity, never()).startActivity(any());
        verify(captchaVerifier, never()).onFailure(any());
    }
}
