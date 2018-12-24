package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
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
import opencontacts.open.com.opencontacts.orm.VCardData;

public class VCardImporterAsyncTask extends AsyncTask<Void, Object, Void> {
    private final String PROGRESS_TOTAL_NUMBER_OF_VCARDS = "total_vcards";
    private final String PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW = "number_of_vcards_imported_until_now";
    private final String PROGRESS_FINAL_RESULT_OF_IMPORT = "final_result_of_import";
    private final Uri fileUri;
    private final ImportProgressListener importProgressListener;
    private Context context;
    private final String noNameString;

    public VCardImporterAsyncTask(Uri fileUri, ImportProgressListener importProgressListener, Context context){
        this.fileUri = fileUri;
        this.importProgressListener = importProgressListener;
        this.context = context;
        noNameString = context.getString(R.string.noname);
    }

    @Override
    protected Void doInBackground(Void[] voids) {
        try {
            InputStream vcardInputStream = context.getContentResolver().openInputStream(fileUri);
            List<VCard> vCards = Ezvcard.parse(vcardInputStream).all();
            publishProgress(PROGRESS_TOTAL_NUMBER_OF_VCARDS, vCards.size());
            int numberOfvCardsImported = 0, numberOfCardsIgnored = 0;
            for (VCard vcard : vCards) {
                if (processVCardAndReturnIfItsASuccess(vcard)) ++numberOfvCardsImported;
                else ++numberOfCardsIgnored;
                publishProgress(PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW, numberOfvCardsImported, numberOfCardsIgnored);
            }
            publishProgress(PROGRESS_FINAL_RESULT_OF_IMPORT, numberOfvCardsImported, numberOfCardsIgnored);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.unexpected_error_happened, Toast.LENGTH_LONG).show();
        }
        return null;
    }

    private boolean processVCardAndReturnIfItsASuccess(VCard vcard){
        Contact contact = createContactSaveInDBAndReturnIt(vcard);
        createMobileNumbersAndSaveInDB(vcard, contact);
        createVCardDataAndSaveInDB(vcard, contact);
        return true;
    }

    private void createVCardDataAndSaveInDB(VCard vcard, Contact contact) {
        new VCardData(contact, vcard.write()).save();
    }

    private void createMobileNumbersAndSaveInDB(VCard vcard, Contact contact) {
        for (Telephone telephoneNumber : vcard.getTelephoneNumbers()) {
            new PhoneNumber(telephoneNumber.getText(), contact, false).save();
        }
    }

    private Contact createContactSaveInDBAndReturnIt(VCard vcard) {
        Contact contact;
        StructuredName structuredName = vcard.getStructuredName();
        FormattedName formattedName = vcard.getFormattedName();
        if (structuredName == null)
            if (formattedName == null) {
                contact = new Contact(noNameString, "");
            } else contact = new Contact(formattedName.getValue(), "");
        else contact = createContactWithStructuredName(structuredName);
        contact.save();
        return contact;
    }

    private Contact createContactWithStructuredName(StructuredName structuredName) {
        List<String> additionalNames = structuredName.getAdditionalNames();
        String lastName = structuredName.getFamily();
        if (additionalNames.size() > 0) {
            StringBuilder nameBuffer = new StringBuilder();
            for (String additionalName : additionalNames)
                nameBuffer.append(additionalName).append(" ");
            lastName = nameBuffer.append(structuredName.getFamily()).toString();
        }
        return new Contact(structuredName.getGiven(), lastName);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void ignore) {
        Toast.makeText(context, R.string.imported_successfully, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        switch ((String) values[0]) {
            case PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW:
                importProgressListener.onNumberOfCardsProcessedUpdate((Integer) values[1], (Integer) values[2]);
                break;
            case PROGRESS_FINAL_RESULT_OF_IMPORT:
                ContactsDataStore.refreshStoreAsync();
                CallLogDataStore.updateCallLogAsyncForAllContacts(context);
                importProgressListener.onFinish((Integer) values[1], (Integer) values[2]);
                break;
            case PROGRESS_TOTAL_NUMBER_OF_VCARDS:
                importProgressListener.onTotalNumberOfCardsToBeImportedDetermined((Integer) values[1]);
                break;

        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    public interface ImportProgressListener{
        void onTotalNumberOfCardsToBeImportedDetermined(int totalNumberOfCards);
        void onNumberOfCardsProcessedUpdate(int imported, int ignored);
        void onFinish(int numberOfCardsImported, int numberOfCardsIgnored);
    }

}
