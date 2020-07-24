package opencontacts.open.com.opencontacts.actions;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.addFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.isFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeFavorite;

public class DefaultContactsListActions implements ContactsListViewAdapter.ContactsListActionsListener {

    private Context context;

    public DefaultContactsListActions(Context context){
        this.context = context;
    }

    @Override
    public void onCallClicked(Contact contact) {
        AndroidUtils.call(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onMessageClicked(Contact contact) {
        AndroidUtils.message(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onShowDetails(Contact contact) {
        if(contact.id < 0) return;
        context.startActivity(AndroidUtils.getIntentToShowContactDetails(contact.id, context));
    }

    @Override
    public void onWhatsappClicked(Contact contact) {
        AndroidUtils.whatsapp(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onLongClick(Contact contact) {
        int favoritesResource = isFavorite(contact) ? R.string.remove_favorite : R.string.add_to_favorites;
        new AlertDialog.Builder(context)
                .setItems(new String[]{
                        context.getString(favoritesResource),
                        context.getString(R.string.add_shortcut)
                }, (dialog, which) -> {
                    switch(which){
                        case 0:
                            if (favoritesResource == R.string.add_to_favorites) addFavorite(contact);
                            else removeFavorite(contact);
                            break;
                        case 1:
                            boolean added = DomainUtils.addContactAsShortcut(contact, context);
                            Toast.makeText(context,
                                    added ? context.getString(R.string.added_shortcut) : context.getString(R.string.failed_adding_shortcut),
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                }).show();
    }

}
