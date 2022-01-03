package opencontacts.open.com.opencontacts.utils;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.mergeContacts;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.requestPauseOnUpdates;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.requestResumeUpdates;

import android.content.Context;
import android.widget.Toast;

import com.github.underscore.lodash.U;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;

public class ContactsAutoMergeUtils {

    public static int merged = 0;

    public static void autoMergeByName(Context context) {
        List<Contact> allContacts = ContactsDataStore.getAllContacts();
        Collections.sort(allContacts, (contact1, contact2) -> contact1.name.compareTo(contact2.name));
        List<Contact> nonEmptyNameContacts = U.reject(allContacts, contact -> "".equals(contact.name));
        merged = 0;
        try {
            requestPauseOnUpdates();
            Common.forEachIndex(nonEmptyNameContacts.size() - 1, (index) -> {
                Contact primaryContact = nonEmptyNameContacts.get(index + 1);
                Contact currentContact = nonEmptyNameContacts.get(index);
                if (currentContact.name.equalsIgnoreCase(primaryContact.name)) {
                    try {
                        mergeContacts(primaryContact, currentContact, context);
                        merged++;
                    } catch (IOException e) {
                        e.printStackTrace();
                        // this happened coz of not being able to read contact data from vcard table. So, its fine if its not merged finally
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.unexpected_error_happened, Toast.LENGTH_SHORT, context);
        } finally {
            requestResumeUpdates();
            AndroidUtils.toastFromNonUIThread(context.getString(R.string.auto_merge_complete, merged), Toast.LENGTH_SHORT, context);
        }


    }

}
