package opencontacts.open.com.opencontacts.data.datastore;

import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.invalidateGroups;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getEncryptingContactsKey;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasEncryptingContactsKey;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.CrashUtils;
import opencontacts.open.com.opencontacts.utils.ZipUtils;

public class VCardImporterAsyncTask extends AsyncTask<Void, Object, List<Pair<VCard, Throwable>>> {
    private final String PROGRESS_TOTAL_NUMBER_OF_VCARDS = "total_vcards";
    private final String PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW = "number_of_vcards_imported_until_now";
    private final String PROGRESS_FINAL_RESULT_OF_IMPORT = "final_result_of_import";
    private final Uri fileUri;
    private final ImportProgressListener importProgressListener;
    private WeakReference<Context> contextWeakReference;

    public VCardImporterAsyncTask(Uri fileUri, ImportProgressListener importProgressListener, Context context) {
        this.fileUri = fileUri;
        this.importProgressListener = importProgressListener;
        this.contextWeakReference = new WeakReference<>(context);
    }

    @Override
    protected List<Pair<VCard, Throwable>> doInBackground(Void[] voids) {
        List<Pair<VCard, Throwable>> vcardsAndTheirExceptions = new ArrayList<>();
        try {
            InputStream vcardInputStream = contextWeakReference.get().getContentResolver().openInputStream(fileUri);
            if (fileUri.toString().endsWith(".zip"))
                vcardInputStream = getPlainTextInputStreamFromZip(vcardInputStream);
            List<VCard> vCards = Ezvcard.parse(vcardInputStream).all();
            publishProgress(PROGRESS_TOTAL_NUMBER_OF_VCARDS, vCards.size());
            int numberOfvCardsImported = 0, numberOfCardsIgnored = 0;
            ContactsDataStore.requestPauseOnUpdates();
            for (VCard vcard : vCards) {
                try {
                    if (processVCard(vcard)) ++numberOfvCardsImported;
                    else ++numberOfCardsIgnored;
                } catch (Exception e) {
                    ++numberOfCardsIgnored;
                    vcardsAndTheirExceptions.add(new Pair<>(vcard, e));
                }
                publishProgress(PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW, numberOfvCardsImported, numberOfCardsIgnored);
            }
            publishProgress(PROGRESS_FINAL_RESULT_OF_IMPORT, numberOfvCardsImported, numberOfCardsIgnored);
            return vcardsAndTheirExceptions;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG, contextWeakReference.get());
            CrashUtils.reportError(e, contextWeakReference.get());
        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.error_while_parsing_vcard_file, Toast.LENGTH_LONG, contextWeakReference.get());
            CrashUtils.reportError(e, contextWeakReference.get());
        } catch (NoPasswordFoundException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.set_password_before_import, Toast.LENGTH_LONG, contextWeakReference.get());
            vcardsAndTheirExceptions.add(new Pair<>(null, e));
        } catch (Exception e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.unexpected_error_happened, Toast.LENGTH_LONG, contextWeakReference.get());
            CrashUtils.reportError(e, contextWeakReference.get());
        }
        return vcardsAndTheirExceptions;
    }

    @NonNull
    private InputStream getPlainTextInputStreamFromZip(InputStream vcardInputStream) throws Exception {
        if (!hasEncryptingContactsKey(contextWeakReference.get()))
            throw new NoPasswordFoundException();
        return ZipUtils.getPlainTextInputStreamFromZip(getEncryptingContactsKey(contextWeakReference.get()), vcardInputStream);
    }

    private boolean processVCard(VCard vcard) {
        try {
            return ContactsDBHelper.addContact(vcard, contextWeakReference.get()) != null;
        } catch (Exception e) {
            throw e;
        }

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(List<Pair<VCard, Throwable>> vCardsAndTheirExceptions) {
        if (vCardsAndTheirExceptions.isEmpty())
            AndroidUtils.toastFromNonUIThread(R.string.imported_successfully, Toast.LENGTH_LONG, contextWeakReference.get());
        importProgressListener.onFinish(vCardsAndTheirExceptions);
    }

    @Override
    protected void onProgressUpdate(Object[] values) {
        super.onProgressUpdate(values);
        switch ((String) values[0]) {
            case PROGRESS_NUMBER_OF_VCARDS_PROCESSED_UNTIL_NOW:
                importProgressListener.onNumberOfCardsProcessedUpdate((Integer) values[1], (Integer) values[2]);
                break;
            case PROGRESS_FINAL_RESULT_OF_IMPORT:
                ContactsDataStore.requestResumeUpdates();
                ContactsDataStore.refreshStoreAsync();
                invalidateGroups();
                CallLogDataStore.updateCallLogAsyncForAllContacts(contextWeakReference.get());
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

    public interface ImportProgressListener {
        void onTotalNumberOfCardsToBeImportedDetermined(int totalNumberOfCards);

        void onNumberOfCardsProcessedUpdate(int imported, int ignored);

        void onFinish(List<Pair<VCard, Throwable>> vCardsAndTheirExceptions);
    }

}

class NoPasswordFoundException extends Exception {

}
