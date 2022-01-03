package opencontacts.open.com.opencontacts.actions;

import static android.widget.Toast.LENGTH_LONG;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasItBeenAWeekSinceLastExportOfContacts;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.markAutoExportComplete;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldExportContactsEveryWeek;

import android.Manifest;
import android.content.Context;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

public class AutoContactsExporter implements ContactsHouseKeepingAction {

    @Override
    public void perform(List<Contact> contacts, Context context) {
        if (!(shouldExportContactsEveryWeek(context) && hasItBeenAWeekSinceLastExportOfContacts(context))) return;
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, context)) return;
        try {
            DomainUtils.exportAllContacts(context);
            markAutoExportComplete(context);
        } catch (Exception e) {
            e.printStackTrace();
            toastFromNonUIThread(R.string.failed_exporting_contacts, LENGTH_LONG, context);
        }
    }
}
