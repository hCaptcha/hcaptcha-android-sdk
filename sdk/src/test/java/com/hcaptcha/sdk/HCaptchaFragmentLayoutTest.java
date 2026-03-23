package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Dialog;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatImageView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaFragmentLayoutTest {
    @Test
    public void layoutInflatedThroughPlainDialog_doesNotApplySrcCompat() {
        final Dialog dialog = new Dialog(RuntimeEnvironment.getApplication(), R.style.HCaptchaDialogTheme);
        final LayoutInflater inflater = LayoutInflater.from(RuntimeEnvironment.getApplication())
                .cloneInContext(dialog.getContext());

        final ImageView logo = inflateLogo(inflater);

        assertEquals(AppCompatImageView.class, logo.getClass());
        assertNotNull(logo.getDrawable());
    }

    private static ImageView inflateLogo(LayoutInflater inflater) {
        final View rootView = inflater.inflate(R.layout.hcaptcha_fragment, null, false);
        final LinearLayout loadingContainer = rootView.findViewById(R.id.loadingContainer);
        return (ImageView) loadingContainer.getChildAt(0);
    }
}
