package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaVerifyParamsFallbackTest {
    private final HCaptchaRenderMode mode;
    private final boolean loaded;
    private final HCaptchaSize size;
    private final String valueKind;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}, loaded={1}, size={2}, value={3}")
    public static List<Object[]> cases() {
        final List<Object[]> cases = new ArrayList<>();
        for (HCaptchaRenderMode mode : HCaptchaRenderMode.values()) {
            for (boolean loaded : new boolean[] {false, true}) {
                for (HCaptchaSize size : new HCaptchaSize[] {HCaptchaSize.INVISIBLE, HCaptchaSize.NORMAL}) {
                    if (mode == HCaptchaRenderMode.HEADLESS && size != HCaptchaSize.INVISIBLE) {
                        continue;
                    }
                    for (String kind : new String[] {"POJO", "enum", "UUID", "cycle", "NaN", "iterator"}) {
                        cases.add(new Object[] {mode, loaded, size, kind});
                    }
                }
            }
        }
        return cases;
    }

    public HCaptchaVerifyParamsFallbackTest(HCaptchaRenderMode mode, boolean loaded,
                                          HCaptchaSize size, String valueKind) {
        this.mode = mode;
        this.loaded = loaded;
        this.size = size;
        this.valueKind = valueKind;
    }

    @Test
    public void invalidJourneyIsOmittedWithoutFailingVerification() throws Exception {
        final FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        final FrameLayout container = new FrameLayout(activity);
        activity.setContentView(container);
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
                .renderMode(mode).size(size).retryPredicate((ignoredConfig, error) -> true).build();
        final HCaptcha client = HCaptcha.getClient(activity).setEmbeddedContainer(container).setup(config);
        final List<HCaptchaException> errors = new ArrayList<>();
        client.addOnFailureListener(errors::add);
        client.verifyWithHCaptcha();
        activity.getSupportFragmentManager().executePendingTransactions();
        final Field verifierField = HCaptcha.class.getDeclaredField("captchaVerifier");
        verifierField.setAccessible(true);
        final IHCaptchaVerifier verifier = (IHCaptchaVerifier) verifierField.get(client);
        final Field helperField = verifier.getClass().getDeclaredField("webViewHelper");
        helperField.setAccessible(true);
        final HCaptchaWebViewHelper helper = (HCaptchaWebViewHelper) helperField.get(verifier);
        if (loaded) {
            verifier.onLoaded();
            verifier.onOpen();
        }
        helper.getWebView().loadUrl("about:blank");

        final Object value;
        switch (valueKind) {
            case "enum":
                value = HCaptchaTheme.LIGHT;
                break;
            case "UUID":
                value = Collections.singletonMap("id", new UUID(0, 1));
                break;
            case "cycle":
                final JSONArray cycle = new JSONArray();
                cycle.put(cycle);
                value = cycle;
                break;
            case "NaN":
                value = new JSONArray().put((Object) Double.NaN);
                break;
            case "iterator":
                value = (Iterable<Object>) () -> {
                    throw new IllegalStateException();
                };
                break;
            default:
                value = new Object();
                break;
        }
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().phonePrefix("44")
                .phoneNumber("+44123456789").rqdata("request-data").userJourney(value).build();
        client.verifyWithHCaptcha(params);
        if (!loaded) {
            verifier.onLoaded();
        }

        assertTrue(errors.isEmpty());
        assertEquals("request-data", params.getRqdata());
        assertSame(value, params.getUserJourney());
        if (mode == HCaptchaRenderMode.EMBEDDED) {
            assertNotNull(helper.getWebView().getParent());
        }
        if (mode == HCaptchaRenderMode.HEADLESS || (!loaded && size == HCaptchaSize.INVISIBLE)) {
            assertEquals("javascript:execute();", shadowOf(helper.getWebView()).getLastLoadedUrl());
        } else {
            assertEquals("javascript:setData({\"mfa_phoneprefix\":\"44\",\"mfa_phone\":\"+44123456789\","
                            + "\"rqdata\":\"request-data\"});",
                    shadowOf(helper.getWebView()).getLastLoadedUrl());
        }
        assertTrue(helper.shouldRetry(new HCaptchaException(HCaptchaError.VERIFY_PARAMS_ERROR)));
        assertTrue(ShadowLog.getLogsForTag("hcaptcha").stream()
                .anyMatch(item -> "Ignoring invalid user journey".equals(item.msg)));
        client.destroy();
        activity.finish();
    }
}
