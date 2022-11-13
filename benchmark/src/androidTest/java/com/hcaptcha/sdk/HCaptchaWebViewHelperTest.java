package com.hcaptcha.sdk;

import static org.junit.Assert.fail;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class HCaptchaWebViewHelperTest {
    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    public HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .loading(false)
            .build();

    final HCaptchaSettings settings = HCaptchaSettings.builder()
            .htmlProvider(new TestHCaptchaHtml())
            .build();

    @Test
    public void benchmarkWebViewLoad() {
        Handler handler = new Handler(Looper.getMainLooper());
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            state.pauseTiming();
            final CountDownLatch latch = new CountDownLatch(1);
            try {
                rule.getScenario().onActivity(activity -> {
                    WebView webView = new WebView(activity);
                    state.resumeTiming();
                    new HCaptchaWebViewHelper(
                            handler,
                            activity,
                            config,
                            settings,
                            new TestHCaptchaVerifier() {
                                @Override
                                public void onLoaded() {
                                    latch.countDown();
                                }
                            },
                            new TestHCaptchaStateListener(),
                            webView
                    );
                });
                latch.await();
            } catch (Exception e) {
                fail("benchmarkInvisibleVerificationColdRun failed");
            }
        }
    }
}
