package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.ListView;

import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter.ContactsListActionsListener;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

/**
 * Created by sultanm on 3/25/17.
 */

public class ContactsListView extends ListView implements DataStoreChangeListener<Contact>, ContactsListActionsListener {
    private List <Contact> contacts;
    private Context context;
    private ContactsListViewAdapter adapter;
    private final AppCompatTextView totalContactsTextView;


    public ContactsListView(final Context context) {
        super(context);
        this.context = context;
        setTextFilterEnabled(true);
        ContactsDataStore.addDataChangeListener(this);
        contacts = ContactsDataStore.getAllContacts();
        sortContacts();
        adapter = new ContactsListViewAdapter(context, R.layout.contact, () -> contacts);
        adapter.setContactsListActionsListener(ContactsListView.this);
        setAdapter(adapter);
        View headerView = inflate(context, R.layout.contacts_list_header, null);
        addHeaderView(headerView);
        totalContactsTextView = (AppCompatTextView) headerView.findViewById(R.id.total_contacts);
        updateHeaderWithContactsCount();
    }

    private void updateHeaderWithContactsCount() {
        totalContactsTextView.setText(String.valueOf(contacts.size()));
    }

    private void sortContacts() {
        Collections.sort(contacts,
                (contact1, contact2) -> contact1.name.compareToIgnoreCase(contact2.name));
    }

    private void addContactsToAdapter() {
        adapter.clear();
        adapter.addAll(contacts);
        adapter.notifyDataSetChanged();
        if (isInFilterMode())
            setFilterText(getTextFilter().toString());
    }

    @Override
    public void onUpdate(final Contact contact) {
        this.post(() -> {
            contacts.remove(contact);
            contacts.add(contact);

            adapter.remove(contact);
            adapter.add(contact);
            adapter.notifyDataSetChanged();
            updateHeaderWithContactsCount();
            adapter.contactsListFilter.updateMap(contact);
        });

    }

    @Override
    public void onRemove(final Contact contact) {
        this.post(() -> {
            contacts.remove(contact);
            adapter.remove(contact);
            adapter.notifyDataSetChanged();
            updateHeaderWithContactsCount();
        });
    }

    @Override
    public void onAdd(final Contact contact) {
        this.post(() -> {
            contacts.add(contact);
            adapter.add(contact);
            adapter.notifyDataSetChanged();
            updateHeaderWithContactsCount();
        });
    }

    @Override
    public void onStoreRefreshed() {
        contacts = ContactsDataStore.getAllContacts();
        sortContacts();
        post(() -> {
            addContactsToAdapter();
            updateHeaderWithContactsCount();
            adapter.contactsListFilter.mapAsync(contacts);
        });
    }

    public void onDestroy(){
        ContactsDataStore.removeDataChangeListener(this);
    }

    @Override
    public void onCallClicked(Contact contact) {
        AndroidUtils.call(contact.primaryPhoneNumber, context);
    }

    @Override
    public void onMessageClicked(Contact contact) {
        AndroidUtils.message(contact.primaryPhoneNumber, context);
    }

    @Override
    public void onShowDetails(Contact contact) {
        context.startActivity(AndroidUtils.getIntentToShowContactDetails(contact.id, context));
    }

    @Override
    public void onWhatsappClicked(Contact contact) {
        AndroidUtils.whatsapp(contact.primaryPhoneNumber, context);
    }
}
