package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.ADDITION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.DELETION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.REFRESH;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.UPDATION;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;

public class ContactsDataStore {
    private static List<Contact> contacts = null;
    private static List<DataStoreChangeListener<Contact>> dataChangeListeners = new ArrayList<>(3);

    public static List<Contact> getAllContacts() {
        if (contacts == null) {
            refreshStoreAsync();
            return new ArrayList<>(0);
        }
        return new ArrayList<>(contacts);
    }

    public static void addContact(VCard vCard, Context context) {
        opencontacts.open.com.opencontacts.orm.Contact newContactWithDatabaseId = ContactsDBHelper.addContact(vCard, context);
        Contact addedDomainContact = ContactsDBHelper.getContact(newContactWithDatabaseId.getId());
        contacts.add(addedDomainContact);
        notifyListenersAsync(ADDITION, addedDomainContact);
        CallLogDataStore.updateCallLogAsyncForNewContact(addedDomainContact);
    }

    public static void removeContact(Contact contact) {
        if (contacts.remove(contact)) {
            ContactsDBHelper.deleteContactInDB(contact.id);
            notifyListenersAsync(DELETION, contact);
        }
    }

    public static void updateContact(long contactId, PhoneNumber primaryNumber, VCard vCard, Context context) {
        ContactsDBHelper.updateContactInDBWith(contactId, primaryNumber, vCard, context);
        reloadContact(contactId);
        CallLogDataStore.updateCallLogAsyncForNewContact(getContactWithId(contactId));
    }

    private static void reloadContact(long contactId) {
        int indexOfContact = contacts.indexOf(new Contact(contactId));
        if (indexOfContact == -1)
            return;
        Contact contactFromDB = ContactsDBHelper.getContact(contactId);
        contacts.set(indexOfContact, contactFromDB);
        notifyListenersAsync(UPDATION, contactFromDB);
    }

    public static void addDataChangeListener(DataStoreChangeListener<Contact> changeListener) {
        dataChangeListeners.add(changeListener);
    }

    public static void removeDataChangeListener(DataStoreChangeListener<Contact> changeListener) {
        dataChangeListeners.remove(changeListener);
    }

    public static opencontacts.open.com.opencontacts.orm.Contact getContact(String phoneNumber) {
        return ContactsDBHelper.getContactFromDB(phoneNumber);
    }

    public static Contact getContactWithId(long contactId) {
        if (contactId == -1 || contacts == null)
            return null;
        int indexOfContact = contacts.indexOf(new Contact(contactId));
        if (indexOfContact == -1)
            return null;
        return contacts.get(indexOfContact);
    }

    public static void updateContactsAccessedDateAsync(final List<CallLogEntry> newCallLogEntries) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                for (CallLogEntry callLogEntry : newCallLogEntries) {
                    long contactId = callLogEntry.getContactId();
                    if(getContactWithId(contactId) == null)
                        continue;
                    ContactsDBHelper.updateLastAccessed(contactId, callLogEntry.getDate());
                }
                refreshStoreAsync();
                return null;
            }
        }.execute();
    }

    public static void togglePrimaryNumber(String mobileNumber, long contactId) {
        ContactsDBHelper.togglePrimaryNumber(mobileNumber, getContactWithId(contactId));
        reloadContact(contactId);
    }

    public static void refreshStoreAsync() {
        processAsync(ContactsDataStore::refreshStore);
    }

    private static void refreshStore() {
        contacts = ContactsDBHelper.getAllContactsFromDB();
        notifyListeners(REFRESH, null);
    }

    private static void notifyListenersAsync(final int type, final Contact contact){
        if(dataChangeListeners.isEmpty())
            return;
        processAsync(() -> notifyListeners(type, contact));
    }

    private static void notifyListeners(int type, Contact contact) {
        if(dataChangeListeners.isEmpty())
            return;
        Iterator<DataStoreChangeListener<Contact>> iterator = dataChangeListeners.iterator();
        if(type == ADDITION)
            while(iterator.hasNext())
                iterator.next().onAdd(contact);
        else if(type == DELETION)
            while(iterator.hasNext())
                iterator.next().onRemove(contact);
        else if(type == UPDATION)
            while(iterator.hasNext())
                iterator.next().onUpdate(contact);
        else if (type == REFRESH)
            while(iterator.hasNext())
                iterator.next().onStoreRefreshed();
    }

    public static void deleteAllContacts(Context context) {
        processAsync(() -> {
            ContactsDBHelper.deleteAllContacts();
            refreshStore();
            toastFromNonUIThread(R.string.deleted_all_contacts, Toast.LENGTH_LONG, context);
        });
    }

    public static VCardData getVCardData(long contactId){
        return ContactsDBHelper.getVCard(contactId);
    }

    public static void init() {
        refreshStoreAsync();
    }
}