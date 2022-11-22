package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class HCaptchaTest {
    private static final long AWAIT_CALLBACK_MS = 5000;

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .tokenExpiration(1)
            .build();

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
            .htmlProvider(new HCaptchaTestHtml())
            .build();

    @Test
    public void testExpiredAfterSuccess() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            HCaptcha.getClient(activity, internalConfig)
                    .verifyWithHCaptcha(config)
                    .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                        @Override
                        public void onSuccess(HCaptchaTokenResponse response) {
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(HCaptchaException exception) {
                            assertEquals(HCaptchaError.TOKEN_TIMEOUT, exception.getHCaptchaError());
                            latch.countDown();
                        }
                    });
        });

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void webViewSessionTimeoutSuppressed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            HCaptcha.getClient(activity, internalConfig)
                    .verifyWithHCaptcha(config)
                    .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                        @Override
                        public void onSuccess(HCaptchaTokenResponse response) {
                            response.markUsed();
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(HCaptchaException exception) {
                            fail("Session timeout should not be happened");
                        }
                    });
        });

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }
}
