package com.hcaptcha.sdk.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaResponse
import com.hcaptcha.sdk.HCaptchaSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HCaptchaComposeTest {
    companion object {
        const val SITE_KEY = "10000000-ffff-ffff-ffff-000000000001"
        const val TEST_TOKEN = "10000000-aaaa-bbbb-cccc-000000000001"
    }

    private val resultContentDescription = "HCaptchaResultString"
    private val timeout = TimeUnit.SECONDS.toMillis(4)

    @get:Rule
    val composeTestRule = createComposeRule()

    fun setContent(siteKey: String = SITE_KEY, passiveSiteKey: Boolean = false) {
        composeTestRule.setContent {
            var text by remember { mutableStateOf("<init>") }
            Column {
                Text(text = text, modifier = Modifier.semantics { contentDescription = resultContentDescription })

                HCaptchaCompose(HCaptchaConfig
                    .builder()
                    .siteKey(siteKey)
                    .diagnosticLog(true)
                    .size(HCaptchaSize.INVISIBLE)
                    .hideDialog(passiveSiteKey)
                    .build()) { result ->
                    when (result) {
                        is HCaptchaResponse.Success -> {
                            text = result.token
                        }
                        is HCaptchaResponse.Failure -> {
                            text = result.error.name
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
        setContent(SITE_KEY, true)

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextEquals(TEST_TOKEN)
    }
}