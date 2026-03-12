package com.hcaptcha.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider

@Composable
public fun HCaptchaCompose(config: HCaptchaConfig, onResult: (HCaptchaResponse) -> Unit) {
    HCaptchaLog.sDiagnosticsLogEnabled = config.diagnosticLog
    val headlessMode = config.isHeadlessMode()
    val suppressDim = config.loading == false

    val context = LocalContext.current
    val handler = Handler(Looper.getMainLooper())
    val internalConfig = HCaptchaInternalConfig(com.hcaptcha.sdk.HCaptchaHtml())
    val onResultState = rememberUpdatedState(onResult)

    val helper = remember { mutableStateOf<HCaptchaWebViewHelper?>(null) }
    var verificationFinished by remember { mutableStateOf(false) }
    var readyForInteraction by remember { mutableStateOf(false) }
    val verifier = remember {
        HCaptchaComposeVerifier(config, { result ->
            if (result is HCaptchaResponse.Success || result is HCaptchaResponse.Failure) {
                verificationFinished = true
            }
            onResultState.value(result)
        }, helper, onReadyForInteraction = {
            readyForInteraction = true
        })
    }
    val preloadedWebView = remember {
        HCaptchaWebView(context).apply {
            helper.value = HCaptchaWebViewHelper(
                handler, context, config, internalConfig, verifier, this
            )
        }
    }
    var dismissed by remember { mutableStateOf(false) }

    val onDismissRequest: () -> Unit = {
        dismissed = true
        verifier.onFailure(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED))
        helper.value?.destroy()
    }

    val webViewFactory: (Context) -> View = {
        preloadedWebView.apply {
            (parent as? ViewGroup)?.removeView(this)
        }
    }

    HCaptchaLog.d("HCaptchaCompose($config)")

    DisposableEffect(Unit) {
        onDispose {
            if (!dismissed && !verificationFinished && !headlessMode) {
                verifier.onFailure(HCaptchaException(HCaptchaError.CHALLENGE_CLOSED))
            }
            if (!dismissed) {
                helper.value?.destroy()
            }
        }
    }

    if (headlessMode) {
        AndroidView(
            modifier = Modifier.size(0.dp),
            factory = webViewFactory
        )
    } else if (!dismissed) {
        if (config.renderMode == HCaptchaRenderMode.DIALOG) {
            Dialog(
                onDismissRequest = onDismissRequest
            ) {
                DialogDimEffect(suppressDim = suppressDim, readyForInteraction = readyForInteraction)

                Column(
                    modifier = Modifier
                        .testTag("dialogRoot")
                        .fillMaxSize()
                        .clickable(
                            interactionSource = MutableInteractionSource(),
                            indication = null,
                            onClick = onDismissRequest
                        ),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AndroidView(factory = webViewFactory)
                }
            }
        } else {
            AndroidView(factory = webViewFactory)
        }
    }
}

private const val DEFAULT_DIM_AMOUNT = 0.6f

@Composable
private fun DialogDimEffect(suppressDim: Boolean, readyForInteraction: Boolean) {
    if (!suppressDim) return

    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return

    SideEffect {
        if (readyForInteraction) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(DEFAULT_DIM_AMOUNT)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
        }
    }
}
