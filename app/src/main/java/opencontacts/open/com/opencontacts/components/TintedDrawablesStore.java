package opencontacts.open.com.opencontacts.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public class TintedDrawablesStore {
    public static Map<Integer, Drawable> tintedDrawables = new HashMap<>();

    public static Drawable getTintedDrawable(@DrawableRes int drawableRes, Context context){
        Drawable cachedDrawable = tintedDrawables.get(drawableRes);
        return cachedDrawable == null ? getDrawableFor(drawableRes, context) : cachedDrawable;
    }

    private static Drawable getDrawableFor(@DrawableRes int drawableRes, Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if(drawable == null) return null;
        AndroidUtils.setColorFilterUsingColorAttribute(drawable, android.R.attr.textColorPrimary, context);
        tintedDrawables.put(drawableRes, drawable);
        return drawable;
    }

    public static void reset(){
        tintedDrawables.clear();
    }
}
