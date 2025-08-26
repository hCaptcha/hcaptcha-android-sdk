package com.hcaptcha.sdk.tasks;

import com.hcaptcha.sdk.HCaptchaException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test class for Task concurrent modification scenarios.
 * Tests that CopyOnWriteArrayList prevents ConcurrentModificationException
 * when listeners modify the list during callbacks.
 */
public class TaskTest {

    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Test implementation of Task to test the concurrent modification fix
     */
    private static class TestTask extends Task<String> {
        public void triggerSuccess(String result) {
            setResult(result);
        }

        public void triggerFailure(HCaptchaException exception) {
            setException(exception);
        }

        public void triggerOpen() {
            captchaOpened();
        }
    }

    @Test
    public void testConcurrentModificationScenarios() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final TestTask task = new TestTask();

        final OnSuccessListener<String> selfRemovingListener = new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String result) {
                task.removeOnSuccessListener(this);
                latch.countDown();
            }
        };

        final OnSuccessListener<String> otherRemovingListener = new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String result) {
                task.removeOnSuccessListener(selfRemovingListener);
                latch.countDown();
            }
        };

        task.addOnSuccessListener(selfRemovingListener)
                .addOnSuccessListener(otherRemovingListener);

        task.triggerSuccess("test result");

        Assert.assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }
}
