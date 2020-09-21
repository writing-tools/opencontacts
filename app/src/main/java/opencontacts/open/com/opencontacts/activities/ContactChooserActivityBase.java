package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListFilter;
import opencontacts.open.com.opencontacts.ContactsListT9Filter;
import opencontacts.open.com.opencontacts.ContactsListTextFilter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.interfaces.SampleDataStoreChangeListener;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getAllContacts;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeDataChangeListener;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.sortContacts;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;

public abstract class ContactChooserActivityBase extends AppBaseActivity {
    protected ListView contactsListView;
    private ArrayAdapter<Contact> adapter;
    private SearchView searchView;
    private List<Contact> contacts;
    private SampleDataStoreChangeListener<Contact> contactsDataChangeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsListView = new ListView(this);
        contactsListView.setTextFilterEnabled(false);
        if(shouldEnableMultiSelect()) contactsListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        else
            contactsListView.setOnItemClickListener((parent, view, position, id) -> onContactSelect(adapter.getItem(position)));
        contacts = sortContacts(getAllContacts(), this);
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
                try{
                    return getItem(position).id;
                }
                catch (Exception e){
                    return -1; // happening as we add and remove entities.
                }
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                convertView = super.getView(position, convertView, parent);
                ((AppCompatCheckedTextView)(convertView.findViewById(R.id.contact_name))).setText(getItem(position).name);
                return convertView;
            }
        };
        adapter.setNotifyOnChange(true);
        contactsListView.setAdapter(adapter);
        ((RelativeLayout)findViewById(R.id.contacts_list)).addView(contactsListView);

        ((Toolbar)findViewById(R.id.toolbar)).setTitle(getTitleResource());

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
        if(TextUtils.isEmpty(query)) return;
        adapter.getFilter().filter(query);
    }

    public int getTitleResource(){
        return R.string.choose_a_contact;
    }

    public abstract void onContactSelect(Contact selectedContact);

    public boolean shouldEnableMultiSelect(){
        return false;
    }

    public ListView getContactsListView(){
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
        return true;
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
                ((ArrayAdapter)contactsListView.getAdapter()).getFilter().filter(newText);
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeDataChangeListener(contactsDataChangeListener);
    }
}
