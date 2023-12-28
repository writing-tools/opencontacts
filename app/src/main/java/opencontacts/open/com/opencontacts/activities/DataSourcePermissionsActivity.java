package opencontacts.open.com.opencontacts.activities;

import static android.text.TextUtils.isEmpty;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;

import static java.util.Collections.emptyList;

import static open.com.opencontactsdatasourcecontract.Contract.PermissionsActivity.PERMISSIONS_EXTRA;
import static open.com.opencontactsdatasourcecontract.Contract.PermissionsActivity.RESULT_AUTH_CODE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.wrapInConfirmation;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.savePermissionsGranted;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.saveAuthCode;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import opencontacts.open.com.opencontacts.R;

public class DataSourcePermissionsActivity extends AppBaseActivity {

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

        authorizeButton.setOnClickListener(v -> wrapInConfirmation(this::authorize, this));
    }

    private void authorize() {
        String authCode = UUID.randomUUID().toString();
        saveAuthCode(this, getCallingPackageName(), authCode);
        savePermissionsGranted(this, getCallingPackageName(), requestedPermissions());
        setResult(RESULT_OK,
            new Intent().putExtra(RESULT_AUTH_CODE, authCode)
            .putExtra(PERMISSIONS_EXTRA, requestedPermissions().toArray(new String[]{}))
        );
        finish();
    }

    private List<String> requestedPermissions() {
        String[] requiredPermissions = getIntent().getStringArrayExtra(PERMISSIONS_EXTRA);
        if(requiredPermissions == null) return emptyList();
        return Arrays.asList(requiredPermissions);
    }

    private String getCallingPackageName() {
        return getCallingPackage();
    }

    private boolean hasValidIntent() {
        List<String> requiredPermissions = requestedPermissions();
        if(requiredPermissions.isEmpty()) {
            makeText(this, "No valid permissions requested", LENGTH_LONG).show();
            return false;
        }
        if(isEmpty(getCallingPackageName())) {
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
        }
        appNameTextView.setText(callingPackageInfo.applicationInfo.name);
        packageNameTextView.setText(callingPackageInfo.packageName);
        List<String> permissions = requestedPermissions();
        String permissionsRequestedAsText = TextUtils.join(", ", permissions);
        requestedPermissionsTextView.setText(permissionsRequestedAsText);
    }

    @Nullable
    private PackageInfo getCallingPackageInfo() {
        packageManager = getPackageManager();
        try {
            String callingPackage = getCallingPackage();
            if(callingPackage == null) {
                finish();
                return null;
            }
            return packageManager.getPackageInfo(callingPackage, PackageManager.GET_META_DATA);
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
