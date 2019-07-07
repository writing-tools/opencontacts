package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter.ContactsListActionsListener;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.addFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.isFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeFavorite;

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
        setTextFilterEnabled(false);
        ContactsDataStore.addDataChangeListener(this);
        contacts = new ArrayList<>();
        adapter = new ContactsListViewAdapter(context, R.layout.contact, () -> contacts);
        adapter.setContactsListActionsListener(ContactsListView.this);
        View headerView = inflate(context, R.layout.contacts_list_header, null);
        addHeaderView(headerView);
        setAdapter(adapter);
        totalContactsTextView = headerView.findViewById(R.id.total_contacts);
        updateHeaderWithContactsCount();
        onStoreRefreshed();
    }

    private void updateHeaderWithContactsCount() {
        totalContactsTextView.setText(String.valueOf(contacts.size()));
    }

    private void sortContacts() {
        Collections.sort(contacts,
                getContactComparator());
    }

    @NonNull
    private Comparator<Contact> getContactComparator() {
        return (contact1, contact2) -> contact1.name.compareToIgnoreCase(contact2.name);
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
        moveFavoritesToTop();
        post(() -> {
            addContactsToAdapter();
            updateHeaderWithContactsCount();
            adapter.contactsListFilter.mapAsync(contacts);
        });
    }

    private void moveFavoritesToTop() {
        List<Contact> favorites = ContactsDataStore.getFavorites();
        Collections.sort(favorites, getContactComparator());
        U.forEach(favorites, contacts::remove);
        contacts.addAll(0, favorites);
    }

    public void onDestroy(){
        ContactsDataStore.removeDataChangeListener(this);
    }

    @Override
    public void onCallClicked(Contact contact) {
        AndroidUtils.call(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onMessageClicked(Contact contact) {
        AndroidUtils.message(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onShowDetails(Contact contact) {
        context.startActivity(AndroidUtils.getIntentToShowContactDetails(contact.id, context));
    }

    @Override
    public void onWhatsappClicked(Contact contact) {
        AndroidUtils.whatsapp(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onLongClick(Contact contact) {
        int favoritesResource = isFavorite(contact) ? R.string.remove_favorite : R.string.add_to_favorites;
        new AlertDialog.Builder(context)
                .setItems(new String[]{context.getString(favoritesResource)}, (dialog, which) -> {
                    switch(which){
                        case 0:
                            if (favoritesResource == R.string.add_to_favorites) addFavorite(contact);
                            else removeFavorite(contact);
                    }
                }).show();
    }

    public void filter(CharSequence filterText){
        ((HeaderViewListAdapter)getAdapter()).getFilter().filter(filterText);
    }
}
