package com.hcaptcha.sdk

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
public fun HCaptchaCompose(config: HCaptchaConfig, onResult: (HCaptchaResponse) -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    var helper: HCaptchaWebViewHelper? = null
    val verifier = object : IHCaptchaVerifier {
        override fun onLoaded() {
            onResult(HCaptchaResponse.Event(HCaptchaEvent.Loaded))
            if (config.hideDialog) {
                helper?.let {
                    it.resetAndExecute()
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
    HCaptchaLog.sDiagnosticsLogEnabled = config.diagnosticLog

    HCaptchaLog.d("HCaptchaCompose($config)")

    if (config.hideDialog) {
        AndroidView(
            modifier = Modifier.size(0.dp),
            factory = { context ->
                HCaptchaWebView(context).apply {
                    helper = HCaptchaWebViewHelper(
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
    } else {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    HCaptchaWebView(context).apply {
                        helper = HCaptchaWebViewHelper(
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
}