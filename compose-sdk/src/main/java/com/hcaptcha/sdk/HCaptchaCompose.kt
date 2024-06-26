package com.hcaptcha.sdk

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
public fun HCaptchaCompose(config: HCaptchaConfig, onResult: (HCaptchaResponse) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
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

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                HCaptchaWebView(context).apply {
                    HCaptchaWebViewHelper(
                        handler,
                        context,
                        config,
                        internalConfig,
                        verifier,
                        this
                    )
                }
            }
        )
    }
}