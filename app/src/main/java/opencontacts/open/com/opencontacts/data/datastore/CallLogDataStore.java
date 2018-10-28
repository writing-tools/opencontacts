package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

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
}