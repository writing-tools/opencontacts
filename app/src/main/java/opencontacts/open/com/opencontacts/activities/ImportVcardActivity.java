package opencontacts.open.com.opencontacts.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;

public class ImportVcardActivity extends AppCompatActivity {

    private VCardParser parser;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(permissionGranted()) {
            Uri uri = getIntent().getData();
            parser.execute(uri, this);
        }
        else
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Can not process this import without storage permission, please retry")
                    .setNeutralButton("Okay", null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    })
                    .create()
                    .show();
    }

    private final String PROGRESS_TOTAL_NUMBER_OF_VCARDS = "total_vcards";
    private final String PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW = "number_of_vcards_imported_until_now";
    private final String PROGRESS_FINAL_RESULT_OF_IMPORT = "final_result_of_import";
    private ProgressBar progressBarComponent;
    private TextView textView_vCardsIgnored, textView_vCardsImported;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_vcard);
        progressBarComponent = (ProgressBar) findViewById(R.id.progressBar_vCard_Import);
        textView_vCardsIgnored = (TextView) findViewById(R.id.textview_vcards_ignored);
        textView_vCardsImported = (TextView) findViewById(R.id.textview_vcards_imported);
        progressBarComponent.setIndeterminate(false);
        progressBarComponent.setProgress(0);
        progressBarComponent.setVisibility(View.VISIBLE);
        Uri uri = getIntent().getData();
        parser = new VCardParser();
        if(permissionGranted())
            parser.execute(uri, this);
        else
            requestPermission();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setTitle("Grant storage permission")
                    .setMessage("Grant storage phone permission to be able to export and import contacts")
                    .setNeutralButton("Okay", null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
                        }
                    })
                    .create()
                    .show();
        }
    }

    private boolean permissionGranted() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private class VCardParser  extends AsyncTask {
        Context context;
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                Uri uri = (Uri) params[0];
                context = (Context) params[1];
                InputStream vcardInputStream = context.getContentResolver().openInputStream(uri);
                List<VCard> vCards = Ezvcard.parse(vcardInputStream).all();
                publishProgress(PROGRESS_TOTAL_NUMBER_OF_VCARDS, vCards.size());
                int numberOfvCardsImported = 0, numberOfCardsIgnored = 0;
                StructuredName structuredName;
                FormattedName formattedName;
                for(VCard vcard : vCards){
                    structuredName = vcard.getStructuredName();
                    formattedName = vcard.getFormattedName();
                    if(structuredName == null)
                        if(formattedName == null){
                            numberOfCardsIgnored++;
                            continue;
                        }
                        else
                            save(formattedName.getValue(), vcard.getTelephoneNumbers());
                    else
                        save(structuredName, vcard.getTelephoneNumbers());
                    numberOfvCardsImported++;
                    publishProgress(PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW, numberOfvCardsImported, numberOfCardsIgnored);
                }
                publishProgress(PROGRESS_FINAL_RESULT_OF_IMPORT, numberOfvCardsImported, numberOfCardsIgnored);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG).show();
            }
            catch(Exception e){
                e.printStackTrace();
                Toast.makeText(context, R.string.unexpected_error_happened, Toast.LENGTH_LONG).show();
            }
            return null;
        }

        private void save(String firstName, List<Telephone> telephoneNumbers){
            Contact contact = new Contact(firstName, "");
            contact.save();
            for(Telephone telephoneNumber : telephoneNumbers){
                new PhoneNumber(telephoneNumber.getText(), contact, false).save();
            }
        }

        private void save(StructuredName structuredName, List<Telephone> telephoneNumbers){
            List<String> additionalNames = structuredName.getAdditionalNames();
            String lastName = structuredName.getFamily();
            if(additionalNames.size() > 0){
                StringBuffer nameBuffer = new StringBuffer();
                for(String additionalName : additionalNames)
                    nameBuffer.append(additionalName).append(" ");
                lastName = nameBuffer.append(structuredName.getFamily()).toString();
            }
            Contact contact = new Contact(structuredName.getGiven(), lastName);
            contact.save();
            for(Telephone telephoneNumber : telephoneNumbers){
                new PhoneNumber(telephoneNumber.getText(), contact, false).save();
            }

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Object o) {
            Toast.makeText(context, "Imported Successfully", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            super.onProgressUpdate(values);
            switch((String)values[0]) {
                case PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW:
                    int imported = (Integer) values[1], ignored = (Integer) values[2];
                    progressBarComponent.setProgress(imported + ignored);
                    textView_vCardsImported.setText("Total cards imported: " + imported);
                    textView_vCardsIgnored.setText("Total cards ignored: " + ignored);
                    break;
                case PROGRESS_FINAL_RESULT_OF_IMPORT:
                    progressBarComponent.setProgress(progressBarComponent.getMax());
                    textView_vCardsImported.setText("Total cards imported: " + values[1]);
                    textView_vCardsIgnored.setText("Total cards ignored: " + values[2]);
                    ContactsDataStore.refreshStoreAsync();
                    CallLogDataStore.updateCallLogAsyncForAllContacts(ImportVcardActivity.this);
                    break;
                case PROGRESS_TOTAL_NUMBER_OF_VCARDS:
                    progressBarComponent.setMax((Integer) values[1]);
                    break;

            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}