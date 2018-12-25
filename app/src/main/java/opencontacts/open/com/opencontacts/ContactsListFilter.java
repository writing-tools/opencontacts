package opencontacts.open.com.opencontacts;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;

public class ContactsListFilter extends Filter{
    private ArrayAdapter<Contact> adapter;
    private AllContactsHolder allContactsHolder;

    public ContactsListFilter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder){
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

    public void updateMap(Contact contact) {
        contact.setT9Text();
    }

    public interface AllContactsHolder{
        List<Contact> getContacts();
    }


    public void mapAsync(List<Contact> contacts) {
        processAsync(() -> {
            for(Contact contact : contacts){
                contact.setT9Text();
            }
        });
    }

    private List<Contact> getMatchingContacts(CharSequence searchText){
        List<Contact> contacts = allContactsHolder.getContacts();
        if(searchText == null || searchText.length() == 0){
            return contacts;
        }

        ArrayList<Contact> filteredContacts = new ArrayList<>();
        for (Contact contact : contacts) {
            if(contact.t9Text == null){
                contact.setT9Text();
            }
            if (contact.t9Text.contains(searchText.toString().toUpperCase())) {
                filteredContacts.add(contact);
            }
        }
        sortFilteredContacts(filteredContacts);
        return filteredContacts;
    }

    private void sortFilteredContacts(ArrayList<Contact> filteredContacts) {
        Collections.sort(filteredContacts, (contact1, contact2) -> {
            String lastAccessedDate1 = contact1.lastAccessed;
            String lastAccessedDate2 = contact2.lastAccessed;
            if(lastAccessedDate1 == null && lastAccessedDate2 == null)
                return 0;
            else if(lastAccessedDate1 == null)
                return 1;
            else if (lastAccessedDate2 == null)
                return -1;
            else
                return lastAccessedDate2.compareTo(lastAccessedDate1);
        });
    }
}
