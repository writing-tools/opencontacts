package opencontacts.open.com.opencontacts.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import opencontacts.open.com.opencontacts.R;

public class ImageButtonWithTint extends android.support.v7.widget.AppCompatImageButton {
    public ImageButtonWithTint(Context context) {
        this(context, null);
    }

    public ImageButtonWithTint(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageButtonWithTint(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr == 0 ? R.attr.imageButtonWithTint : defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ImageButtonWithTint);
        int resourceId = typedArray.getResourceId(R.styleable.ImageButtonWithTint_android_src, -1);
        typedArray.recycle();
        if(resourceId == -1) return;
        Drawable drawable = TintedDrawablesStore.getTintedDrawable(resourceId, context);
        setImageDrawable(drawable);
    }

}
