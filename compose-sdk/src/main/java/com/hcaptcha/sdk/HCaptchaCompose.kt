package com.hcaptcha.sdk

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

@Composable
public fun HCaptchaCompose(config: HCaptchaConfig, onResult: (HCaptchaResponse) -> Unit) {
    HCaptchaLog.sDiagnosticsLogEnabled = config.diagnosticLog

    val context = LocalContext.current
    val handler = Handler(Looper.getMainLooper())
    val internalConfig = HCaptchaInternalConfig(com.hcaptcha.sdk.HCaptchaHtml())

    val helper = remember { mutableStateOf<HCaptchaWebViewHelper?>(null) }
    val verifier = remember { HCaptchaComposeVerifier(config, onResult, helper) }
    val preloadedWebView = remember {
        HCaptchaWebView(context).apply {
            helper.value = HCaptchaWebViewHelper(
                handler, context, config, internalConfig, verifier, this
            )
        }
    }
    var dismissed by remember { mutableStateOf(false) }

    HCaptchaLog.d("HCaptchaCompose($config)")

    if (config.hideDialog) {
        AndroidView(
            modifier = Modifier.size(0.dp),
            factory = { preloadedWebView }
        )
    } else if (!dismissed) {
        Dialog(
            onDismissRequest = { dismissed = true }
        ) {
            Column(
                modifier = Modifier
                    .testTag("dialogRoot")
                    .fillMaxSize()
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = {
                            dismissed = true
                            verifier.onFailure(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
                            helper.value?.destroy()
                        }
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AndroidView(
                    factory = { preloadedWebView }
                )
            }
        }
    }
}