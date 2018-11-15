package opencontacts.open.com.opencontacts;

import android.widget.ArrayAdapter;
import android.widget.Filter;

import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

public class ContactsListFilter extends Filter{
    private ArrayAdapter<Contact> adapter;
    private AllContactsHolder allContactsHolder;

    public ContactsListFilter(ArrayAdapter<Contact> adapter, AllContactsHolder allContactsHolder){
        this.adapter = adapter;
        this.allContactsHolder = allContactsHolder;
    }
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();

        List<Contact> filteredContacts = DomainUtils.filter(constraint, allContactsHolder.getContacts());
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

    public interface AllContactsHolder{
        List<Contact> getContacts();
    }
}
