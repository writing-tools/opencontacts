package opencontacts.open.com.opencontacts.activities;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getAllContacts;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeDataChangeListener;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.sortContactsBasedOnName;
import static opencontacts.open.com.opencontacts.utils.PrimitiveDataTypeUtils.toPrimitiveLongs;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.github.underscore.lodash.U;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import opencontacts.open.com.opencontacts.ContactsListFilter;
import opencontacts.open.com.opencontacts.ContactsListT9Filter;
import opencontacts.open.com.opencontacts.ContactsListTextFilter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SampleDataStoreChangeListener;
import opencontacts.open.com.opencontacts.utils.Common;

public abstract class ContactChooserActivityBase extends AppBaseActivity {
    protected ListView contactsListView;
    private ArrayAdapter<Contact> adapter;
    private SearchView searchView;
    private List<Contact> contacts;
    private SampleDataStoreChangeListener<Contact> contactsDataChangeListener;
    private Set<Contact> selectedContactsSet = new HashSet<>(0);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsListView = new ListView(this) {
            @Override
            public void setItemChecked(int position, boolean isChecked) {
                Contact contact = (Contact) getItemAtPosition(position);
                if (isChecked) selectedContactsSet.add(contact);
                else selectedContactsSet.remove(contact);
                super.setItemChecked(position, isChecked);
            }

            @Override
            public int getCheckedItemCount() {
                return selectedContactsSet.size();
            }

            @Override
            public long[] getCheckedItemIds() {
                return toPrimitiveLongs(
                    U.map(new ArrayList<>(selectedContactsSet), contact -> contact.id)
                );
            }

            @Override
            public SparseBooleanArray getCheckedItemPositions() {
                SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
                Common.forEachIndex(getCount(), index -> sparseBooleanArray.put(index, isItemChecked(index)));
                return sparseBooleanArray;
            }

            @Override
            public boolean isItemChecked(int position) {
                return selectedContactsSet.contains(getItemAtPosition(position));
            }
        };
        contactsListView.setTextFilterEnabled(false);
        if (shouldEnableMultiSelect())
            contactsListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        else
            contactsListView.setOnItemClickListener((parent, view, position, id) -> onContactSelect(adapter.getItem(position)));
        contacts = sortContactsBasedOnName(getAllContacts(), this);
        adapter = new ArrayAdapter<Contact>(this, shouldEnableMultiSelect() ?
            R.layout.layout_simple_multi_select_contact : R.layout.layout_simple_contact_select
            , R.id.contact_name, new ArrayList<>(contacts)) {
            @NonNull
            @Override
            public Filter getFilter() {
                return isT9SearchEnabled(getContext()) ?
                    new ContactsListT9Filter(this, () -> contacts)
                    : new ContactsListTextFilter(this, () -> contacts);
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public long getItemId(int position) {
                try {
                    return getItem(position).id;
                } catch (Exception e) {
                    return -1; // happening as we add and remove entities.
                }
            }

            public void onItemSelect(View v) {
                Contact contact = (Contact) v.getTag();
                if (selectedContactsSet.contains(contact)) selectedContactsSet.remove(contact);
                else selectedContactsSet.add(contact);
                adapter.notifyDataSetChanged();
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                convertView = super.getView(position, convertView, parent);
                AppCompatCheckedTextView checkedTextView = convertView.findViewById(R.id.contact_name);
                Contact contact = getItem(position);
                checkedTextView.setText(contact.name);
                checkedTextView.setTag(contact);
                if (shouldEnableMultiSelect()) {
                    checkedTextView.setOnClickListener(this::onItemSelect);
                    checkedTextView.setChecked(selectedContactsSet.contains(contact));
                }
                return convertView;
            }
        };
        adapter.setNotifyOnChange(true);
        contactsListView.setAdapter(adapter);
        ((RelativeLayout) findViewById(R.id.contacts_list)).addView(contactsListView);

        ((Toolbar) findViewById(R.id.toolbar)).setTitle(getTitleResource());

        contactsDataChangeListener = new SampleDataStoreChangeListener<Contact>() {
            @Override
            public void onUpdate(Contact contact) {
                contactsListView.post(() -> {
                    adapter.remove(contact);
                    adapter.add(contact);
                    contacts.remove(contact);
                    contacts.add(contact);
                    ((ContactsListFilter) (adapter.getFilter())).updateMap(contact);
                    setFilterInCaseExisting();
                });
            }

            @Override
            public void onRemove(Contact contact) {
                contactsListView.post(() -> {
                    adapter.remove(contact);
                    contacts.remove(contact);
                    ((ContactsListFilter) (adapter.getFilter())).updateMap(contact);
                    setFilterInCaseExisting();
                });
            }

            @Override
            public void onAdd(Contact contact) {
                contactsListView.post(() -> {
                    adapter.add(contact);
                    setFilterInCaseExisting();
                });
            }

            @Override
            public void onStoreRefreshed() {
                contactsListView.post(() -> {
                    contacts = getAllContacts();
                    adapter.clear();
                    adapter.addAll(contacts);
                    ((ContactsListFilter) (adapter.getFilter())).mapAsync(contacts);
                    setFilterInCaseExisting();
                });
            }
        };
        ContactsDataStore.addDataChangeListener(contactsDataChangeListener);
    }

    private void setFilterInCaseExisting() {
        CharSequence query = searchView.getQuery();
        if (TextUtils.isEmpty(query)) return;
        adapter.getFilter().filter(query);
    }

    public int getTitleResource() {
        return R.string.choose_a_contact;
    }

    public abstract void onContactSelect(Contact selectedContact);

    public boolean shouldEnableMultiSelect() {
        return false;
    }

    public ListView getContactsListView() {
        return contactsListView;
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_base_contact_chooser;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        searchView = new SearchView(this);
        bindSearchViewToContacts(searchView);
        menu.add(R.string.search)
            .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            .setActionView(searchView);
        return super.onCreateOptionsMenu(menu);
    }

    private void bindSearchViewToContacts(SearchView searchView) {
        searchView.setInputType(isT9SearchEnabled(this) ? InputType.TYPE_CLASS_PHONE : InputType.TYPE_CLASS_TEXT);
        searchView.setOnCloseListener(() -> {
            contactsListView.clearTextFilter();
            return false;
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                ((ArrayAdapter) contactsListView.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDataChangeListener(contactsDataChangeListener);
    }

    @NonNull
    protected List<Contact> getSelectedContacts() {
        return new ArrayList<>(selectedContactsSet);
    }

    protected void setSelectedContacts(Collection<Contact> contactsToBeSelected) {
        Common.forEachIndex(contactsListView.getCount(), index -> {
            if (contactsToBeSelected.contains(contactsListView.getItemAtPosition(index)))
                contactsListView.setItemChecked(index, true);
        });

    }
}
