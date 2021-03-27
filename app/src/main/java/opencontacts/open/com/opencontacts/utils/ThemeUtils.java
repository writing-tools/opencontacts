package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import opencontacts.open.com.opencontacts.activities.AppBaseActivity;

import static android.content.Context.WINDOW_SERVICE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getThemeAttributeColor;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getCurrentTheme;

public class ThemeUtils {
    public static int getSecondaryColor(Context context) {
        return getThemeAttributeColor(android.R.attr.textColorSecondary, context);
    }

    public static int getPrimaryColor(Context context) {
        return getThemeAttributeColor(android.R.attr.textColorPrimary, context);
    }

    public static int getHighlightColor(Context context) {
        return getThemeAttributeColor(android.R.attr.colorMultiSelectHighlight, context);
    }

    public static int getBackgroundColor(Context context) {
        return getThemeAttributeColor(android.R.attr.colorBackground, context);
    }

    public static int getBackgroundFloatingColor(Context context) {
        return getThemeAttributeColor(android.R.attr.colorBackgroundFloating, context);
    }

    public static void applyOptedTheme(Context context) {
        context.getTheme().applyStyle(getCurrentTheme(context), true);
//        setCustomFontSize(context);
    }

    private static void setCustomFontSize(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = 1f;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        ((AppBaseActivity)context).getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }
}
