package opencontacts.open.com.opencontacts.domain;

import java.util.HashSet;

import opencontacts.open.com.opencontacts.utils.DomainUtils;

public class ContactGroup {
    public HashSet<Contact> contacts = new HashSet<>(0);
    private String name;
    public String t9Name;

    public ContactGroup(String name) {
        updateName(name);
    }

    public void updateName(String name) {
        this.name = name;
        this.t9Name = DomainUtils.getNumericKeyPadNumberForString(name);
    }

    public ContactGroup addContact(Contact contact) {
        contacts.add(contact);
        return this;
    }

    public ContactGroup removeContact(Contact contact) {
        contacts.remove(contact);
        return this;
    }

    public String getName() {
        return name; // added this to make sure no one updates the name directly and leaves behind T9 text
    }

    @Override
    public String toString() {
        return name;
    }
}
