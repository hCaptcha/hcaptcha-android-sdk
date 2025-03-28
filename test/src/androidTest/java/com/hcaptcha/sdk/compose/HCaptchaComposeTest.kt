package com.hcaptcha.sdk.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaResponse
import com.hcaptcha.sdk.HCaptchaSize
import androidx.test.espresso.Espresso.pressBack
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HCaptchaComposeTest {
    companion object {
        const val PASSIVE_SITE_KEY = "10000000-ffff-ffff-ffff-000000000001"
        const val CHALLENGE_SITE_KEY = "00000000-0000-0000-0000-000000000000"
        const val TEST_TOKEN = "10000000-aaaa-bbbb-cccc-000000000001"
    }

    private val resultContentDescription = "HCaptchaResultString"
    private val timeout = TimeUnit.SECONDS.toMillis(4)

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(siteKey: String = PASSIVE_SITE_KEY,
                           passiveSiteKey: Boolean = false,
                           size: State<HCaptchaSize> = mutableStateOf(HCaptchaSize.INVISIBLE)) {

        composeTestRule.setContent {
            var text by remember { mutableStateOf("<init>") }
            Column {
                Text(text = text, modifier = Modifier.semantics { contentDescription = resultContentDescription })

                HCaptchaCompose(
                    HCaptchaConfig
                        .builder()
                        .siteKey(siteKey)
                        .diagnosticLog(true)
                        .size(size.value)
                        .hideDialog(passiveSiteKey)
                        .build()
                ) { result ->
                    when (result) {
                        is HCaptchaResponse.Success -> {
                            text = result.token
                        }

                        is HCaptchaResponse.Failure -> {
                            if (result.error.name == text) {
                                text += "2"
                            } else {
                                text = result.error.name
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    @Test
    fun validToken() {
        setContent()

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
                .assertTextEquals(TEST_TOKEN)
    }

    @Test
    fun invalidToken() {
        setContent("")

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextContains(HCaptchaError.ERROR.name)
    }

    @Test
    fun passiveSiteKey() {
        setContent(PASSIVE_SITE_KEY,true)

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextEquals(TEST_TOKEN)
    }

    @Test
    fun visualChallengeCanceled() {
        setContent(CHALLENGE_SITE_KEY)

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithTag("dialogRoot").performTouchInput {
            val x = this.width / 2f
            val y = this.height * 0.9f

            click(Offset(x, y))
        }

        runBlocking { delay(timeout / 2) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextContains(HCaptchaError.CHALLENGE_CLOSED.name)
    }

    @Test
    fun checkboxCanceled() {
        setContent("00000000-0000-0000-0000-000000000000",
            false, mutableStateOf(HCaptchaSize.COMPACT))

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithTag("dialogRoot").performTouchInput {
            val x = this.width / 2f
            val y = this.height * 0.9f

            click(Offset(x, y))
        }

        runBlocking { delay(timeout / 2) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextContains(HCaptchaError.CHALLENGE_CLOSED.name)
    }

    @Test
    fun checkboxCanceledWithBack() {
        setContent(CHALLENGE_SITE_KEY, false, mutableStateOf(HCaptchaSize.COMPACT))

        runBlocking { delay(timeout) }

        pressBack()

        runBlocking { delay(timeout / 2) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextContains(HCaptchaError.CHALLENGE_CLOSED.name)
    }

    @Test
    fun preloadedWebViewReused() {
        val size = mutableStateOf(HCaptchaSize.COMPACT)
        setContent(CHALLENGE_SITE_KEY, false, size)
        runBlocking { delay(timeout) }
        size.value = HCaptchaSize.INVISIBLE
        runBlocking { delay(timeout) }
        size.value = HCaptchaSize.COMPACT
        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextContains("<init>")
    }
}