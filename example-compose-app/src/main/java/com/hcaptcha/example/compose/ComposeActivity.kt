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
import androidx.compose.material3.MaterialTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hCaptchaStarted by remember { mutableStateOf(false) }
            var hCaptchaLoaded by remember { mutableStateOf(false) }
            var hideDialog by remember { mutableStateOf(false) }
            var text by remember { mutableStateOf("") }

            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                TextField(
                    value = text,
                    placeholder = { Text("Verification result will be here...") },
                    onValueChange = { newText -> text = newText },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Gray)
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hideDialog,
                        onCheckedChange = { isChecked ->
                            hideDialog = isChecked
                        }
                    )

                    Text(
                        text = "Hide Dialog (Passive Site Key)",
                    )
                }

                Button(
                    onClick = {
                        hCaptchaStarted = !hCaptchaStarted
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text(text = "Verify with HCaptcha")
                }

                if (hCaptchaStarted && !hCaptchaLoaded) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(64.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }

                if (hCaptchaStarted) {
                    HCaptchaCompose(HCaptchaConfig
                        .builder()
                        .siteKey("10000000-ffff-ffff-ffff-000000000001")
                        .size(if (hideDialog) HCaptchaSize.INVISIBLE else HCaptchaSize.NORMAL)
                        .hideDialog(hideDialog)
                        .diagnosticLog(true)
                        .build()) { result ->
                        when (result) {
                            is HCaptchaResponse.Success -> {
                                text = "Success: ${result.token}"
                                hCaptchaStarted = false
                                hCaptchaLoaded = false
                                println(text)
                            }
                            is HCaptchaResponse.Failure -> {
                                hCaptchaStarted = false
                                hCaptchaLoaded = false
                                text = "Failure: ${result.error.message}"
                                println(text)
                            }
                            is HCaptchaResponse.Event -> {
                                if (result.event == HCaptchaEvent.Opened) {
                                    hCaptchaLoaded = true
                                }
                                println("Event: ${result.event}")
                            }
                        }
                    }
                }
            }
        }
    }
}

