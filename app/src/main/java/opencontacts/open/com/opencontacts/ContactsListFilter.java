package opencontacts.open.com.opencontacts;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getContactComparatorBasedOnLastAccessed;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;

public abstract class ContactsListFilter extends Filter {
    private ArrayAdapter<Contact> adapter;
    private AllContactsHolder allContactsHolder;

    public ContactsListFilter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder) {
        this.adapter = adapter;
        this.allContactsHolder = allContactsHolder;
        mapAsync(allContactsHolder.getContacts());
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();

        List<Contact> filteredContacts = getMatchingContacts(constraint);
        results.values = filteredContacts;
        results.count = filteredContacts.size();
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.clear();
        if (constraint == null || constraint.length() == 0)
            adapter.addAll(allContactsHolder.getContacts());
        else
            adapter.addAll((List<Contact>) results.values);
        adapter.notifyDataSetChanged();
    }

    public abstract void updateMap(Contact contact);

    public interface AllContactsHolder {
        List<Contact> getContacts();
    }


    public void mapAsync(List<Contact> contacts) {
        processAsync(() -> createDataMapping(contacts));
    }

    public abstract void createDataMapping(List<Contact> contacts);

    private List<Contact> getMatchingContacts(CharSequence searchText) {
        List<Contact> contacts = allContactsHolder.getContacts();
        if (searchText == null || searchText.length() == 0) {
            return contacts;
        }

        List<Contact> filteredContacts = filter(searchText, contacts);
        sortFilteredContacts(filteredContacts);
        return filteredContacts;
    }

    public abstract List<Contact> filter(CharSequence searchText, List<Contact> contacts);

    protected void sortFilteredContacts(List<Contact> filteredContacts) {
        Collections.sort(filteredContacts, getContactComparatorBasedOnLastAccessed());
    }
}
