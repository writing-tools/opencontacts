package opencontacts.open.com.opencontacts.actions;

import android.content.Context;

import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;

public interface ContactsHouseKeepingAction {
    void perform(List<Contact> contacts, Context context);
}
