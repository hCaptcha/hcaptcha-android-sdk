package com.hcaptcha.sdk

import android.app.Activity
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.State

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal class HCaptchaComposeVerifier(
    private val config: HCaptchaConfig,
    private val onResult: (HCaptchaResponse) -> Unit,
    private val helperState: State<HCaptchaWebViewHelper?>
) : IHCaptchaVerifier {

    private var verifyParams: HCaptchaVerifyParams? = null

    override fun onLoaded() {
        onResult(HCaptchaResponse.Event(HCaptchaEvent.Loaded))
        if (config.hideDialog) {
            helperState.value?.let {
                helper -> {
                    if (verifyParams != null) {
                        helper.reset()
                        helper.setVerifyParams(verifyParams)
                    }
                    helper.execute()
                }
            } ?: run {
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
        helperState.value?.takeIf { it.shouldRetry(exception) }?.let {
            helper -> {
                if (verifyParams != null) {
                    helper.reset()
                    helper.setVerifyParams(verifyParams)
                }
                helper.execute()
            }
        } ?: onResult(HCaptchaResponse.Failure(exception.hCaptchaError))
    }

    override fun startVerification(activity: Activity, verifyParams: HCaptchaVerifyParams?) {
        this.verifyParams = verifyParams
        error("startVerification should never be reached")
    }

    override fun reset() {
        error("reset should never be reached")
    }
}