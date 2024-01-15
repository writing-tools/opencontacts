package opencontacts.open.com.opencontacts.utils;

import static open.fontscaling.FontScalingUtil.setCustomFontSizeOnViewCreated;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getThemeAttributeColor;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getCurrentTheme;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import opencontacts.open.com.opencontacts.components.TintedDrawablesStore;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getThemeAttributeColor(android.R.attr.colorBackgroundFloating, context);
        } else return getThemeAttributeColor(android.R.attr.colorBackground, context);
    }

    public static void applyOptedTheme(Activity activity) {
        int theme = getCurrentTheme(activity);
        activity.getTheme().applyStyle(theme, true);
        TintedDrawablesStore.resetOnThemeMismatch(theme);
        setCustomFontSizeOnViewCreated(activity);
    }

}
