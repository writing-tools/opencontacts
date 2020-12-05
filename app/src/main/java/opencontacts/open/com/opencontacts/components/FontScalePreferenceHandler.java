package opencontacts.open.com.opencontacts.components;

import android.content.Context;
import android.support.v4.util.Consumer;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import opencontacts.open.com.opencontacts.R;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.widget.LinearLayout.VERTICAL;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getTextSizeScaling;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getSystemScaledDensity;

public class FontScalePreferenceHandler {
    private float currentScale;
    private final Context context;
    private float originalTextSizeInSP;
    private final AppCompatSeekBar seekBar;
    private final AppCompatTextView contactNameExampleTextView;
    private final LinearLayout container;

    public FontScalePreferenceHandler(Context context) {
        this.currentScale = getTextSizeScaling(context);
        this.context = context;
        container = new LinearLayout(context);
        container.setOrientation(VERTICAL);
        seekBar = new AppCompatSeekBar(context);
        contactNameExampleTextView = new AppCompatTextView(context);
        contactNameExampleTextView.setText("David Lou");

        container.addView(seekBar);
        container.addView(contactNameExampleTextView);
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
        System.out.println("new scale yolo " + currentScale);
    }

    public void open(Consumer<Float> onScaleSave) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setView(container)
                .setPositiveButton(R.string.okay, (dialog, which) -> {
                    onScaleSave.accept(currentScale);
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        alertDialog.setOnShowListener(dialog -> {
            int progress = (int) Math.ceil((currentScale - 1) * 100) + 50;
            System.out.println("progress yolo " + progress);
            seekBar.setProgress(progress);
            contactNameExampleTextView.setTextAppearance(context, R.style.TextAppearance_AppCompat_Medium);
            originalTextSizeInSP = getSPAtCurrentScale(contactNameExampleTextView.getTextSize());
            System.out.println("sp we got yolo " + originalTextSizeInSP);
            updateScale(currentScale);
        });
        alertDialog.show();
    }

    private float getPixelsAtSystemScale(float sp) {
        return getSystemScaledDensity() * sp;
    }

    private float getSPAtCurrentScale(float px) {
        return px / context.getResources().getDisplayMetrics().scaledDensity;
    }

}
