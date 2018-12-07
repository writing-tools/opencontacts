package opencontacts.open.com.opencontacts;

import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;

public class OpenContactsApplication extends com.orm.SugarApp{
    @Override
    public void onCreate() {
        super.onCreate();
        ContactsDataStore.init();
        CallLogDataStore.init(getApplicationContext());
    }
}
