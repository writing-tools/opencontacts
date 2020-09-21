package opencontacts.open.com.opencontacts.domain;

import java.util.HashSet;

public class ContactGroup {
    public HashSet<Contact> contacts = new HashSet<>(0);
    public String name;

    public ContactGroup(String name) {
        this.name = name;
    }
    public ContactGroup addContact(Contact contact){
        contacts.add(contact);
        return this;
    }

    public ContactGroup removeContact(Contact contact){
        contacts.remove(contact);
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
