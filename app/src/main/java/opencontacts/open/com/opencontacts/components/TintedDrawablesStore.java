package opencontacts.open.com.opencontacts.components;

import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getBackgroundFloatingColor;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getPrimaryColor;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public class TintedDrawablesStore {
    public static Map<Integer, Drawable> tintedDrawables = new HashMap<>();
    private static int drawablesTheme = -1;

    public static Drawable getTintedDrawable(@DrawableRes int drawableRes, Context context) {
        Drawable cachedDrawable = tintedDrawables.get(drawableRes);
        return cachedDrawable == null ? getDrawableFor(drawableRes, context) : cachedDrawable;
    }

    private static Drawable getDrawableFor(@DrawableRes int drawableRes, Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if (drawable == null) return null;
        AndroidUtils.setColorFilterUsingColor(drawable, getPrimaryColor(context));
        tintedDrawables.put(drawableRes, drawable);
        return drawable;
    }

    public static void setDrawableForFAB(@DrawableRes int drawableRes, FloatingActionButton fab, Context context) {
        fab.setImageDrawable(getTintedDrawable(drawableRes, context));
        fab.setBackgroundTintList(ColorStateList.valueOf(getBackgroundFloatingColor(context)));
    }

    public static void resetOnThemeMismatch(int theme) {
        if(drawablesTheme == theme) return;
        drawablesTheme = theme;
        tintedDrawables.clear();
    }
}
