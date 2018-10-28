package opencontacts.open.com.opencontacts.data.datastore;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;

import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.ADDITION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.DELETION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.REFRESH;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.UPDATION;

public class ContactsDataStore {
    private static List<Contact> contacts = new ArrayList<>(1);
    private static List<DataStoreChangeListener<Contact>> dataChangeListeners = new ArrayList<>(3);

    public static List<Contact> getAllContacts() {
        if (contacts.size() == 0) {
            contacts = ContactsDBHelper.getAllContactsFromDB();
        }
        return new ArrayList<>(contacts);
    }

    public static void addContact(String firstName, String lastName, List<String> phoneNumbers) {
        opencontacts.open.com.opencontacts.orm.Contact dbContact = new opencontacts.open.com.opencontacts.orm.Contact(firstName, lastName);
        dbContact.save();
        ContactsDBHelper.replacePhoneNumbersInDB(dbContact, phoneNumbers, phoneNumbers.get(0));
        Contact newContactWithDatabaseId = ContactsDBHelper.getContact(dbContact.getId());
        contacts.add(newContactWithDatabaseId);
        notifyListenersAsync(ADDITION, newContactWithDatabaseId);
    }

    public static void removeContact(Contact contact) {
        if (contacts.remove(contact)) {
            ContactsDBHelper.deleteContactInDB(contact.id);
            notifyListenersAsync(DELETION, contact);
        }
    }

    public static void updateContact(Contact contact) {
        int indexOfContact = contacts.indexOf(contact);
        if (indexOfContact == -1)
            return;
        ContactsDBHelper.updateContactInDBWith(contact);
        reloadContact(contact.id);
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
        if (contactId == -1)
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
        new Thread(){
            @Override
            public void run() {
                contacts = ContactsDBHelper.getAllContactsFromDB();
                notifyListenersAsync(REFRESH, null);
            }
        }.start();
    }

    private static void notifyListenersAsync(final int type, final Contact contact){
        if(dataChangeListeners.isEmpty())
            return;
        new Thread(){
            @Override
            public void run() {
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
        }.start();
    }
}