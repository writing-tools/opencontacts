package opencontacts.open.com.opencontacts.activities;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.underscore.Consumer;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.ContactsListT9Filter;
import opencontacts.open.com.opencontacts.ContactsListTextFilter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;

import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;

public abstract class SingleContactChooserActivityBase extends AppBaseActivity {
    protected ListView contactsListView;
    private Consumer<Contact> onContactSelect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsListView = new ListView(this);
        contactsListView.setTextFilterEnabled(false);
        final List<Contact> contacts = ContactsDataStore.getAllContacts();
        final ArrayAdapter<Contact> adapter = new ArrayAdapter<Contact>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<>(contacts)) {
            @NonNull
            @Override
            public Filter getFilter() {
                return isT9SearchEnabled(getContext()) ?
                        new ContactsListT9Filter(this, () -> contacts)
                        : new ContactsListTextFilter(this, () -> contacts);
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                convertView = super.getView(position, convertView, parent);
                ((TextView)(convertView.findViewById(android.R.id.text1))).setText(getItem(position).name);
                return convertView;
            }
        };
        contactsListView.setOnItemClickListener((parent, view, position, id) -> {
            if(onContactSelect == null) return;
            onContactSelect.accept(adapter.getItem(position));

        });
        contactsListView.setAdapter(adapter);
        ((LinearLayout)findViewById(R.id.parent_linear_layout)).addView(contactsListView);
    }

    public void setOnContactSelect(Consumer<Contact> onContactSelect){
        this.onContactSelect = onContactSelect;
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_add_to_contact;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchView searchView = new SearchView(this);
        bindSearchViewToContacts(searchView);
        menu.add(R.string.search)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setActionView(searchView);
        return true;
    }

    private void bindSearchViewToContacts(SearchView searchView) {
        searchView.setInputType(InputType.TYPE_CLASS_PHONE);

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
}
