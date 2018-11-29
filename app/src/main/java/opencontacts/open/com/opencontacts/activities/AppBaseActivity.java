package opencontacts.open.com.opencontacts.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.applyOptedTheme;

public abstract class AppBaseActivity extends AppCompatActivity{

    protected Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyOptedTheme(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(getLayoutResource());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.more_overflow_menu));
        AndroidUtils.setBackButtonInToolBar(toolbar, this);
        super.onCreate(savedInstanceState);
    }

    abstract int getLayoutResource();
}
