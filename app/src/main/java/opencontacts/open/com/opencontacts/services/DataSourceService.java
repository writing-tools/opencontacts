package opencontacts.open.com.opencontacts.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.github.underscore.U;
import com.opencsv.CSVWriterBuilder;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsDataStoreService;
import opencontacts.open.com.opencontacts.activities.AboutActivity;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.utils.AIDLTranslationUtils;

public class DataSourceService extends Service {
    ContactsDataStoreService.Stub service = new ContactsDataStoreService.Stub() {
        @Override
        public List<String> getAllVCards() throws RemoteException {
//            TODO: implement later
            return new ArrayList<>();
        }

        @Override
        public String getNameAndPhoneNumbers() throws RemoteException {
            List<String[]> contactsAsCSV = U.map(ContactsDataStore.getAllContacts(), AIDLTranslationUtils::contactToCSV);
            StringWriter stringWriter = new StringWriter();
            startActivity(new Intent(getBaseContext(), AboutActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            new CSVWriterBuilder(stringWriter)
                .build()
                .writeAll(contactsAsCSV);
            return stringWriter.toString();
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return service;
    }
}
