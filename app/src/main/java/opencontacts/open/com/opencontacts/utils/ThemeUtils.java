package opencontacts.open.com.opencontacts.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import opencontacts.open.com.opencontacts.activities.AppBaseActivity;

import static android.content.Context.WINDOW_SERVICE;
import static opencontacts.open.com.opencontacts.OpenContactsApplication.fontScalingSet;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getThemeAttributeColor;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getCurrentTheme;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getTextSizeScaling;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.saveTextSizeScaling;

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
        if(!fontScalingSet)
        setCustomFontSize(context);
    }

    private static void setCustomFontSize(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.fontScale = configuration.fontScale * getTextSizeScaling(context);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = metrics.density * configuration.fontScale;
        ((AppBaseActivity)context).getBaseContext().getResources().updateConfiguration(configuration, metrics);
        fontScalingSet = true;
    }

    public static void applyNewFontScaling(float newFontScale, Activity activity) {
        Configuration configuration = activity.getResources().getConfiguration();
        float oldScaling = getTextSizeScaling(activity);
        configuration.fontScale = (configuration.fontScale / oldScaling) * newFontScale;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = (metrics.density / oldScaling) * newFontScale;
        activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
        saveTextSizeScaling(newFontScale, activity);
        activity.recreate();
    }

    public static float getSystemScaledDensity(){
        return Resources.getSystem().getDisplayMetrics().scaledDensity;
    }
}
