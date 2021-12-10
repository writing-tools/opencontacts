package opencontacts.open.com.opencontacts;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

public class ContactsListT9Filter extends ContactsListFilter {
    public ContactsListT9Filter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder) {
        super(adapter, allContactsHolder);
    }

    public void updateMap(Contact contact) {
        contact.setT9Text();
    }

    public void createDataMapping(List<Contact> contacts) {
        List<Contact> threadSafeContacts = new ArrayList<>(contacts);
        for (Contact contact : threadSafeContacts) {
            contact.setT9Text();
        }
    }

    public List<Contact> filter(CharSequence t9Text, List<Contact> contacts) {
        return DomainUtils.filterContactsBasedOnT9Text(t9Text, contacts);
    }
}
