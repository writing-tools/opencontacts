package opencontacts.open.com.opencontacts.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import opencontacts.open.com.opencontacts.ContactsListView;
import opencontacts.open.com.opencontacts.activities.MainActivity;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;

import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;

public class ContactsFragment extends AppBaseFragment implements SelectableTab {
    private LinearLayout linearLayout;
    private ContactsListView contactsListView;
    private MainActivity mainActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contactsListView = new ContactsListView(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        linearLayout = new LinearLayout(getContext());
        mainActivity = (MainActivity) getActivity();
        if(contactsListView != null) {
            linearLayout.removeAllViews();
            linearLayout.addView(contactsListView);
        }
        return linearLayout;
    }

    @Override
    public void onSelect() {}

    @Override
    public void onUnSelect() {
        contactsListView.clearTextFilter();
        mainActivity.collapseSearchView();
    }

    public void configureSearchInMenu(SearchView searchView) {
        searchView.setOnCloseListener(() -> {
            if(contactsListView != null)
                contactsListView.clearTextFilter();
            return false;
        });

        searchView.setInputType(isT9SearchEnabled(getContext()) ? InputType.TYPE_CLASS_PHONE : InputType.TYPE_CLASS_TEXT);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(contactsListView != null)
                    contactsListView.setFilterText(newText);
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        contactsListView.onDestroy();
        super.onDestroy();
    }
}
