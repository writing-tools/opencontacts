package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;

import opencontacts.open.com.opencontacts.BuildConfig;
import opencontacts.open.com.opencontacts.R;

public class AboutActivity extends AppBaseActivity{
    @Override
    int getLayoutResource() {
        return R.layout.activity_about;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatTextView)findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);
    }
}
