package opencontacts.open.com.opencontacts.activities;

import static android.text.TextUtils.isEmpty;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import opencontacts.open.com.opencontacts.R;

public class DataSourcePermissions extends AppBaseActivity {

    private static final String PERMISSIONS_EXTRA = "PERMISSIONS";
    private AppCompatImageView iconImageView;
    private AppCompatTextView appNameTextView;
    private AppCompatTextView packageNameTextView;
    private AppCompatTextView requestedPermissionsTextView;
    private PackageManager packageManager;
    private View authorizeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!hasValidIntent()) {
            finish();
            return;
        }
        initializeViews();
        showCallingAppAndRequiredPermissions();
    }

    private void initializeViews() {
        iconImageView = findViewById(R.id.calling_app_icon);
        appNameTextView = findViewById(R.id.calling_app_name);
        packageNameTextView = findViewById(R.id.calling_app_package_name);
        requestedPermissionsTextView = findViewById(R.id.permissions_list);
        authorizeButton = findViewById(R.id.authorize_button);

        authorizeButton.setOnClickListener(v -> authorize());
    }

    private void authorize() {
        setResult(123, new Intent().putExtra("CODE", "yolo"));
        finish();
    }

    private boolean hasValidIntent() {
        Intent intent = getIntent();
        String[] requiredPermissions = intent.getStringArrayExtra(PERMISSIONS_EXTRA);
        if(requiredPermissions == null || requiredPermissions.length == 0) {
            makeText(this, "No valid permissions requested", LENGTH_LONG).show();
            return false;
        }
        String callingPackage = this.getCallingPackage();
        if(isEmpty(callingPackage)) {
            makeText(this, "Could not retrieve calling app details", LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void showCallingAppAndRequiredPermissions() {
        PackageInfo callingPackageInfo = getCallingPackageInfo();
        if(callingPackageInfo == null) {
            finish();
            return;
        }
        try {
            iconImageView.setImageDrawable(packageManager.getApplicationIcon(callingPackageInfo.packageName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        };
        appNameTextView.setText(callingPackageInfo.applicationInfo.name);
        packageNameTextView.setText(callingPackageInfo.packageName);
        String[] permissions = getIntent().getStringArrayExtra(PERMISSIONS_EXTRA);
        String permissionsRequestedAsText = TextUtils.join(", ", permissions);
        requestedPermissionsTextView.setText(permissionsRequestedAsText);
    }

    private PackageInfo getCallingPackageInfo() {
        packageManager = getPackageManager();
        try {
            return packageManager.getPackageInfo(getCallingPackage(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            makeText(this, "Could not retrieve the app details", LENGTH_LONG).show();
            return null;
        }
    }


    @Override
    int getLayoutResource() {
        return R.layout.activity_data_source_permissions;
    }
}
