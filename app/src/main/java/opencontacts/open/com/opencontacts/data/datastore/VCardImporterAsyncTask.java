package opencontacts.open.com.opencontacts.data.datastore;

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
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.CrashUtils;

public class VCardImporterAsyncTask extends AsyncTask<Void, Object, Boolean> {
    private final String PROGRESS_TOTAL_NUMBER_OF_VCARDS = "total_vcards";
    private final String PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW = "number_of_vcards_imported_until_now";
    private final String PROGRESS_FINAL_RESULT_OF_IMPORT = "final_result_of_import";
    private final Uri fileUri;
    private final ImportProgressListener importProgressListener;
    private Context context;

    public VCardImporterAsyncTask(Uri fileUri, ImportProgressListener importProgressListener, Context context){
        this.fileUri = fileUri;
        this.importProgressListener = importProgressListener;
        this.context = context;
    }

    @Override
    protected Boolean doInBackground(Void[] voids) {
        try {
            InputStream vcardInputStream = context.getContentResolver().openInputStream(fileUri);
            List<VCard> vCards = Ezvcard.parse(vcardInputStream).all();
            publishProgress(PROGRESS_TOTAL_NUMBER_OF_VCARDS, vCards.size());
            int numberOfvCardsImported = 0, numberOfCardsIgnored = 0;
            for (VCard vcard : vCards) {
                if (processVCard(vcard)) ++numberOfvCardsImported;
                else ++numberOfCardsIgnored;
                publishProgress(PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW, numberOfvCardsImported, numberOfCardsIgnored);
            }
            publishProgress(PROGRESS_FINAL_RESULT_OF_IMPORT, numberOfvCardsImported, numberOfCardsIgnored);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG, context);
            CrashUtils.reportError(e, context);
        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG, context);
            CrashUtils.reportError(e, context);
        } catch (Exception e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.unexpected_error_happened, Toast.LENGTH_LONG, context);
            CrashUtils.reportError(e, context);
        }
        return false;
    }

    private boolean processVCard(VCard vcard) {
        return ContactsDBHelper.addContact(vcard, context) != null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean importedSuccessfully) {
        if(importedSuccessfully) Toast.makeText(context, R.string.imported_successfully, Toast.LENGTH_LONG).show();
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
