package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.github.underscore.U;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Favorite;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static opencontacts.open.com.opencontacts.data.datastore.DataStoreState.LOADED;
import static opencontacts.open.com.opencontacts.data.datastore.DataStoreState.LOADING;
import static opencontacts.open.com.opencontacts.data.datastore.DataStoreState.NONE;
import static opencontacts.open.com.opencontacts.data.datastore.DataStoreState.REFRESHING;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.computeGroupsAsync;
import static opencontacts.open.com.opencontacts.domain.Contact.createDummyContact;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.ADDITION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.DELETION;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.REFRESH;
import static opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener.UPDATION;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.markFavoriteInVCard;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.mergeVCardStrings;

public class ContactsDataStore {
    private static List<Contact> contacts = null;
    private static final List<DataStoreChangeListener<Contact>> dataChangeListeners = Collections.synchronizedList(new ArrayList<>(3));
    private static List<Contact> favorites = new ArrayList<>(0);
    private static boolean pauseUpdates;
    private static int currentState;

    public synchronized static List<Contact> getAllContacts() {
        if(currentState == LOADING) {
            System.out.println("skipping the load yolo");
            return Collections.emptyList();
        }
        if (currentState == NONE ) {
            currentState = LOADING;
            refreshStoreAsync();
            return Collections.emptyList();
        }
        return new ArrayList<>(contacts);
    }

    public static void requestPauseOnUpdates() {
        pauseUpdates = true;
    }

    public static void requestResumeUpdates() {
        pauseUpdates = false;
        notifyListenersAsync(REFRESH, null);
    }

    public static void addContact(VCard vCard, Context context) {
        opencontacts.open.com.opencontacts.orm.Contact newContactWithDatabaseId = ContactsDBHelper.addContact(vCard, context);
        Contact addedDomainContact = ContactsDBHelper.getContact(newContactWithDatabaseId.getId());
        contacts.add(addedDomainContact);
        ContactGroupsDataStore.handleNewContactAddition(addedDomainContact);
        notifyListenersAsync(ADDITION, addedDomainContact);
        CallLogDataStore.updateCallLogAsyncForNewContact(addedDomainContact);
    }

    public static void removeContact(Contact contact) {
        if (contacts.remove(contact)) {
            ContactsDBHelper.deleteContactInDB(contact.id);
            notifyListenersAsync(DELETION, contact);
            removeFavorite(contact);
            ContactGroupsDataStore.handleContactDeletion(contact);
        }
    }

    public static void removeContact(long contactId) {
        removeContact(getContactWithId(contactId));
    }

    public static void updateContact(long contactId, String primaryNumber, VCard vCard, Context context) {
        ContactsDBHelper.updateContactInDBWith(contactId, primaryNumber, vCard, context);
        reloadContact(contactId);
        ContactGroupsDataStore.handleContactUpdate(getContactWithId(contactId));
        CallLogDataStore.updateCallLogAsyncForNewContact(getContactWithId(contactId));
    }

    private static void reloadContact(long contactId) {
        int indexOfContact = contacts.indexOf(createDummyContact(contactId));
        if (indexOfContact == -1)
            return;
        Contact contactFromDB = ContactsDBHelper.getContact(contactId);
        contacts.set(indexOfContact, contactFromDB);
        notifyListenersAsync(UPDATION, contactFromDB);
    }

    public static void addDataChangeListener(DataStoreChangeListener<Contact> changeListener) {
        synchronized (dataChangeListeners){
            dataChangeListeners.add(changeListener);
        }
    }

    public static void removeDataChangeListener(DataStoreChangeListener<Contact> changeListener) {
        processAsync(() -> { //for those listeners who want to deregister inside the registered listener callback
            synchronized (dataChangeListeners){
                dataChangeListeners.remove(changeListener);
            }
        });
    }

    public static opencontacts.open.com.opencontacts.orm.Contact getContact(String phoneNumber) {
        return ContactsDBHelper.getContactFromDB(phoneNumber);
    }

    public static Contact cautiouslyGetContactFromDatabase(long contactId) throws Exception{
        return U.checkNotNull(ContactsDBHelper.getContact(contactId));
    }

    public static Contact getContactWithId(long contactId) {
        if (contactId == -1 || contacts == null)
            return null;
        int indexOfContact = contacts.indexOf(createDummyContact(contactId));
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
        if (currentState == LOADED) currentState = REFRESHING;
        contacts = ContactsDBHelper.getAllContactsFromDB();
        currentState = LOADED;
        ContactGroupsDataStore.initInCaseHasNot();
        updateFavoritesList();
        notifyListeners(REFRESH, null);
    }

    private static void notifyListenersAsync(final int type, final Contact contact){
        if(dataChangeListeners.isEmpty() || pauseUpdates)
            return;
        processAsync(() -> notifyListeners(type, contact));
    }

    private static void notifyListeners(int type, Contact contact) {
        if(dataChangeListeners.isEmpty() || pauseUpdates)
            return;
        synchronized (dataChangeListeners){
            U.forEach(dataChangeListeners, listener -> {
                if(listener == null) return;
                if(type == ADDITION)
                    listener.onAdd(contact);
                else if(type == DELETION)
                    listener.onRemove(contact);
                else if(type == UPDATION)
                    listener.onUpdate(contact);
                else if (type == REFRESH)
                    listener.onStoreRefreshed();
            });
        }
    }

    public static void deleteAllContacts(Context context) {
        processAsync(() -> {
            ContactsDBHelper.deleteAllContactsAndRelatedStuff();
            refreshStore();
            computeGroupsAsync();
            toastFromNonUIThread(R.string.deleted_all_contacts, Toast.LENGTH_LONG, context);
        });
    }

    public static VCardData getVCardData(long contactId){
        return ContactsDBHelper.getVCard(contactId);
    }

    public static void init() {
        refreshStoreAsync();
    }

    public static void updateFavoritesList(){
        favorites = U.chain(Favorite.listAll(Favorite.class))
                .map(favoriteFromDB -> favoriteFromDB.contact.getId())
                .map(ContactsDataStore::getContactWithId)
                .filterFalse(U::isNull)
                .value();
    }

    public static List<Contact> getFavorites(){
        if(favorites.size() != 0 || Favorite.count(Favorite.class) == 0)
            return favorites;
        updateFavoritesList();
        return favorites;
    }

    public static void addFavorite(Contact contact) {
        if(getFavorites().contains(contact)) return;
        new Favorite(ContactsDBHelper.getDBContactWithId(contact.id)).save();
        favorites.add(contact);
        markAsFavoriteInVCard(contact.id);
        notifyListenersAsync(REFRESH, null);
    }

    private static void markAsFavoriteInVCard(long contactId) {
        try {
            markFavoriteInVCard(true, ContactsDBHelper.getVCard(contactId).vcardDataAsString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addFavorite(opencontacts.open.com.opencontacts.orm.Contact contact) {
        Contact dummyContactMatchingId = createDummyContact(contact.getId());
        if(getFavorites().contains(dummyContactMatchingId)) return;
        new Favorite(ContactsDBHelper.getDBContactWithId(dummyContactMatchingId.id)).save();
        favorites.add(ContactsDBHelper.getContact(dummyContactMatchingId.id));
        markAsFavoriteInVCard(dummyContactMatchingId.id);
    }

    public static void removeFavorite(Contact contact) {
        List<Favorite> favoriteContactQueryResults = Favorite.find(Favorite.class, "contact = ?", contact.id + "");
        Favorite.deleteInTx(favoriteContactQueryResults);
        favorites.remove(contact);
        notifyListenersAsync(REFRESH, null);
    }

    public static boolean isFavorite(Contact contact){
        return getFavorites().contains(contact);
    }

    public static void mergeContacts(Contact primaryContact, Contact secondaryContact, Context context) throws IOException {
        VCardData primaryVCardData = VCardData.getVCardData(primaryContact.id);
        VCardData secondaryVCardData = VCardData.getVCardData(secondaryContact.id);
        VCard mergedVCard = mergeVCardStrings(primaryVCardData.vcardDataAsString, secondaryVCardData.vcardDataAsString, context);
        removeContact(secondaryContact);
        updateContact(primaryContact.id, primaryContact.primaryPhoneNumber.phoneNumber, mergedVCard, context);
    }

    public static void mergeContacts(List<Contact> contactsToMerge, Context context){
        if (contactsToMerge == null || contactsToMerge.size() < 2) return;
        final Contact firstContact = U.first(contactsToMerge);
        U.chain(contactsToMerge)
                .rest()
                .forEach(contactToMerge -> {
                    try {
                        mergeContacts(firstContact, contactToMerge, context);
                    } catch (IOException e) {
                        e.printStackTrace();
                        AndroidUtils.toastFromNonUIThread(R.string.failed_merging_a_contact, Toast.LENGTH_SHORT, context);
                        // this happened coz of not being able to read contact data from vcard table. So, its fine if its not merged finally
                    }
                });
    }

    public static List<Contact> getContactsMatchingT9(String t9Text) {
        return DomainUtils.filterContactsBasedOnT9Text(t9Text, contacts);
    }
}