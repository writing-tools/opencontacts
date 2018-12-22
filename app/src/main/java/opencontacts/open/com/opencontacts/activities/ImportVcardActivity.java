package opencontacts.open.com.opencontacts.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.VCardImporterAsyncTask;

public class ImportVcardActivity extends AppCompatActivity {

    private VCardImporterAsyncTask parser;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(permissionGranted()) {
            parser.execute();
        }
        else
            new AlertDialog.Builder(this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.could_not_process_import_without_storage_permission)
                    .setNeutralButton(R.string.okay, null)
                    .setOnDismissListener(dialog -> finish())
                    .create()
                    .show();
    }

    private ProgressBar progressBarComponent;
    private TextView textView_vCardsIgnored, textView_vCardsImported;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_vcard);
        progressBarComponent = findViewById(R.id.progressBar_vCard_Import);
        textView_vCardsIgnored = findViewById(R.id.textview_vcards_ignored);
        textView_vCardsImported = findViewById(R.id.textview_vcards_imported);
        progressBarComponent.setIndeterminate(false);
        progressBarComponent.setProgress(0);
        progressBarComponent.setVisibility(View.VISIBLE);

        Uri uri = getIntent().getData();
        parser = new VCardImporterAsyncTask(uri, new VCardImporterAsyncTask.ImportProgressListener() {
            @Override
            public void onTotalNumberOfCardsToBeImportedDetermined(int totalNumberOfCards) {
                progressBarComponent.setMax(totalNumberOfCards);
            }

            @Override
            public void onNumberOfCardsProcessedUpdate(int imported, int ignored) {
                progressBarComponent.setProgress(imported + ignored);
                textView_vCardsImported.setText(getString(R.string.total_cards_imported, imported));
                textView_vCardsIgnored.setText(getString(R.string.total_cards_ignored, ignored));
            }

            @Override
            public void onFinish(int imported, int ignored) {
                progressBarComponent.setProgress(progressBarComponent.getMax());
                textView_vCardsImported.setText(getString(R.string.total_cards_imported, imported));
                textView_vCardsIgnored.setText(getString(R.string.total_cards_ignored, ignored));
            }
        }, this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_contacts)
                .setMessage(R.string.do_you_want_to_import)
                .setPositiveButton(R.string.okay, (x, y) -> {
                    if(permissionGranted())
                        parser.execute();
                    else
                        requestPermission();
                })
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.grant_storage_permission)
                    .setMessage(R.string.grant_storage_permisson_detail)
                    .setNeutralButton(R.string.okay, null)
                    .setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123))
                    .create()
                    .show();
        }
    }

    private boolean permissionGranted() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

}