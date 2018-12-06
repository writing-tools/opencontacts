package opencontacts.open.com.opencontacts;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opencontacts.open.com.opencontacts.domain.Contact;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;

public class ContactsListFilter extends Filter{
    private ArrayAdapter<Contact> adapter;
    private AllContactsHolder allContactsHolder;
    private Map<Long, String> t9Map;

    public ContactsListFilter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder){
        this.adapter = adapter;
        this.allContactsHolder = allContactsHolder;
        t9Map = new HashMap<>();
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
        t9Map.put(contact.id, contact.toString());
    }

    public interface AllContactsHolder{
        List<Contact> getContacts();
    }


    public void mapAsync(List<Contact> contacts) {
        processAsync(() -> {
            for(Contact contact : contacts){
                t9Map.put(contact.id, contact.toString());
            }
        });
    }

    private List<Contact> getMatchingContacts(CharSequence constraint){
        List<Contact> contacts = allContactsHolder.getContacts();
        if(constraint == null || constraint.length() == 0){
            return contacts;
        }

        ArrayList<Contact> filteredContacts = new ArrayList<>();
        String t9Text;
        for (Contact c : contacts) {
            t9Text = t9Map.get(c.id);

            if(t9Text == null){
                t9Text = c.toString().toUpperCase();
                t9Map.put(c.id, t9Text);
            }
            if (t9Text.contains( constraint.toString().toUpperCase() )) {
                filteredContacts.add(c);
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
