package opencontacts.open.com.opencontacts.views;

import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SIGNAL;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.TELEGRAM;

import android.content.Context;
import android.util.AttributeSet;


import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils;

public class SocialAppButton extends ImageButtonWithTint {
    public SocialAppButton(Context context) {
        this(context, null);
    }

    public SocialAppButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SocialAppButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        String defaultSocialApp = SharedPreferencesUtils.defaultSocialAppEnabled(context);
        if (defaultSocialApp.equalsIgnoreCase(TELEGRAM)) setImageResource(R.drawable.ic_telegram);
        else if (defaultSocialApp.equalsIgnoreCase(SIGNAL)) setImageResource(R.drawable.ic_signal_app);
        else setImageResource(R.drawable.ic_whatsapp);
    }

}
