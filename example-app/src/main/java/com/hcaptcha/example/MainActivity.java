package com.hcaptcha.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView tokenTextView;

    private TextView errorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.tokenTextView = findViewById(R.id.tokenTextView);
        this.errorTextView = findViewById(R.id.errorTextView);
        // For debugging purposes only
        // android.webkit.WebView.setWebContentsDebuggingEnabled(true);
    }

    private void setTokenTextView(final String text) {
        tokenTextView.setText(text.substring(0, Math.min(100, text.length())) + "...");
        errorTextView.setText("-");
    }

    private void setErrorTextView(final String error) {
        tokenTextView.setText("-");
        errorTextView.setText(error);
    }

    public void onClickHCaptchaInvisible(View v) {
        onClickHCaptcha(HCaptchaSize.INVISIBLE);
    }

    public void onClickHCaptchaCompact(View v) {
        onClickHCaptcha(HCaptchaSize.COMPACT);
    }

    public void onClickHCaptchaNormal(View v) {
        onClickHCaptcha(HCaptchaSize.NORMAL);
    }

    public void onClickHCaptcha(final HCaptchaSize hCaptchaSize) {
        setTokenTextView("-");
        final String YOUR_API_SITE_KEY = "10000000-ffff-ffff-ffff-000000000001";
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(YOUR_API_SITE_KEY)
                .size(hCaptchaSize)
                .loading(true)
                .build();
        HCaptcha.getClient(this).verifyWithHCaptcha(config)
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
                });
    }

}
