package com.hcaptcha.sdk;

import static org.junit.Assert.fail;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class HCaptchaBenchmarkTest {

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    public HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .loading(false)
            .htmlProvider(new TestHCaptchaHtml())
            .build();

    @Test
    public void benchmarkInvisibleSetup() {
        rule.getScenario().onActivity(activity -> {
            final BenchmarkState state = benchmarkRule.getState();
            while (state.keepRunning()) {
                HCaptcha.getClient(activity).setup(config);
            }
        });
    }

    @Test
    public void benchmarkInvisibleVerification() {
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            state.pauseTiming();
            final CountDownLatch latch = new CountDownLatch(1);
            try {
                rule.getScenario().onActivity(activity -> {
                    HCaptcha hCaptcha = HCaptcha.getClient(activity).setup(config);
                    state.resumeTiming();
                    hCaptcha.verifyWithHCaptcha()
                            .addOnSuccessListener(response -> latch.countDown())
                            .addOnFailureListener(exception -> latch.countDown());

                });
                latch.await();
            } catch (InterruptedException e) {
                fail("benchmarkInvisibleVerification failed");
            }
        }
    }

    @Test
    public void benchmarkInvisibleVerificationColdRun() {
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            state.pauseTiming();
            final CountDownLatch latch = new CountDownLatch(1);
            try {
                rule.getScenario().onActivity(activity -> {
                    state.resumeTiming();
                    HCaptcha.getClient(activity).verifyWithHCaptcha(config)
                            .addOnSuccessListener(response -> latch.countDown());

                });
                latch.await();
            } catch (InterruptedException e) {
                fail("benchmarkInvisibleVerificationColdRun failed");
            }
        }
    }
}
