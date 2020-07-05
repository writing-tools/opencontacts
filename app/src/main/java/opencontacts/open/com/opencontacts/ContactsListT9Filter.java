package opencontacts.open.com.opencontacts;

import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;

public class ContactsListT9Filter extends ContactsListFilter{
    public ContactsListT9Filter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder) {
        super(adapter, allContactsHolder);
    }

    public void updateMap(Contact contact) {
        contact.setT9Text();
    }

    public void createDataMapping(List<Contact> contacts) {
        List<Contact> threadSafeContacts = new ArrayList<>(contacts);
        for(Contact contact : threadSafeContacts){
            contact.setT9Text();
        }
    }

    public List<Contact> filter(CharSequence searchText, List<Contact> contacts) {
        ArrayList<Contact> filteredContacts = new ArrayList<>();
        for (Contact contact : contacts) {
            if(contact.t9Text == null){
                contact.setT9Text();
            }
            if (contact.t9Text.contains(searchText.toString().toUpperCase())) {
                filteredContacts.add(contact);
            }
        }
        return filteredContacts;
    }
}
