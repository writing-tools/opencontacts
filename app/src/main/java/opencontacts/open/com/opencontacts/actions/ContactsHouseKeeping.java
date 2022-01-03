package opencontacts.open.com.opencontacts.actions;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;

import android.content.Context;

import java.util.List;

import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SampleDataStoreChangeListener;

public class ContactsHouseKeeping {
    private Context context;

    public ContactsHouseKeeping(Context context) {
        this.context = context;
    }

    public void start() {
        if (ContactsDataStore.getAllContacts().isEmpty())//loading contacts meanwhile
            ContactsDataStore.addDataChangeListener(new SampleDataStoreChangeListener<Contact>() {
                @Override
                public void onStoreRefreshed() {
                    if (!ContactsDataStore.getAllContacts().isEmpty())
                        processAsync(() -> performHouseKeeping(ContactsDataStore.getAllContacts()));
                    ContactsDataStore.removeDataChangeListener(this);
                }
            });
    }

    private void performHouseKeeping(List<Contact> allContacts) {
        new AutoContactsExporter().perform(allContacts, context);
        new DeleteTemporaryContacts().perform(allContacts, context);
    }

}
