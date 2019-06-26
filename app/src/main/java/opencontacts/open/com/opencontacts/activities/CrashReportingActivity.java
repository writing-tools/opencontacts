package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.sharePlainText;


public class CrashReportingActivity extends AppCompatActivity {

    public static final String EXCEPTION_BUNDLE_EXTRA_KEY = "Exception";
    public static final String IS_NOT_CRASH_BUNDLE_EXTRA_KEY = "isNotCrash";
    public static final String ERROR_CONTENT_BUNDLE_EXTRA_KEY = "errorContent";
    private AppCompatTextView crashLog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crash_reporting);
        setSupportActionBar(findViewById(R.id.toolbar));
        crashLog = findViewById(R.id.crash_log);
        crashLog.setText(getErrorContent());
        findViewById(R.id.button_copy).setOnClickListener(v -> AndroidUtils.copyToClipboard(crashLog.getText(), true, this));
        if(getIntent().getBooleanExtra(IS_NOT_CRASH_BUNDLE_EXTRA_KEY, false)) getSupportActionBar().setTitle(R.string.report_error_screen_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(R.string.share_menu_item)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setIcon(android.R.drawable.ic_menu_share)
                .setOnMenuItemClickListener(item -> {
                    sharePlainText(crashLog.getText().toString(), this);
                    return true;
                });
        return super.onCreateOptionsMenu(menu);
    }

    private String getErrorContent() {
        Throwable exception = (Throwable) getIntent().getSerializableExtra(EXCEPTION_BUNDLE_EXTRA_KEY);
        if(exception != null) return Log.getStackTraceString(exception);
        return getIntent().getStringExtra(ERROR_CONTENT_BUNDLE_EXTRA_KEY);
    }
}