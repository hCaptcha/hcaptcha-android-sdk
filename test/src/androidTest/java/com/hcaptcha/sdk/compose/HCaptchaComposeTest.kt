package com.hcaptcha.sdk.compose

import android.view.View
import android.view.WindowManager
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.test.espresso.Espresso.onView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaDialogWindowSetup
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaRenderMode
import com.hcaptcha.sdk.HCaptchaResponse
import com.hcaptcha.sdk.HCaptchaSize
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
                           size: State<HCaptchaSize> = mutableStateOf(HCaptchaSize.INVISIBLE),
                           loading: Boolean = true,
                           endpoint: String? = null,
                           renderMode: HCaptchaRenderMode = HCaptchaRenderMode.DIALOG) {

        composeTestRule.setContent {
            var text by remember { mutableStateOf("<init>") }
            Column {
                Text(text = text, modifier = Modifier.semantics { contentDescription = resultContentDescription })

                val configBuilder = HCaptchaConfig
                    .builder()
                    .siteKey(siteKey)
                    .diagnosticLog(true)
                    .size(size.value)
                    .loading(loading)
                    .renderMode(if (passiveSiteKey) HCaptchaRenderMode.HEADLESS else renderMode)

                endpoint?.let { configBuilder.endpoint(it) }

                HCaptchaCompose(
                    configBuilder.build()
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

    private fun getDialogWindowParams(): WindowManager.LayoutParams? {
        var dialogWindowParams: WindowManager.LayoutParams? = null

        return try {
            onView(isRoot())
                .inRoot(isDialog())
                .perform(object : ViewAction {
                    override fun getConstraints(): Matcher<View> = isRoot()
                    override fun getDescription(): String = "extract dialog window params"
                    override fun perform(uiController: UiController, view: View) {
                        var root = view
                        while (root.parent is View) {
                            root = root.parent as View
                        }
                        dialogWindowParams = root.layoutParams as? WindowManager.LayoutParams
                    }
                })
            dialogWindowParams
        } catch (_: Throwable) {
            null
        }
    }

    private fun setDialogWindowSetupContent(dimBehind: MutableState<Boolean>) {
        composeTestRule.setContent {
            Dialog(onDismissRequest = {}) {
                HCaptchaDialogWindowSetup(dimBehind = dimBehind.value)
                Box(
                    modifier = Modifier
                        .testTag("dialogRoot")
                        .fillMaxSize()
                )
            }
        }
    }

    private fun waitForDialogDimmed(dimmed: Boolean, timeoutMs: Long = timeout) {
        composeTestRule.waitUntil(timeoutMs) {
            getDialogWindowParams()?.let { params ->
                val hasDimFlag = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
                if (dimmed) {
                    hasDimFlag && params.dimAmount > 0f
                } else {
                    !hasDimFlag && params.dimAmount == 0f
                }
            } == true
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

    @Test
    fun renderCustomHost() {
        setContent(renderMode = HCaptchaRenderMode.EMBEDDED)

        runBlocking { delay(timeout) }

        composeTestRule.onNodeWithTag("dialogRoot").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(resultContentDescription)
            .assertTextEquals(TEST_TOKEN)
    }

    @Test
    fun loadingFalseSuppressesDimWhileCaptchaIsStillHidden() {
        setContent(
            siteKey = CHALLENGE_SITE_KEY,
            loading = false,
            endpoint = "https://invalid.invalid/1/api.js"
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dialogRoot").assertIsDisplayed()
        waitForDialogDimmed(dimmed = false, timeoutMs = TimeUnit.SECONDS.toMillis(2))

        val params = getDialogWindowParams()!!
        assertFalse(params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
        assertEquals(0f, params.dimAmount, 0f)
    }

    @Test
    fun loadingFalseRestoresDimWhenCheckboxBecomesVisible() {
        setContent(
            siteKey = CHALLENGE_SITE_KEY,
            size = mutableStateOf(HCaptchaSize.COMPACT),
            loading = false
        )

        composeTestRule.onNodeWithTag("dialogRoot").assertIsDisplayed()
        waitForDialogDimmed(dimmed = true, timeoutMs = TimeUnit.SECONDS.toMillis(8))

        val params = getDialogWindowParams()!!
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
        assertTrue(params.dimAmount > 0f)
    }

    @Test
    fun loadingFalseRestoresDimWhenInvisibleChallengeOpens() {
        val dimBehind = mutableStateOf(false)
        setDialogWindowSetupContent(dimBehind)

        composeTestRule.onNodeWithTag("dialogRoot").assertIsDisplayed()
        waitForDialogDimmed(dimmed = false, timeoutMs = TimeUnit.SECONDS.toMillis(2))

        composeTestRule.runOnIdle {
            dimBehind.value = true
        }

        waitForDialogDimmed(dimmed = true, timeoutMs = TimeUnit.SECONDS.toMillis(2))

        val params = getDialogWindowParams()!!
        assertTrue(params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0)
        assertTrue(params.dimAmount > 0f)
    }
}
