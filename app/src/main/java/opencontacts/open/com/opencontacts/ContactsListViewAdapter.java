package opencontacts.open.com.opencontacts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;

import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.domain.Contact;

import static android.view.View.*;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isT9SearchEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isWhatsappIntegrationEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldToggleContactActions;

public class ContactsListViewAdapter extends ArrayAdapter<Contact>{
    private boolean shouldToggleContactActions;
    private ContactsListActionsListener contactsListActionsListener;
    private LayoutInflater layoutInflater;
    public ContactsListFilter contactsListFilter;
    private boolean whatsappIntegrationEnabled;

    public ContactsListViewAdapter(@NonNull Context context, int resource, ContactsListFilter.AllContactsHolder allContactsHolder) {
        super(context, resource, new ArrayList<>(allContactsHolder.getContacts()));
        init(context);
        createContactsListFilter(allContactsHolder);
    }

    public ContactsListViewAdapter(@NonNull Context context) {
        super(context, R.layout.contact, new ArrayList<>());
        init(context);
    }

    private void init(@NonNull Context context) {
        layoutInflater = LayoutInflater.from(context);
        whatsappIntegrationEnabled = isWhatsappIntegrationEnabled(context);
        shouldToggleContactActions = shouldToggleContactActions(context);
    }

    private void createContactsListFilter(ContactsListFilter.AllContactsHolder allContactsHolder) {
        contactsListFilter = isT9SearchEnabled(getContext()) ? new ContactsListT9Filter(this, allContactsHolder)
                : new ContactsListTextFilter(this, allContactsHolder);
    }

    private final OnLongClickListener onLongClicked = v -> {
        if(contactsListActionsListener == null)
            return false;
        Contact contact = (Contact) v.getTag();
        contactsListActionsListener.onLongClick(contact);
        return true;
    };

    private final OnClickListener callContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onCallClicked(contact);
    };
    private final OnClickListener messageContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onMessageClicked(contact);
    };
    private final OnClickListener showContactDetails = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) v.getTag();
            contactsListActionsListener.onShowDetails(contact);
    };
    private final OnClickListener whatsappContact = v -> {
            if(contactsListActionsListener == null)
                return;
            Contact contact = (Contact) ((View)v.getParent()).getTag();
            contactsListActionsListener.onWhatsappClicked(contact);
    };

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Contact contact = getItem(position);
        if(convertView == null)
            convertView = layoutInflater.inflate(R.layout.contact, parent, false);
        ((TextView) convertView.findViewById(R.id.textview_full_name)).setText(contact.name);
        ((TextView) convertView.findViewById(R.id.textview_phone_number)).setText(contact.primaryPhoneNumber.phoneNumber);
        ImageButtonWithTint actionButton1 = convertView.findViewById(R.id.button_action1);
        ImageButtonWithTint actionButton2 = convertView.findViewById(R.id.button_action2);
        if(shouldToggleContactActions){
            actionButton1.setOnClickListener(messageContact);
            actionButton1.setImageResource(R.drawable.ic_chat_black_24dp);
            actionButton2.setOnClickListener(callContact);
            actionButton2.setImageResource(R.drawable.ic_call_black_24dp);
        }
        else {
            actionButton1.setOnClickListener(callContact);
            actionButton1.setImageResource(R.drawable.ic_call_black_24dp);
            actionButton2.setOnClickListener(messageContact);
            actionButton2.setImageResource(R.drawable.ic_chat_black_24dp);
        }
        View whatsappIcon = convertView.findViewById(R.id.button_whatsapp);
        if(whatsappIntegrationEnabled){
            whatsappIcon.setOnClickListener(whatsappContact);
            whatsappIcon.setVisibility(VISIBLE);
        }
        else whatsappIcon.setVisibility(GONE);
        convertView.setTag(contact);
        convertView.setOnClickListener(showContactDetails);
        convertView.setOnLongClickListener(onLongClicked);
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return contactsListFilter;
    }

    public void setContactsListActionsListener(ContactsListActionsListener contactsListActionsListener){
        this.contactsListActionsListener = contactsListActionsListener;
    }

    public interface ContactsListActionsListener {
        void onCallClicked(Contact contact);
        void onMessageClicked(Contact contact);
        void onShowDetails(Contact contact);
        void onWhatsappClicked(Contact contact);
        void onLongClick(Contact contact);
    }
}
