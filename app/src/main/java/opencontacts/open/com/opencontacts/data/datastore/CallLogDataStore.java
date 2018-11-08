package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;

public class CallLogDataStore {
    private static CallLogDBHelper callLogDBHelper = new CallLogDBHelper();
    private static List<CallLogEntry> callLogEntries = new ArrayList<>(1);
    private static List<DataStoreChangeListener<CallLogEntry>> dataChangeListeners = new ArrayList<>(3);

    public static synchronized void loadRecentCallLogEntries(Context context) {
        final List<CallLogEntry> recentCallLogEntries = callLogDBHelper.loadRecentCallLogEntriesIntoDB(context);
        if(recentCallLogEntries.size() == 0)
            return;
        ContactsDataStore.updateContactsAccessedDateAsync(recentCallLogEntries);
        addRecentCallLogEntriesToStore(recentCallLogEntries);
    }

    private static void addRecentCallLogEntriesToStore(final List<CallLogEntry> recentCallLogEntries) {
        new Thread() {
            @Override
            public void run() {
                if(recentCallLogEntries.size() > 1){
                    refreshStore();
                }
                else if(recentCallLogEntries.size() == 1 && callLogEntries.size() > 0){
                    CallLogEntry callLogEntry = recentCallLogEntries.get(0);
                    callLogEntries.add(0, callLogEntry);
                    for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                        dataStoreChangeListener.onAdd(callLogEntry);
                    }
                }
            }
        }.start();
    }

    private synchronized static void refreshStore() {
        if(callLogEntries.size() == 0)
            return;
        callLogEntries = CallLogDBHelper.getRecent100CallLogEntriesFromDB();
        for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
            dataStoreChangeListener.onStoreRefreshed();
        }
    }

    public static CallLogEntry getMostRecentCallLogEntry(Context context) {
        if (callLogEntries.size() > 0)
            callLogEntries.get(0);
        callLogEntries = CallLogDBHelper.getRecent100CallLogEntriesFromDB();
        if (callLogEntries.size() > 0)
            return callLogEntries.get(0);
        List<CallLogEntry> callLogEntriesFromDB = callLogDBHelper.loadRecentCallLogEntriesIntoDB(context);
        if (callLogEntriesFromDB.size() > 0) {
            return callLogEntriesFromDB.get(0);
        }
        return null;
    }
    public static List<CallLogEntry> getRecent100CallLogEntries(Context context){
        if(callLogEntries.size() > 0)
            new ArrayList<>(callLogEntries);
        callLogEntries = CallLogDBHelper.getRecent100CallLogEntriesFromDB();
        if(callLogEntries.size() > 0)
            return new ArrayList<>(callLogEntries);
        loadRecentCallLogEntries(context);
        return new ArrayList<>(0);
    }

    public static void addDataChangeListener(DataStoreChangeListener<CallLogEntry> changeListener) {
        dataChangeListeners.add(changeListener);
    }

    public static void removeDataChangeListener(DataStoreChangeListener<CallLogEntry> changeListener) {
        dataChangeListeners.remove(changeListener);
    }

    public static void updateCallLogAsyncForNewContact(final Contact newContact, final Context context){
        new Thread(){
            @Override
            public void run() {
                if(callLogEntries == null)
                    callLogEntries = getRecent100CallLogEntries(context);
                int numberOfEntriesUpdated = 0;
                for(CallLogEntry callLogEntry : callLogEntries){
                    if(callLogEntry.getContactId() != -1)
                        continue;
                    for(String phoneNumber : newContact.phoneNumbers){
                        if(phoneNumber.equals(callLogEntry.getPhoneNumber())){
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
        }.start();
    }

    public static void updateCallLogAsyncForAllContacts(final Context context){
        new Thread(){
            @Override
            public void run() {
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
                    numberOfEntriesUpdated ++;
                }
                if(numberOfEntriesUpdated == 0)
                    return;
                for(DataStoreChangeListener<CallLogEntry> dataStoreChangeListener: dataChangeListeners){
                    dataStoreChangeListener.onStoreRefreshed();
                }

            }
        }.start();
    }
}