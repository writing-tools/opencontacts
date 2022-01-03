package opencontacts.open.com.opencontacts;

import static android.text.TextUtils.isEmpty;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getASpaceOfHeight;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getContactComparatorBasedOnName;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.sortContactsBasedOnName;

import android.content.Context;
import androidx.appcompat.widget.AppCompatTextView;
import android.view.View;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;

import com.github.underscore.Supplier;
import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.actions.DefaultContactsListActions;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.DataStoreChangeListener;

/**
 * Created by sultanm on 3/25/17.
 */

public class ContactsListView extends ListView implements DataStoreChangeListener<Contact> {
    private List<Contact> contacts;
    private Context context;
    private Supplier<String> searchStringSupplier;
    private ContactsListViewAdapter adapter;
    private final AppCompatTextView totalContactsTextView;


    public ContactsListView(final Context context, Supplier<String> searchStringSupplier) {
        super(context);
        this.context = context;
        this.setFastScrollEnabled(true);
        this.searchStringSupplier = searchStringSupplier;
        setTextFilterEnabled(false);
        ContactsDataStore.addDataChangeListener(this);
        contacts = new ArrayList<>();
        adapter = new ContactsListViewAdapter(context, R.layout.contact, () -> contacts);
        adapter.setContactsListActionsListener(new DefaultContactsListActions(context));
        View headerView = inflate(context, R.layout.contacts_list_header, null);
        addHeaderView(headerView);
        addFooterView(getASpaceOfHeight(10, 56, context)); //56 is height of bottom menu, 10 is arbitrary
        setAdapter(adapter);
        totalContactsTextView = headerView.findViewById(R.id.total_contacts);
        updateHeaderWithContactsCount();
        onStoreRefreshed();
    }

    private void updateHeaderWithContactsCount() {
        totalContactsTextView.setText(String.valueOf(contacts.size()));
    }

    private void addContactsToAdapter() {
        adapter.clear();
        adapter.addAll(contacts);
        adapter.notifyDataSetChanged();
        String searchText = searchStringSupplier.get();
        if (isEmpty(searchText)) return;
        filter(searchText);
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
        contacts = sortContactsBasedOnName(ContactsDataStore.getAllContacts(), context);
        moveFavoritesToTop();
        post(() -> {
            addContactsToAdapter();
            updateHeaderWithContactsCount();
            adapter.contactsListFilter.mapAsync(contacts);
        });
    }

    private void moveFavoritesToTop() {
        List<Contact> favorites = ContactsDataStore.getFavorites();
        Collections.sort(favorites, getContactComparatorBasedOnName(context));
        U.forEach(favorites, contacts::remove);
        contacts.addAll(0, favorites);
    }

    public void onDestroy() {
        ContactsDataStore.removeDataChangeListener(this);
    }

    public void filter(CharSequence filterText) {
        ((HeaderViewListAdapter) getAdapter()).getFilter().filter(filterText);
    }
}
