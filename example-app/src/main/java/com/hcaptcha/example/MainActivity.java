package com.hcaptcha.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Spinner sizeSpinner;
    private CheckBox setupCheckBox;
    private CheckBox showDialog;
    private CheckBox loading;
    private TextView tokenTextView;
    private TextView errorTextView;

    private HCaptcha hCaptcha;
    private HCaptchaConfig hCaptchaConfig;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupCheckBox = findViewById(R.id.setupCheckBox);
        setupCheckBox.setOnCheckedChangeListener((checkBox, checked) -> {
            initHCaptcha();
        });

        sizeSpinner = (Spinner) findViewById(R.id.sizes);
        ArrayAdapter<HCaptchaSize> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(HCaptchaSize.NORMAL, HCaptchaSize.INVISIBLE, HCaptchaSize.COMPACT));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(adapter);
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                initHCaptcha();
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        tokenTextView = findViewById(R.id.tokenTextView);
        errorTextView = findViewById(R.id.errorTextView);

        CheckBox debugCheckBox = findViewById(R.id.webViewDebug);
        debugCheckBox.setOnCheckedChangeListener((checkBox, checked) -> {
            android.webkit.WebView.setWebContentsDebuggingEnabled(checked);
        });

        showDialog = findViewById(R.id.show_dialog);
        showDialog.setOnCheckedChangeListener((checkBox, checked) -> {
            initHCaptcha();
        });

        loading = findViewById(R.id.loading);
        loading.setOnCheckedChangeListener((checkBox, checked) -> {
            initHCaptcha();
        });

        initHCaptcha();
    }

    private HCaptchaSize getSizeFromSpinner() {
        return (HCaptchaSize) sizeSpinner.getSelectedItem();
    }

    private void initHCaptcha() {
        HCaptchaSize size = getSizeFromSpinner();
        boolean showDialog = this.showDialog.isChecked();
        boolean loading = this.loading.isChecked();

        HCaptchaConfig.HCaptchaConfigBuilder builder = HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .loading(loading)
                .showDialog(showDialog);
        if (size != null) {
            builder.size(size);
        }

        hCaptchaConfig = builder.build();

        hCaptcha = HCaptcha.getClient(this);
        if (setupCheckBox.isChecked()) {
            hCaptcha.setup(hCaptchaConfig);
        }
    }

    private void setTokenTextView(final String text) {
        tokenTextView.setText(text);
        errorTextView.setText("-");
    }

    private void setErrorTextView(final String error) {
        tokenTextView.setText("-");
        errorTextView.setText(error);
    }

    public HCaptcha verifyWithHCaptcha() {
        if (setupCheckBox.isChecked() || getSizeFromSpinner() == null) {
            return hCaptcha.verifyWithHCaptcha();
        }

        return hCaptcha.verifyWithHCaptcha(hCaptchaConfig);
    }

    public void onClickShowHCaptcha(final View v) {
        setTokenTextView("-");
        verifyWithHCaptcha()
                .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                    @Override
                    public void onSuccess(HCaptchaTokenResponse response) {
                        String userResponseToken = response.getTokenResult();
                        setTokenTextView(userResponseToken);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(HCaptchaException e) {
                        Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
                        setErrorTextView(e.getMessage());
                    }
                })
                .addOnOpenListener(new OnOpenListener() {
                    @Override
                    public void onOpen() {
                        Toast.makeText(MainActivity.this, "hCaptcha shown", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
