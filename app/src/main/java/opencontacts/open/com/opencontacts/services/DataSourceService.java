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

import open.com.opencontactsdatasourcecontract.ContactsDataStoreService;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
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
            System.out.println("called service yolo");
            List<Contact> contacts = null;
            try {
                contacts = ContactsDataStore.getAllContactsSync();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("Error occured loading contacts");
            }
            List<String[]> contactsAsCSV = U.map(contacts, AIDLTranslationUtils::contactToCSV);
            StringWriter stringWriter = new StringWriter();
            new CSVWriterBuilder(stringWriter)
                .build()
                .writeAll(contactsAsCSV);
            return stringWriter.toString();
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("on bind called yolo");
        return service;
    }
}
