package opencontacts.open.com.opencontacts.activities;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.setColorFilterUsingColor;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.applyOptedTheme;
import static opencontacts.open.com.opencontacts.utils.ThemeUtils.getSecondaryColor;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public abstract class AppBaseActivity extends AppCompatActivity {

    protected Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applyOptedTheme(this);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(getLayoutResource());
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title());
        toolbar.setOverflowIcon(getResources().getDrawable(R.drawable.more_overflow_menu));
        setColorFilterUsingColor(toolbar.getOverflowIcon(), getSecondaryColor(this));
        AndroidUtils.setBackButtonInToolBar(toolbar, this);
        super.onCreate(savedInstanceState);
    }

    abstract int getLayoutResource();
    abstract int title();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!menu.hasVisibleItems())
            return super.onCreateOptionsMenu(menu);
        processMenu(menu, getSecondaryColor(this));
        return super.onCreateOptionsMenu(menu);
    }

    private void processMenu(Menu menu, int textColorPrimary) {
        for (int i = 0, totalItems = menu.size(); i < totalItems; i++) {
            MenuItem menuItem = menu.getItem(i);
            if (menuItem.hasSubMenu()) processMenu(menuItem.getSubMenu(), textColorPrimary);
            if (menuItem.getIcon() == null) continue;
            setColorFilterUsingColor(menuItem.getIcon(), textColorPrimary);
        }
    }
}
