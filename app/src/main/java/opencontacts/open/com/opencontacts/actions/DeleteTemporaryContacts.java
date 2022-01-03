package opencontacts.open.com.opencontacts.actions;

import static java.util.Calendar.DAY_OF_MONTH;
import static opencontacts.open.com.opencontacts.utils.Common.hasItBeen;

import android.content.Context;

import com.github.underscore.U;

import java.util.List;

import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;

public class DeleteTemporaryContacts implements ContactsHouseKeepingAction{

    @Override
    public void perform(List<Contact> contacts, Context context) {
        U.forEach(ContactsDataStore.getTemporaryContactDetails(),
            tempContactDetails -> {
                if(hasItBeen(30, DAY_OF_MONTH, tempContactDetails.markedTemporaryOn.getTime()))
                ContactsDataStore.removeContact(tempContactDetails.contact.getId());
            });
    }
}
