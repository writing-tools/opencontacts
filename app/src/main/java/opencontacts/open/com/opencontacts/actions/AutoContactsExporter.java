package opencontacts.open.com.opencontacts.actions;

import android.Manifest;
import android.content.Context;

import java.io.IOException;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SampleDataStoreChangeListener;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static android.widget.Toast.LENGTH_LONG;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasItBeenAWeekSinceLastExportOfContacts;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.markAutoExportComplete;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldExportContactsEveryWeek;

public class AutoContactsExporter {
    private Context context;

    public AutoContactsExporter(Context context) {
        this.context = context;
    }

    public void exportContactsAsPerPreferences(){
        if(shouldExportContactsEveryWeek(context) && hasItBeenAWeekSinceLastExportOfContacts(context))
            loadAndExportContacts();
    }

    private void loadAndExportContacts() {
        if(ContactsDataStore.getAllContacts().isEmpty())//loading contacts meanwhile
            ContactsDataStore.addDataChangeListener(new SampleDataStoreChangeListener<Contact>() {
                @Override
                public void onStoreRefreshed() {
                    if(!ContactsDataStore.getAllContacts().isEmpty())
                        processAsync(AutoContactsExporter.this::exportContactsAndUpdateLastExportTimeStamp);
                    ContactsDataStore.removeDataChangeListener(this);
                }
            });
        else processAsync(AutoContactsExporter.this::exportContactsAndUpdateLastExportTimeStamp);
    }

    private void exportContactsAndUpdateLastExportTimeStamp() {
        if(!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) {
            toastFromNonUIThread(R.string.grant_storage_permisson_detail, LENGTH_LONG, context);
            return;
        }
        try {
            DomainUtils.exportAllContacts(context);
            markAutoExportComplete(context);
        } catch (IOException e) {
            e.printStackTrace();
            toastFromNonUIThread(R.string.failed_exporting_contacts, LENGTH_LONG, context);
        }

    }

}
