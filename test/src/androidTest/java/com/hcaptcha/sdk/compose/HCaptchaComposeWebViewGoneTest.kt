package com.hcaptcha.sdk.compose

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaRenderMode
import com.hcaptcha.sdk.HCaptchaResponse
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

// Verifying again once the WebView's renderer is gone (#214)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class HCaptchaComposeWebViewGoneTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val timeout = TimeUnit.SECONDS.toMillis(10)
    private val config = HCaptchaConfig.builder()
        .siteKey(HCaptchaComposeTest.PASSIVE_SITE_KEY)
        .renderMode(HCaptchaRenderMode.HEADLESS)
        .build()
    private val results = CopyOnWriteArrayList<HCaptchaResponse>()

    @Test
    fun verifiesAgainAfterRecomposition() {
        val tick = mutableIntStateOf(0)
        composeTestRule.setContent {
            val current = tick.intValue
            // A new lambda each tick recomposes HCaptchaCompose with the same config
            HCaptchaCompose(config) { result -> if (current >= 0) results.add(result) }
        }

        crashAfterFirstToken()
        tick.intValue++

        waitForTokens(2)
    }

    @Test
    fun verifiesAgainInNewKey() {
        val attempt = mutableIntStateOf(0)
        composeTestRule.setContent {
            key(attempt.intValue) {
                HCaptchaCompose(config) { result -> results.add(result) }
            }
        }

        crashAfterFirstToken()
        attempt.intValue++

        waitForTokens(2)
    }

    private fun crashAfterFirstToken() {
        waitForTokens(1)
        composeTestRule.runOnUiThread {
            val webView = findWebView(composeTestRule.activity.window.decorView)!!
            webView.webViewClient.onRenderProcessGone(webView, CrashedRenderer())
        }
        composeTestRule.waitUntil(timeout) {
            results.any { it is HCaptchaResponse.Failure }
        }
        val failure = results.filterIsInstance<HCaptchaResponse.Failure>().single()
        assertEquals(HCaptchaError.ERROR, failure.error)
    }

    private fun waitForTokens(count: Int) {
        composeTestRule.waitUntil(timeout) {
            results.count { it is HCaptchaResponse.Success } >= count
        }
    }

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
        else -> null
    }

    private class CrashedRenderer : RenderProcessGoneDetail() {
        override fun didCrash() = true

        override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
    }
}
