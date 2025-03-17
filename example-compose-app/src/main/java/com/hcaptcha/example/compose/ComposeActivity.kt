package com.hcaptcha.example.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaEvent
import com.hcaptcha.sdk.HCaptchaResponse
import com.hcaptcha.sdk.HCaptchaSize

class ComposeActivity : ComponentActivity() {

    private enum class CaptchaState { Idle, Started, Loaded }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hideDialog by remember { mutableStateOf(false) }
            var captchaState by remember { mutableStateOf(CaptchaState.Idle) }
            var text by remember { mutableStateOf("") }

            val hCaptchaConfig = remember(hideDialog) {
                HCaptchaConfig.builder()
                    .siteKey("10000000-ffff-ffff-ffff-000000000001")
                    .size(if (hideDialog) HCaptchaSize.INVISIBLE else HCaptchaSize.NORMAL)
                    .hideDialog(hideDialog)
                    .diagnosticLog(true)
                    .build()
            }

            if (captchaState != CaptchaState.Idle) {
                HCaptchaCompose(hCaptchaConfig) { result ->
                    val message = when (result) {
                        is HCaptchaResponse.Success -> {
                            captchaState = CaptchaState.Idle
                            "Success: ${result.token}"
                        }
                        is HCaptchaResponse.Failure -> {
                            captchaState = CaptchaState.Idle
                            "Failure: ${result.error.message}"
                        }
                        is HCaptchaResponse.Event -> {
                            if (result.event == HCaptchaEvent.Opened) {
                                captchaState = CaptchaState.Loaded
                            }
                            "Event: ${result.event}"
                        }
                    }
                    text += "\n${message}"
                    println(message)
                }
            }

            CaptchaControlUI(
                hideDialog = hideDialog,
                onHideDialogChanged = { hideDialog = it },
                text = text,
                onVerifyClick = {
                    captchaState = CaptchaState.Started
                    text = ""
                },
                showProgress = captchaState == CaptchaState.Started
            )
        }
    }

    @Composable
    private fun CaptchaControlUI(
        hideDialog: Boolean,
        onHideDialogChanged: (Boolean) -> Unit,
        text: String,
        onVerifyClick: () -> Unit,
        showProgress: Boolean
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            TextField(
                value = text,
                onValueChange = {},
                placeholder = { Text("Verification result will be here...") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Gray)
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = hideDialog,
                    onCheckedChange = onHideDialogChanged
                )
                Text(text = "Hide Dialog (Passive Site Key)")
            }

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(text = "Verify with HCaptcha")
            }

            if (showProgress) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(64.dp),
                    )
                }
            }
        }
    }
}

