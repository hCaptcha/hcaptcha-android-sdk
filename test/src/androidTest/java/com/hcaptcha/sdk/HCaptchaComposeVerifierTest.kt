package com.hcaptcha.sdk

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hcaptcha.sdk.test.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HCaptchaComposeVerifierTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val config = HCaptchaConfig.builder()
        .siteKey("siteKey")
        .retryPredicate { _, error -> error.hCaptchaError == HCaptchaError.NETWORK_ERROR }
        .build()

    private fun createVerifier(onResult: (HCaptchaResponse) -> Unit): IHCaptchaVerifier {
        val context = composeTestRule.activity
        val handler = Handler(Looper.getMainLooper())
        val internalConfig = HCaptchaInternalConfig(com.hcaptcha.sdk.HCaptchaHtml())
        val helper = mutableStateOf<HCaptchaWebViewHelper?>(null)

        val clazz = Class.forName("com.hcaptcha.sdk.HCaptchaComposeVerifier")
        val constructor = clazz.constructors.first()
        constructor.isAccessible = true

        val result = constructor.newInstance(config, onResult, helper) as IHCaptchaVerifier
        helper.value = HCaptchaWebViewHelper(handler, context, config, internalConfig, result, HCaptchaWebView(context))
        return result
    }

    @Test
    @UiThreadTest
    fun retryHappens() {
        var onFailureCalled = false

        val subject = createVerifier { result ->
            when (result) {
                is HCaptchaResponse.Failure -> {
                    onFailureCalled = true
                }

                is HCaptchaResponse.Event -> {}
                is HCaptchaResponse.Success -> error("unreachable")
            }
        }

        subject.onFailure(HCaptchaException(HCaptchaError.NETWORK_ERROR))

        assert(!onFailureCalled)
    }

    @Test
    @UiThreadTest
    fun noRetryHappens() {
        var onFailureCalled = false

        val subject = createVerifier { result ->
            when (result) {
                is HCaptchaResponse.Failure -> {
                    onFailureCalled = true
                }

                is HCaptchaResponse.Event -> {}
                is HCaptchaResponse.Success -> error("unreachable")
            }
        }

        subject.onFailure(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED))

        assert(onFailureCalled)
    }
}