package opencontacts.open.com.opencontacts.activities;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.applyOptedTheme;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getThemeAttributeColor;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.setColorFilterUsingColor;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.setColorFilterUsingColorAttribute;

public abstract class AppBaseActivity extends AppCompatActivity{

    protected Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyOptedTheme(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(getLayoutResource());
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.more_overflow_menu));
        setColorFilterUsingColorAttribute(toolbar.getOverflowIcon(), android.R.attr.textColorSecondary, this);
        AndroidUtils.setBackButtonInToolBar(toolbar, this);
        super.onCreate(savedInstanceState);
    }

    abstract int getLayoutResource();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!menu.hasVisibleItems())
            return super.onCreateOptionsMenu(menu);
        int textColorPrimary = getThemeAttributeColor(android.R.attr.textColorSecondary, this);
        processMenu(menu, textColorPrimary);
        return super.onCreateOptionsMenu(menu);
    }

    private void processMenu(Menu menu, int textColorPrimary) {
        for(int i = 0, totalItems = menu.size(); i < totalItems; i++){
            MenuItem menuItem = menu.getItem(i);
            if(menuItem.hasSubMenu()) processMenu(menuItem.getSubMenu(), textColorPrimary);
            if(menuItem.getIcon() == null) continue;
            setColorFilterUsingColor(menuItem.getIcon(), textColorPrimary);
        }
    }
}
