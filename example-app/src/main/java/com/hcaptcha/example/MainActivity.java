package com.hcaptcha.example;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hcaptcha.sdk.HCaptcha;
import com.hcaptcha.sdk.HCaptchaConfig;
import com.hcaptcha.sdk.HCaptchaError;
import com.hcaptcha.sdk.HCaptchaRenderMode;
import com.hcaptcha.sdk.HCaptchaSize;
import com.hcaptcha.sdk.HCaptchaTheme;
import com.hcaptcha.sdk.HCaptchaTokenResponse;
import com.hcaptcha.sdk.HCaptchaVerifyParams;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SITEKEY_VISUAL = "00000000-0000-0000-0000-000000000000";
    private static final String SITEKEY_PASSIVE = "10000000-ffff-ffff-ffff-000000000001";
    private static final int MAX_AUDIT_LOG_LINES = 100;
    private static final int TAB_CONFIGURATION = 0;
    private static final int TAB_CUSTOM = 1;
    private static final int TAB_RESULT = 2;
    private static final int TAB_AUDIT = 3;

    private RadioGroup modeGroup;
    private RadioGroup sizeGroup;
    private RadioGroup sitekeyGroup;
    private TextInputLayout sitekeyInputLayout;
    private TextInputEditText sitekeyInput;
    private CheckBox loading;
    private CheckBox disableHardwareAccel;
    private CheckBox themeDark;
    private CheckBox webViewDebug;
    private TextInputLayout phoneInputLayout;
    private TextInputEditText phonePrefixInput;
    private SwitchCompat phoneModeSwitch;
    private TextInputEditText rqdataInput;
    private boolean loadingPreference = true;

    private View resultCard;
    private TextView resultStatusTextView;
    private TextView resultMessageTextView;
    private Button copyTokenButton;
    private Button markUsedButton;

    private TextView embeddedHostStateTextView;
    private TextView auditLogTextView;
    private View tabConfigurationView;
    private View tabCustomView;
    private View tabResultView;
    private View tabAuditView;
    private View embeddedLoadingOverlay;
    private TabLayout topTabs;
    private int selectedTab = TAB_CONFIGURATION;
    private final List<Integer> visibleTopTabs = new ArrayList<>();

    private HCaptcha hCaptcha;
    private HCaptchaTokenResponse tokenResponse;
    private String lastToken;

    private HCaptchaRenderMode selectedMode = HCaptchaRenderMode.DIALOG;
    private FrameLayout embeddedChallengeContainer;

    private final List<String> auditEntries = new ArrayList<>();
    private final SimpleDateFormat auditTimeFormatter = new SimpleDateFormat("HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applySystemBarStyle();

        modeGroup = findViewById(R.id.challenge_mode_group);
        sizeGroup = findViewById(R.id.challenge_size_group);
        sitekeyGroup = findViewById(R.id.sitekey_group);
        sitekeyInputLayout = findViewById(R.id.sitekeyInputLayout);
        sitekeyInput = findViewById(R.id.sitekeyInput);
        loading = findViewById(R.id.loading);
        disableHardwareAccel = findViewById(R.id.hwAccel);
        themeDark = findViewById(R.id.themeDark);
        webViewDebug = findViewById(R.id.webViewDebug);
        phoneInputLayout = findViewById(R.id.phoneInputLayout);
        phonePrefixInput = findViewById(R.id.phonePrefix);
        phoneModeSwitch = findViewById(R.id.phoneModeSwitch);
        rqdataInput = findViewById(R.id.rqdataInput);

        resultCard = findViewById(R.id.resultCard);
        resultStatusTextView = findViewById(R.id.resultStatusTextView);
        resultMessageTextView = findViewById(R.id.resultMessageTextView);
        copyTokenButton = findViewById(R.id.copyTokenButton);
        markUsedButton = findViewById(R.id.markUsedButton);

        embeddedHostStateTextView = findViewById(R.id.embeddedHostStateTextView);
        embeddedChallengeContainer = findViewById(R.id.embedded_challenge_container);
        auditLogTextView = findViewById(R.id.auditLogTextView);
        tabConfigurationView = findViewById(R.id.tabConfiguration);
        tabCustomView = findViewById(R.id.tabEmbedded);
        tabResultView = findViewById(R.id.tabResult);
        tabAuditView = findViewById(R.id.tabAudit);
        embeddedLoadingOverlay = findViewById(R.id.embeddedLoadingOverlay);

        topTabs = findViewById(R.id.topTabs);
        final BottomNavigationView bottomActions = findViewById(R.id.bottomActions);
        loadingPreference = loading.isChecked();
        setupTopTabs();

        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            final HCaptchaRenderMode newMode = modeFromId(checkedId);
            if (newMode != selectedMode) {
                selectedMode = newMode;
                addAuditLog("Mode switched to " + modeToLabel(newMode));
                resetForModeChange();
                setupTopTabs();
            }
            updateLoadingControlForMode();
            updateCustomHostUiState();
        });

        sitekeyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            sitekeyInputLayout.setVisibility(checkedId == R.id.sitekey_custom ? View.VISIBLE : View.GONE);
            addAuditLog("Sitekey switched to " + sitekeyLabel(checkedId));
        });

        setPhoneModeUi(phoneModeSwitch.isChecked());
        phoneModeSwitch.setOnCheckedChangeListener((button, checked) -> {
            setPhoneModeUi(checked);
            addAuditLog("Phone mode: " + (checked ? "full number" : "prefix"));
        });

        webViewDebug.setOnCheckedChangeListener((checkBox, checked) -> {
            android.webkit.WebView.setWebContentsDebuggingEnabled(checked);
            addAuditLog("Web debug " + (checked ? "enabled" : "disabled"));
        });
        loading.setOnCheckedChangeListener((checkBox, checked) -> {
            if (selectedMode != HCaptchaRenderMode.EMBEDDED) {
                loadingPreference = checked;
            }
        });
        bottomActions.setOnItemSelectedListener(item -> {
            final int id = item.getItemId();
            if (id == R.id.action_reset) {
                onClickReset(bottomActions);
                return true;
            }
            if (id == R.id.action_setup) {
                onClickSetup(bottomActions);
                return true;
            }
            if (id == R.id.action_verify) {
                onClickVerify(bottomActions);
                return true;
            }
            if (id == R.id.action_destroy) {
                onClickDestroy(bottomActions);
                return true;
            }
            if (id == R.id.action_hit_test) {
                onHitTest(bottomActions);
                return true;
            }
            return false;
        });

        final View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            final Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), bars.top, view.getPaddingRight(), 0);
            return insets;
        });

        clearOutput();
        updateAuditLogView();
        updateLoadingControlForMode();
        updateCustomHostUiState();
        addAuditLog("Screen ready");
    }

    private void applySystemBarStyle() {
        final Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.hc_bottom_bar_bg));
        final WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
    }

    private void setupTopTabs() {
        visibleTopTabs.clear();
        topTabs.clearOnTabSelectedListeners();
        topTabs.removeAllTabs();
        addTopTab(TAB_CONFIGURATION, R.string.tab_configuration);
        if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
            addTopTab(TAB_CUSTOM, R.string.tab_embedded_host);
        }
        addTopTab(TAB_RESULT, R.string.tab_result);
        addTopTab(TAB_AUDIT, R.string.tab_audit_log);
        topTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
                final int position = tab.getPosition();
                if (position < 0 || position >= visibleTopTabs.size()) {
                    showTab(TAB_CONFIGURATION);
                    return;
                }
                showTab(visibleTopTabs.get(position));
            }

            @Override
            public void onTabUnselected(final TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(final TabLayout.Tab tab) {
            }
        });

        int selectedPosition = visibleTopTabs.indexOf(selectedTab);
        if (selectedPosition < 0) {
            selectedTab = TAB_CONFIGURATION;
            selectedPosition = 0;
        }
        final TabLayout.Tab tabToSelect = topTabs.getTabAt(selectedPosition);
        if (tabToSelect != null) {
            tabToSelect.select();
        } else {
            showTab(TAB_CONFIGURATION);
        }
    }

    private void addTopTab(final int tabKey, final int titleResId) {
        visibleTopTabs.add(tabKey);
        topTabs.addTab(topTabs.newTab().setText(titleResId));
    }

    private void showTab(final int tabKey) {
        selectedTab = tabKey;
        tabConfigurationView.setVisibility(tabKey == TAB_CONFIGURATION ? View.VISIBLE : View.GONE);
        tabCustomView.setVisibility(tabKey == TAB_CUSTOM ? View.VISIBLE : View.GONE);
        tabResultView.setVisibility(tabKey == TAB_RESULT ? View.VISIBLE : View.GONE);
        tabAuditView.setVisibility(tabKey == TAB_AUDIT ? View.VISIBLE : View.GONE);
    }

    private void selectTopTab(final int tabKey) {
        final int tabPosition = visibleTopTabs.indexOf(tabKey);
        if (tabPosition < 0) {
            showTab(tabKey);
            return;
        }
        final TabLayout.Tab tab = topTabs.getTabAt(tabPosition);
        if (tab != null) {
            tab.select();
            return;
        }
        showTab(tabKey);
    }

    @Override
    protected void onDestroy() {
        destroyCaptcha();
        super.onDestroy();
    }

    private HCaptchaRenderMode modeFromId(final int id) {
        if (id == R.id.mode_headless) {
            return HCaptchaRenderMode.HEADLESS;
        }
        if (id == R.id.mode_embedded) {
            return HCaptchaRenderMode.EMBEDDED;
        }
        return HCaptchaRenderMode.DIALOG;
    }

    private String modeToLabel(final HCaptchaRenderMode mode) {
        switch (mode) {
            case EMBEDDED:
                return "Embedded";
            case HEADLESS:
                return "Headless";
            case DIALOG:
            default:
                return "Dialog";
        }
    }

    private HCaptchaSize getSelectedSize() {
        final int checkedId = sizeGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.size_invisible) {
            return HCaptchaSize.INVISIBLE;
        }
        if (checkedId == R.id.size_compact) {
            return HCaptchaSize.COMPACT;
        }
        return HCaptchaSize.NORMAL;
    }

    private String getSelectedSitekey() {
        final int checkedId = sitekeyGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.sitekey_passive) {
            return SITEKEY_PASSIVE;
        }
        if (checkedId == R.id.sitekey_custom) {
            final String custom = sitekeyInput.getText() != null ? sitekeyInput.getText().toString().trim() : "";
            return custom.isEmpty() ? SITEKEY_VISUAL : custom;
        }
        return SITEKEY_VISUAL;
    }

    private String sitekeyLabel(final int checkedId) {
        if (checkedId == R.id.sitekey_passive) {
            return "Passive";
        }
        if (checkedId == R.id.sitekey_custom) {
            return "Custom";
        }
        return "Challenge";
    }

    private HCaptcha getConfiguredClient() {
        final HCaptcha client = HCaptcha.getClient(this);
        client.setEmbeddedContainer(selectedMode == HCaptchaRenderMode.EMBEDDED ? embeddedChallengeContainer : null);
        return client;
    }

    private HCaptchaConfig getConfig() {
        final HCaptchaSize size = selectedMode == HCaptchaRenderMode.HEADLESS
                ? HCaptchaSize.INVISIBLE
                : getSelectedSize();
        final boolean isDark = themeDark.isChecked();
        return HCaptchaConfig.builder()
                .siteKey(getSelectedSitekey())
                .size(size)
                .renderMode(selectedMode)
                .loading(selectedMode != HCaptchaRenderMode.EMBEDDED && loading.isChecked())
                .disableHardwareAcceleration(disableHardwareAccel.isChecked())
                .theme(isDark ? HCaptchaTheme.DARK : HCaptchaTheme.LIGHT)
                .tokenExpiration(120)
                .diagnosticLog(true)
                .retryPredicate((config, exception) -> exception.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT)
                .build();
    }

    private void setPhoneModeUi(final boolean fullNumberMode) {
        phoneModeSwitch.setText(fullNumberMode
                ? getString(R.string.phone_mode_number)
                : getString(R.string.phone_mode_prefix));
        phoneModeSwitch.setTextColor(ContextCompat.getColor(this, R.color.hc_text_primary));
        phoneInputLayout.setPlaceholderText(fullNumberMode
                ? getString(R.string.phone_number_hint)
                : getString(R.string.phone_prefix_hint));
        phonePrefixInput.setInputType(fullNumberMode ? InputType.TYPE_CLASS_PHONE : InputType.TYPE_CLASS_NUMBER);
    }

    private void updateLoadingControlForMode() {
        final boolean embeddedMode = selectedMode == HCaptchaRenderMode.EMBEDDED;
        if (embeddedMode) {
            loading.setChecked(false);
            loading.setEnabled(false);
            loading.setAlpha(0.6f);
            return;
        }
        loading.setEnabled(true);
        loading.setAlpha(1f);
        loading.setChecked(loadingPreference);
    }

    private void addAuditLog(final String event) {
        final String line = auditTimeFormatter.format(new Date()) + "  " + event;
        auditEntries.add(0, line);
        if (auditEntries.size() > MAX_AUDIT_LOG_LINES) {
            auditEntries.remove(auditEntries.size() - 1);
        }
        updateAuditLogView();
    }

    private void updateAuditLogView() {
        if (auditEntries.isEmpty()) {
            auditLogTextView.setText("-");
            return;
        }
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < auditEntries.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(auditEntries.get(i));
        }
        auditLogTextView.setText(builder.toString());
    }

    private void showResult(final String status, final String message, final boolean successState) {
        resultStatusTextView.setText(status);
        resultMessageTextView.setText(message == null || message.isEmpty() ? "-" : message);
        resultCard.setBackgroundColor(ContextCompat.getColor(this,
                successState ? R.color.hc_result_ok_bg : R.color.hc_result_error_bg));
        copyTokenButton.setEnabled(lastToken != null && !lastToken.isEmpty());
        markUsedButton.setEnabled(tokenResponse != null);
    }

    private void clearOutput() {
        tokenResponse = null;
        lastToken = null;
        showResult(getString(R.string.result_idle), "-", true);
    }

    private void destroyCaptcha() {
        if (hCaptcha != null) {
            hCaptcha.destroy();
            hCaptcha = null;
            addAuditLog("hCaptcha destroyed");
        }
    }

    private void resetForModeChange() {
        destroyCaptcha();
        clearOutput();
    }

    private void withCustomHost(final Runnable action) {
        if (selectedMode == HCaptchaRenderMode.EMBEDDED && embeddedChallengeContainer == null) {
            Toast.makeText(this, "Render host container is unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        action.run();
    }

    private void updateCustomHostUiState() {
        if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
            setCustomHostStatus(R.string.embedded_host_closed, R.color.hc_text_secondary);
            setCustomLoading(false);
        } else {
            setCustomHostStatus(R.string.embedded_host_closed, R.color.hc_text_secondary);
            setCustomLoading(false);
        }
    }

    private void setCustomLoading(final boolean isLoading) {
        if (embeddedLoadingOverlay != null) {
            embeddedLoadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void setCustomHostStatus(final int statusResId, final int colorResId) {
        embeddedHostStateTextView.setText(statusResId);
        embeddedHostStateTextView.setTextColor(ContextCompat.getColor(this, colorResId));
    }

    public void onClickReset(final View view) {
        if (hCaptcha != null) {
            hCaptcha.reset();
            addAuditLog("hCaptcha reset");
        }
        hCaptcha = null;
        setCustomLoading(false);
        setCustomHostStatus(R.string.embedded_host_closed, R.color.hc_text_secondary);
        clearOutput();
        showResult("Status: Reset", "reset called", true);
    }

    public void onClickDestroy(final View view) {
        destroyCaptcha();
        setCustomLoading(false);
        setCustomHostStatus(R.string.embedded_host_closed, R.color.hc_text_secondary);
        clearOutput();
        showResult("Status: Destroyed", "destroy called", false);
    }

    public void onClickSetup(final View view) {
        withCustomHost(() -> {
            destroyCaptcha();
            hCaptcha = getConfiguredClient().setup(getConfig());
            setupClient(hCaptcha);
            showResult("Status: Setup complete", "Client preloaded", true);
            addAuditLog("Setup completed");
        });
    }

    public void onClickVerify(final View view) {
        clearOutput();

        final String input = phonePrefixInput.getText() != null ? phonePrefixInput.getText().toString().trim() : null;
        final String rqdata = rqdataInput.getText() != null ? rqdataInput.getText().toString().trim() : null;
        final HCaptchaVerifyParams verifyParams;
        if ((input != null && !input.isEmpty()) || (rqdata != null && !rqdata.isEmpty())) {
            final HCaptchaVerifyParams.HCaptchaVerifyParamsBuilder builder = HCaptchaVerifyParams.builder();
            if (input != null && !input.isEmpty()) {
                if (phoneModeSwitch.isChecked()) {
                    builder.phoneNumber(input);
                    addAuditLog("Verify params: phoneNumber set");
                } else {
                    builder.phonePrefix(input);
                    addAuditLog("Verify params: phonePrefix set");
                }
            }
            if (rqdata != null && !rqdata.isEmpty()) {
                builder.rqdata(rqdata);
                addAuditLog("Verify params: rqdata set");
            }
            verifyParams = builder.build();
        } else {
            verifyParams = null;
            addAuditLog("Verify params: none");
        }

        if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
            selectTopTab(TAB_CUSTOM);
            setCustomLoading(true);
            setCustomHostStatus(R.string.embedded_host_loading, R.color.hc_text_secondary);
        }

        withCustomHost(() -> {
            showResult("Status: Verifying", "Verification started", true);
            addAuditLog("Verification started");
            if (hCaptcha != null) {
                hCaptcha.verifyWithHCaptcha(verifyParams);
            } else {
                hCaptcha = getConfiguredClient().verifyWithHCaptcha(getConfig(), verifyParams);
                setupClient(hCaptcha);
            }
        });
    }

    public void onMarkUsed(final View view) {
        if (tokenResponse != null) {
            tokenResponse.markUsed();
            addAuditLog("Token marked used");
            Toast.makeText(this, "Token marked used", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No token response", Toast.LENGTH_SHORT).show();
        }
    }

    public void onHitTest(final View view) {
        Toast.makeText(this, "Hit Test!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onHitTest");
        addAuditLog("Hit test invoked");
    }

    public void onCopyToken(final View view) {
        if (lastToken == null || lastToken.isEmpty()) {
            Toast.makeText(this, R.string.no_token_to_copy, Toast.LENGTH_SHORT).show();
            return;
        }
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("hCaptcha Token", lastToken);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.token_copied, Toast.LENGTH_SHORT).show();
        addAuditLog("Token copied to clipboard");
    }

    public void onClearAuditLog(final View view) {
        auditEntries.clear();
        updateAuditLogView();
        addAuditLog("Audit log cleared");
    }

    private void setupClient(final HCaptcha client) {
        client
                .addOnLoadedListener(() -> {
                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                        setCustomLoading(false);
                        setCustomHostStatus(R.string.embedded_host_open, R.color.hc_text_secondary);
                    }
                })
                .addOnSuccessListener(response -> {
                    tokenResponse = response;
                    lastToken = response.getTokenResult();
                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                        setCustomLoading(false);
                        setCustomHostStatus(R.string.embedded_host_result_success, R.color.hc_status_success);
                    }
                    showResult("Status: Success", lastToken, true);
                    addAuditLog("Verification success");
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
                    tokenResponse = null;
                    lastToken = null;
                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                        setCustomLoading(false);
                        setCustomHostStatus(R.string.embedded_host_result_error, R.color.hc_status_error);
                    }
                    showResult("Status: Error", e.getMessage(), false);
                    addAuditLog("Verification failed: " + e.getMessage());
                })
                .addOnOpenListener(() -> {
                    if (selectedMode == HCaptchaRenderMode.EMBEDDED) {
                        setCustomLoading(false);
                        setCustomHostStatus(R.string.embedded_host_open, R.color.hc_text_secondary);
                    }
                    addAuditLog("Challenge opened");
                    Toast.makeText(MainActivity.this, "hCaptcha shown", Toast.LENGTH_SHORT).show();
                });
    }
}
