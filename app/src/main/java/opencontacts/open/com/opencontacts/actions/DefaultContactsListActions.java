package opencontacts.open.com.opencontacts.actions;

import static opencontacts.open.com.opencontacts.activities.CallLogGroupDetailsActivity.getIntentToShowCallLogEntries;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.addFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.isFavorite;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.removeFavorite;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.onSocialLongPress;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.shareContact;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.shareContactAsText;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import opencontacts.open.com.opencontacts.ContactsListViewAdapter;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

public class DefaultContactsListActions implements ContactsListViewAdapter.ContactsListActionsListener {

    private Context context;

    public DefaultContactsListActions(Context context) {
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
        if (contact.id < 0) return;
        context.startActivity(AndroidUtils.getIntentToShowContactDetails(contact.id, context));
    }

    @Override
    public void onSocialAppClicked(Contact contact) {
        AndroidUtils.openSocialApp(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onSocialLongClicked(Contact contact) {
        onSocialLongPress(contact.primaryPhoneNumber.phoneNumber, context);
    }

    @Override
    public void onLongClick(Contact contact) {
        int favoritesResource = isFavorite(contact) ? R.string.remove_favorite : R.string.add_to_favorites;
        new AlertDialog.Builder(context)
            .setItems(new String[]{
                context.getString(favoritesResource),
                context.getString(R.string.add_shortcut),
                context.getString(R.string.share_menu_item),
                context.getString(R.string.share_as_text),
                context.getString(R.string.calllog)
            }, (dialog, which) -> {
                switch (which) {
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
                    case 2:
                        shareContact(contact.id, context);
                        break;
                    case 3:
                        shareContactAsText(contact.id, context);
                        break;
                    case 4:
                        context.startActivity(getIntentToShowCallLogEntries(contact.primaryPhoneNumber.phoneNumber, context));
                        break;
                }
            }).show();
    }

}
