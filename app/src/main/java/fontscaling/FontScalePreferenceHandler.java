package fontscaling;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import opencontacts.open.com.opencontacts.R;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.widget.LinearLayout.VERTICAL;
import static fontscaling.FontScalingUtil.applyNewFontScaling;
import static fontscaling.FontScalingUtil.getSystemScaledDensity;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getTextSizeScaling;

public class FontScalePreferenceHandler {
    private float currentScale;
    private final Activity activity;
    private float originalTextSizeInSP;
    private final AppCompatSeekBar seekBar;
    private final AppCompatTextView contactNameExampleTextView;
    private final AppCompatTextView currentScaleTextView;
    private final LinearLayout container;

    public FontScalePreferenceHandler(Activity activity) {
        this.currentScale = getTextSizeScaling(activity);
        this.activity = activity;
        container = new LinearLayout(activity);
        container.setOrientation(VERTICAL);
        int containerMarginInPixels = (int)(Resources.getSystem().getDisplayMetrics().density * 16);
        container.setPadding(containerMarginInPixels, containerMarginInPixels, containerMarginInPixels, containerMarginInPixels);
        seekBar = new AppCompatSeekBar(activity);
        contactNameExampleTextView = new AppCompatTextView(activity);
        contactNameExampleTextView.setText("Sample text");
        currentScaleTextView = new AppCompatTextView(activity);
        container.addView(seekBar);
        container.addView(contactNameExampleTextView);
        container.addView(currentScaleTextView);
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser) return;
                updateScale(1 + ((progress - 50) / 100f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    private void updateScale(float newScale) {
        currentScale = newScale;
        contactNameExampleTextView.setTextSize(COMPLEX_UNIT_PX, getPixelsAtSystemScale(originalTextSizeInSP) * currentScale);
        currentScaleTextView.setText(String.valueOf(currentScale));
    }

    public void open() {
        AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setView(container)
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    applyNewFontScaling(currentScale, activity);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {
            int progress = (int) Math.ceil((currentScale - 1) * 100) + 50;
            seekBar.setProgress(progress);
            contactNameExampleTextView.setTextAppearance(activity, R.style.TextAppearance_AppCompat_Medium);
            originalTextSizeInSP = getSPAtCurrentScale(contactNameExampleTextView.getTextSize());
            updateScale(currentScale);
        });
        alertDialog.show();
    }

    private float getPixelsAtSystemScale(float sp) {
        return getSystemScaledDensity() * sp;
    }

    private float getSPAtCurrentScale(float px) {
        return px / activity.getResources().getDisplayMetrics().scaledDensity;
    }

}
