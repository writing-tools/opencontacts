package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getAllNumericPhoneNumber;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getSearchablePhoneNumber;

public class CallLogDataStore {
    private static CallLogDBHelper callLogDBHelper = new CallLogDBHelper();
    private static List<CallLogEntry> callLogEntries = new ArrayList<>(0);
    private static List<DataStoreChangeListener<CallLogEntry>> dataChangeListeners = new ArrayList<>(3);

    public static synchronized void loadRecentCallLogEntriesAsync(Context context) {
        processAsync(() -> loadRecentCallLogEntries(context));
    }

    public static synchronized void loadRecentCallLogEntries(Context context) {
        final List<CallLogEntry> recentCallLogEntries = callLogDBHelper.loadRecentCallLogEntriesIntoDB(context);
        if(recentCallLogEntries.isEmpty())
            return;
        ContactsDataStore.updateContactsAccessedDateAsync(recentCallLogEntries);
        addRecentCallLogEntriesToStore(recentCallLogEntries);
    }

    private static void addRecentCallLogEntriesToStore(final List<CallLogEntry> recentCallLogEntries) {
        if(recentCallLogEntries.size() > 1){
            refreshStore();
        }
        else if(recentCallLogEntries.size() == 1){
            CallLogEntry callLogEntry = recentCallLogEntries.get(0);
            callLogEntries.add(0, callLogEntry);
            processAsync(() -> {
                for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                    dataStoreChangeListener.onAdd(callLogEntry);
                }
            });
        }
    }

    private synchronized static void refreshStore() {
        callLogEntries = CallLogDBHelper.getRecent100CallLogEntriesFromDB();
        if(!callLogEntries.isEmpty())
            for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                dataStoreChangeListener.onStoreRefreshed();
            }
    }

    public static CallLogEntry getMostRecentCallLogEntry(Context context) {
        loadRecentCallLogEntries(context);
        return callLogEntries.isEmpty() ? null : callLogEntries.get(0);
    }

    public static List<CallLogEntry> getRecent100CallLogEntries(Context context){
        if(callLogEntries.isEmpty()){
            processAsync(CallLogDataStore::refreshStore);
            return new ArrayList<>(0);
        }
        return new ArrayList<>(callLogEntries);
    }

    public static void addDataChangeListener(DataStoreChangeListener<CallLogEntry> changeListener) {
        dataChangeListeners.add(changeListener);
    }

    public static void removeDataChangeListener(DataStoreChangeListener<CallLogEntry> changeListener) {
        dataChangeListeners.remove(changeListener);
    }

    public static void updateCallLogAsyncForNewContact(final Contact newContact, final Context context){
        processAsync(new Runnable() {
            @Override
            public void run() {
                List<CallLogEntry> callLogEntriesToWorkWith = getCallLogEntriesToWorkWith();
                if(callLogEntriesToWorkWith.isEmpty())
                    return;
                int numberOfEntriesUpdated = 0;
                for(String phoneNumber : newContact.phoneNumbers) {
                    String searchablePhoneNumber = getSearchablePhoneNumber(phoneNumber);
                    if (searchablePhoneNumber == null)
                        continue;
                    for (CallLogEntry callLogEntry : callLogEntriesToWorkWith) {
                        if (callLogEntry.getContactId() != -1)
                            continue;
                        String allNumericPhoneNumberOfCallLogEntry = getAllNumericPhoneNumber(callLogEntry.getPhoneNumber());
                        if(allNumericPhoneNumberOfCallLogEntry.contains(searchablePhoneNumber)){
                            callLogEntry.setContactId(newContact.id);
                            callLogEntry.setName(newContact.name);
                            callLogEntry.save();
                            numberOfEntriesUpdated ++;
                            break;
                        }
                    }
                }
                if(numberOfEntriesUpdated == 0)
                    return;
                for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                    dataStoreChangeListener.onStoreRefreshed();
                }
            }

            private List<CallLogEntry> getCallLogEntriesToWorkWith() {
                return callLogEntries.isEmpty() ? CallLogDBHelper.getRecent100CallLogEntriesFromDB() : callLogEntries;
            }
        });
    }

    public static void updateCallLogAsyncForAllContacts(final Context context){
        processAsync(() -> updateCallLogForAllContacts(context));
    }

    public static void updateCallLogForAllContacts(Context context) {
        if(callLogEntries == null)
            callLogEntries = getRecent100CallLogEntries(context);
        int numberOfEntriesUpdated = 0;
        for(CallLogEntry callLogEntry : callLogEntries){
            if(callLogEntry.getContactId() != -1)
                continue;
            opencontacts.open.com.opencontacts.orm.Contact contactFromDB = ContactsDBHelper.getContactFromDB(callLogEntry.getPhoneNumber());
            if(contactFromDB == null)
                continue;
            callLogEntry.setName(contactFromDB.firstName + " " + contactFromDB.lastName);
            callLogEntry.setContactId(contactFromDB.getId());
            callLogEntry.save();
            numberOfEntriesUpdated ++;
        }
        if(numberOfEntriesUpdated == 0)
            return;
        for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
            dataStoreChangeListener.onStoreRefreshed();
        }
    }

    public static void delete(Long id) {
        boolean hasBeenDeleted = CallLogDBHelper.delete(id);
        if(!hasBeenDeleted)
            return;
        for(CallLogEntry callLogEntryToBeRemoved : callLogEntries){
            if(!callLogEntryToBeRemoved.getId().equals(id))
                continue;
            callLogEntries.remove(callLogEntryToBeRemoved);
            processAsync(() -> {
                for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                    dataStoreChangeListener.onRemove(callLogEntryToBeRemoved);
                }
            });
            break;
        }
    }
}