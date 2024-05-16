package com.hcaptcha.sdk

import android.app.Activity
import android.app.Dialog
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
public fun HCaptchaCompose(config: HCaptchaConfig, onResult: (HCaptchaResponse) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    val context = LocalContext.current
    val verifier = object : IHCaptchaVerifier {
        override fun onLoaded() {
            onResult(HCaptchaResponse.Event(HCaptchaEvent.Loaded))
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
    val internalConfig = HCaptchaInternalConfig(com.hcaptcha.sdk.HCaptchaHtml())

    DisposableEffect(context) {
        val dialog = Dialog(context)
        dialog.setContentView(
            HCaptchaWebView(context).apply {
                HCaptchaWebViewHelper(handler, context, config, internalConfig, verifier, this)
            }
        )
        dialog.show()
        onDispose {
            dialog.dismiss()
        }
    }
}