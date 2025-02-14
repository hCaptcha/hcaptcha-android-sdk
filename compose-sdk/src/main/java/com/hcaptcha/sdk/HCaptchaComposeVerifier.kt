package com.hcaptcha.sdk

import android.app.Activity
import androidx.compose.runtime.State

internal class HCaptchaComposeVerifier(
    private val config: HCaptchaConfig,
    private val onResult: (HCaptchaResponse) -> Unit,
    private val helperState: State<HCaptchaWebViewHelper?>
) : IHCaptchaVerifier {

    override fun onLoaded() {
        onResult(HCaptchaResponse.Event(HCaptchaEvent.Loaded))
        if (config.hideDialog) {
            helperState.value?.resetAndExecute() ?: run {
                HCaptchaLog.w("HCaptchaWebViewHelper wasn't created, report but to developer")
                onResult(HCaptchaResponse.Failure(HCaptchaError.INTERNAL_ERROR))
            }
        }
    }

    override fun onOpen() {
        onResult(HCaptchaResponse.Event(HCaptchaEvent.Opened))
    }

    override fun onSuccess(result: String) {
        onResult(HCaptchaResponse.Success(result))
    }

    override fun onFailure(exception: HCaptchaException) {
        onResult(HCaptchaResponse.Failure(exception.hCaptchaError))
    }

    override fun startVerification(activity: Activity) {
        error("startVerification should never be reached")
    }

    override fun reset() {
        error("reset should never be reached")
    }
}