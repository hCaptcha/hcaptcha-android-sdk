package com.hcaptcha.sdk;

import android.content.Context;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HCaptchaDebugInfoTest {
    @Rule
    public BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Test
    public void benchmarkDebugInfo() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            new HCaptchaDebugInfo(context).debugInfo(
                    context.getPackageName(),
                    context.getPackageCodePath());
        }
    }

    @Test
    public void benchmarkDebugSys() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final BenchmarkState state = benchmarkRule.getState();
        while (state.keepRunning()) {
            new HCaptchaDebugInfo(context).roBuildProps();
        }
    }
}
