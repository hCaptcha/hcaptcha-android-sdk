package com.hcaptcha.example.compose

import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaEvent
import com.hcaptcha.sdk.HCaptchaRenderMode
import com.hcaptcha.sdk.HCaptchaResponse
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.journeylitics.AnalyticsScreen
import com.hcaptcha.sdk.HCaptchaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComposeActivity : ComponentActivity() {
    private enum class ResultState { Idle, Verifying, Success, Error, Reset, Destroyed }
    private enum class TopTab { Configuration, CustomHost, Result, AuditLog }
    private enum class SitekeyOption { Visual, Passive, Custom }

    companion object {
        private const val SITEKEY_VISUAL = "00000000-0000-0000-0000-000000000000"
        private const val SITEKEY_PASSIVE = "10000000-ffff-ffff-ffff-000000000001"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContent {
            AnalyticsScreen("ComposeActivity") {
                val compactTypography = Typography(
                    bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                    bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    titleMedium = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    labelMedium = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp)
                )
                MaterialTheme(typography = compactTypography) {
                    val context = LocalContext.current
                    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

                val hcBgApp = Color(0xFF211238)
                val hcSurface = Color(0xFFFFFFFF)
                val hcSurfaceAlt = Color(0xFFF1E8FF)
                val hcTextPrimary = Color(0xFF2B1450)
                val hcTextSecondary = Color(0xFF5D3E8F)
                val hcResultOk = Color(0xFFF3ECFF)
                val hcResultError = Color(0xFFE9DDFF)
                val hcAuditBg = Color(0xFFEFE4FF)
                val hcBottomBar = Color(0xFF4B2B8A)
                val hcBottomText = Color(0xFFFFFFFF)

                var selectedTab by remember { mutableStateOf(TopTab.Configuration) }
                var selectedMode by remember { mutableStateOf(HCaptchaRenderMode.DIALOG) }
                var selectedSize by remember { mutableStateOf(HCaptchaSize.NORMAL) }
                var selectedSitekeyOption by remember { mutableStateOf(SitekeyOption.Passive) }
                var customSitekey by remember { mutableStateOf("") }

                var loadingEnabled by remember { mutableStateOf(true) }
                var webDebugEnabled by remember { mutableStateOf(false) }
                var disableHwAccel by remember { mutableStateOf(true) }
                var darkTheme by remember { mutableStateOf(false) }
                var userJourney by remember { mutableStateOf(false) }

                var resultState by remember { mutableStateOf(ResultState.Idle) }
                var resultMessage by remember { mutableStateOf("-") }
                var lastToken by remember { mutableStateOf<String?>(null) }
                val auditLog = remember { mutableStateListOf<String>() }

                var captchaVisible by remember { mutableStateOf(false) }
                var captchaRenderKey by remember { mutableIntStateOf(0) }
                var customWaitingForOpen by remember { mutableStateOf(false) }
                var customStatusMessage by remember { mutableStateOf<String?>(null) }
                var customStatusIsError by remember { mutableStateOf(false) }

                fun addAudit(event: String) {
                    auditLog.add(0, "${formatter.format(Date())}  $event")
                    if (auditLog.size > 100) {
                        auditLog.removeAt(auditLog.lastIndex)
                    }
                }

                fun clearResult() {
                    resultState = ResultState.Idle
                    resultMessage = "-"
                    lastToken = null
                }

                fun modeLabel(mode: HCaptchaRenderMode): String = when (mode) {
                    HCaptchaRenderMode.DIALOG -> "Dialog"
                    HCaptchaRenderMode.EMBEDDED -> "Embedded"
                    HCaptchaRenderMode.HEADLESS -> "Headless"
                }

                fun resetCaptcha() {
                    captchaVisible = false
                    captchaRenderKey += 1
                    customWaitingForOpen = false
                    customStatusMessage = null
                    customStatusIsError = false
                }

                val loadingForConfig = if (selectedMode == HCaptchaRenderMode.EMBEDDED) false else loadingEnabled
                val sizeForConfig = if (selectedMode == HCaptchaRenderMode.HEADLESS) HCaptchaSize.INVISIBLE else selectedSize
                val activeSitekey = when (selectedSitekeyOption) {
                    SitekeyOption.Visual -> SITEKEY_VISUAL
                    SitekeyOption.Passive -> SITEKEY_PASSIVE
                    SitekeyOption.Custom -> customSitekey.ifBlank { SITEKEY_VISUAL }
                }

                val config = remember(
                    selectedMode,
                    sizeForConfig,
                    loadingForConfig,
                    disableHwAccel,
                    darkTheme,
                    activeSitekey,
                    userJourney,
                    captchaRenderKey
                ) {
                    HCaptchaConfig.builder()
                        .siteKey(activeSitekey)
                        .size(sizeForConfig)
                        .renderMode(selectedMode)
                        .loading(loadingForConfig)
                        .disableHardwareAcceleration(disableHwAccel)
                        .theme(if (darkTheme) HCaptchaTheme.DARK else HCaptchaTheme.LIGHT)
                        .userJourney(userJourney)
                        .tokenExpiration(120)
                        .diagnosticLog(true)
                        .build()
                }

                LaunchedEffect(webDebugEnabled) {
                    WebView.setWebContentsDebuggingEnabled(webDebugEnabled)
                    addAudit("Web debug ${if (webDebugEnabled) "enabled" else "disabled"}")
                }

                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = hcBottomText,
                    unselectedIconColor = hcSurfaceAlt,
                    selectedTextColor = hcBottomText,
                    unselectedTextColor = hcSurfaceAlt,
                    indicatorColor = Color.Transparent
                )

                Scaffold(
                    containerColor = hcBgApp,
                    bottomBar = {
                        NavigationBar(containerColor = hcBottomBar) {
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    resetCaptcha()
                                    clearResult()
                                    resultState = ResultState.Reset
                                    resultMessage = "reset called"
                                    addAudit("Reset")
                                },
                                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                label = { Text("Reset") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    resultState = ResultState.Verifying
                                    resultMessage = "verification started"
                                    lastToken = null
                                    captchaVisible = true
                                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                                        selectedTab = TopTab.CustomHost
                                        customWaitingForOpen = true
                                        customStatusMessage = null
                                        customStatusIsError = false
                                    }
                                    addAudit("Verify")
                                },
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                label = { Text("Verify") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    resetCaptcha()
                                    resultState = ResultState.Destroyed
                                    resultMessage = "destroy called"
                                    lastToken = null
                                    addAudit("Destroy")
                                },
                                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                label = { Text("Destroy") },
                                colors = navItemColors
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    addAudit("Hit test")
                                    Toast.makeText(context, "Hit Test!", Toast.LENGTH_SHORT).show()
                                },
                                icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                                label = { Text("Hit") },
                                colors = navItemColors
                            )
                        }
                    }
                ) { padding ->
                    val tabs = if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                        listOf(TopTab.Configuration, TopTab.CustomHost, TopTab.Result, TopTab.AuditLog)
                    } else {
                        listOf(TopTab.Configuration, TopTab.Result, TopTab.AuditLog)
                    }
                    if (selectedTab == TopTab.CustomHost && selectedMode != HCaptchaRenderMode.EMBEDDED) {
                        selectedTab = TopTab.Configuration
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        TabRow(
                            selectedTabIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
                            containerColor = hcSurface,
                            contentColor = hcTextPrimary
                        ) {
                            tabs.forEach { tab ->
                                Tab(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    text = {
                                        Text(
                                            when (tab) {
                                                TopTab.Configuration -> "Configuration"
                                                TopTab.CustomHost -> "Render Host"
                                                TopTab.Result -> "Result"
                                                TopTab.AuditLog -> "Audit Log"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        when (selectedTab) {
                            TopTab.Configuration -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(colors = CardDefaults.cardColors(containerColor = hcSurface)) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Configuration", style = MaterialTheme.typography.titleMedium, color = hcTextPrimary)
                                            Text("Sitekey", color = hcTextPrimary)
                                            InlineRadioRow(
                                                labels = listOf("Passive", "Challenge", "Custom"),
                                                selectedIndex = when (selectedSitekeyOption) {
                                                    SitekeyOption.Passive -> 0
                                                    SitekeyOption.Visual -> 1
                                                    SitekeyOption.Custom -> 2
                                                },
                                                onSelected = {
                                                    selectedSitekeyOption = when (it) {
                                                        1 -> SitekeyOption.Visual
                                                        2 -> SitekeyOption.Custom
                                                        else -> SitekeyOption.Passive
                                                    }
                                                    val label = when (selectedSitekeyOption) {
                                                        SitekeyOption.Visual -> "Challenge"
                                                        SitekeyOption.Passive -> "Passive"
                                                        SitekeyOption.Custom -> "Custom"
                                                    }
                                                    addAudit("Sitekey switched to $label")
                                                }
                                            )
                                            if (selectedSitekeyOption == SitekeyOption.Custom) {
                                                OutlinedTextField(
                                                    value = customSitekey,
                                                    onValueChange = { customSitekey = it },
                                                    label = { Text("Custom sitekey") },
                                                    placeholder = { Text("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                                )
                                            }
                                            Text("Render mode", color = hcTextPrimary)
                                            InlineRadioRow(
                                                labels = listOf("Dialog", "Headless", "Embedded"),
                                                selectedIndex = when (selectedMode) {
                                                    HCaptchaRenderMode.DIALOG -> 0
                                                    HCaptchaRenderMode.HEADLESS -> 1
                                                    HCaptchaRenderMode.EMBEDDED -> 2
                                                },
                                                onSelected = {
                                                    selectedMode = when (it) {
                                                        1 -> HCaptchaRenderMode.HEADLESS
                                                        2 -> HCaptchaRenderMode.EMBEDDED
                                                        else -> HCaptchaRenderMode.DIALOG
                                                    }
                                                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                                                        loadingEnabled = false
                                                    }
                                                    addAudit("Mode switched to ${modeLabel(selectedMode)}")
                                                }
                                            )
                                            Text("Checkbox size", color = hcTextPrimary)
                                            InlineRadioRow(
                                                labels = listOf("Normal", "Invisible", "Compact"),
                                                selectedIndex = when (selectedSize) {
                                                    HCaptchaSize.NORMAL -> 0
                                                    HCaptchaSize.INVISIBLE -> 1
                                                    HCaptchaSize.COMPACT -> 2
                                                },
                                                onSelected = {
                                                    selectedSize = when (it) {
                                                        1 -> HCaptchaSize.INVISIBLE
                                                        2 -> HCaptchaSize.COMPACT
                                                        else -> HCaptchaSize.NORMAL
                                                    }
                                                }
                                            )

                                            LabeledCheckbox("Loading", loadingEnabled, enabled = selectedMode != HCaptchaRenderMode.EMBEDDED) {
                                                loadingEnabled = it
                                            }
                                            LabeledCheckbox("Web Debug", webDebugEnabled) { webDebugEnabled = it }
                                            LabeledCheckbox("Disable HW Accel", disableHwAccel) { disableHwAccel = it }
                                            LabeledCheckbox("Dark Theme", darkTheme) { darkTheme = it }
                                            LabeledCheckbox("User Journey", userJourney) { userJourney = it }
                                        }
                                    }
                                }
                            }

                            TopTab.CustomHost -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(colors = CardDefaults.cardColors(containerColor = hcSurface)) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Embedded render host showcase", style = MaterialTheme.typography.titleMedium, color = hcTextPrimary)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                                    .background(hcSurfaceAlt)
                                            ) {
                                                if (captchaVisible && selectedMode == HCaptchaRenderMode.EMBEDDED) {
                                                    HCaptchaCompose(config = config) { response ->
                                                        when (response) {
                                                            is HCaptchaResponse.Success -> {
                                                                captchaVisible = false
                                                                customWaitingForOpen = false
                                                                customStatusMessage = "Success. See Result tab for details."
                                                                customStatusIsError = false
                                                                resultState = ResultState.Success
                                                                lastToken = response.token
                                                                resultMessage = response.token
                                                                addAudit("Verification success")
                                                            }

                                                            is HCaptchaResponse.Failure -> {
                                                                captchaVisible = false
                                                                customWaitingForOpen = false
                                                                customStatusMessage = "Error. See Result tab for details."
                                                                customStatusIsError = true
                                                                resultState = ResultState.Error
                                                                lastToken = null
                                                                resultMessage = response.error.message
                                                                addAudit("Verification failed: ${response.error.message}")
                                                                if (response.error == HCaptchaError.CHALLENGE_CLOSED) {
                                                                    addAudit("Challenge closed by user")
                                                                }
                                                            }

                                                            is HCaptchaResponse.Event -> {
                                                                if (response.event == HCaptchaEvent.Loaded || response.event == HCaptchaEvent.Opened) {
                                                                    customWaitingForOpen = false
                                                                }
                                                                if (response.event == HCaptchaEvent.Opened) {
                                                                    addAudit("Challenge opened")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                if (captchaVisible && selectedMode == HCaptchaRenderMode.EMBEDDED && customWaitingForOpen) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .wrapContentHeight()
                                                            .align(Alignment.Center),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        CircularProgressIndicator(color = hcTextSecondary)
                                                        Text("Loading captcha...", color = hcTextSecondary)
                                                    }
                                                }
                                            }
                                            customStatusMessage?.let { message ->
                                                Text(
                                                    text = message,
                                                    color = if (customStatusIsError) Color(0xFF8E1B1B) else Color(0xFF1B5E20)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            TopTab.Result -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (resultState == ResultState.Error) hcResultError else hcResultOk
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Result", style = MaterialTheme.typography.titleMedium, color = hcTextPrimary)
                                            Text("Status: ${resultState.name}", color = hcTextPrimary)
                                            Text(
                                                text = resultMessage,
                                                color = hcTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        lastToken?.let {
                                                            val clip = android.content.ClipData.newPlainText("hCaptcha Token", it)
                                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                            clipboard.setPrimaryClip(clip)
                                                            Toast.makeText(context, "Token copied", Toast.LENGTH_SHORT).show()
                                                            addAudit("Token copied to clipboard")
                                                        }
                                                    },
                                                    enabled = !lastToken.isNullOrEmpty()
                                                ) { Text("Copy token") }
                                                Button(
                                                    onClick = {
                                                        addAudit("Token marked used")
                                                        Toast.makeText(context, "Token marked used", Toast.LENGTH_SHORT).show()
                                                    },
                                                    enabled = !lastToken.isNullOrEmpty(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = hcTextSecondary)
                                                ) { Text("Mark used") }
                                            }
                                        }
                                    }
                                }
                            }

                            TopTab.AuditLog -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Card(colors = CardDefaults.cardColors(containerColor = hcAuditBg)) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Audit log", style = MaterialTheme.typography.titleMedium, color = hcTextPrimary)
                                                Button(onClick = {
                                                    auditLog.clear()
                                                    addAudit("Audit log cleared")
                                                }) {
                                                    Text("Clear")
                                                }
                                            }
                                            HorizontalDivider()
                                            if (auditLog.isEmpty()) {
                                                Text("-", color = hcTextSecondary)
                                            } else {
                                                auditLog.forEach { line ->
                                                    Text(line, color = hcTextSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (captchaVisible && selectedMode != HCaptchaRenderMode.EMBEDDED) {
                    HCaptchaCompose(config = config) { response ->
                        when (response) {
                            is HCaptchaResponse.Success -> {
                                captchaVisible = false
                                resultState = ResultState.Success
                                lastToken = response.token
                                resultMessage = response.token
                                addAudit("Verification success")
                            }

                            is HCaptchaResponse.Failure -> {
                                captchaVisible = false
                                resultState = ResultState.Error
                                lastToken = null
                                resultMessage = response.error.message
                                addAudit("Verification failed: ${response.error.message}")
                            }

                            is HCaptchaResponse.Event -> {
                                if (response.event == HCaptchaEvent.Opened) {
                                    addAudit("Challenge opened")
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }

    @Composable
    private fun InlineRadioRow(
        labels: List<String>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { onSelected(index) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null
                    )
                    Text(label, color = Color(0xFF2B1450), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    @Composable
    private fun LabeledCheckbox(
        label: String,
        checked: Boolean,
        enabled: Boolean = true,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color(0xFF2B1450))
            Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        }
    }
}
