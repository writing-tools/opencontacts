package opencontacts.open.com.opencontacts.orm;

import android.content.Context;

import com.github.underscore.U;
import com.orm.SugarRecord;

import java.io.IOException;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

import static android.widget.Toast.LENGTH_SHORT;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.setFormattedNameIfNotPresent;

public class VCardData extends SugarRecord {
    public static final int   STATUS_NONE    = 0,
                              STATUS_CREATED = 1,
                              STATUS_UPDATED = 2,
                              STATUS_DELETED = 3;
    public Contact contact;
    public String vcardDataAsString;
    public String uid;
    public int status;
    public String etag;
    public String href;

    public VCardData(Contact contact, VCard vCard, String uid, int status, String etag) {
        VCardUtils.setFormattedNameIfNotPresent(vCard);
        this.contact = contact;
        this.vcardDataAsString = vCard.write();
        this.uid = uid;
        this.status = status;
        this.etag = etag;
    }

    public VCardData(Contact contact, VCard vCard, String uid, int status, String etag, String href) {
        VCardUtils.setFormattedNameIfNotPresent(vCard);
        this.contact = contact;
        this.vcardDataAsString = vCard.write();
        this.uid = uid;
        this.status = status;
        this.etag = etag;
        this.href = href;
    }

    public VCardData(){}

    public static VCardData getVCardData(long contactId){
        return U.firstOrNull(find(VCardData.class, "contact = ?", "" + contactId));
    }

    public static void updateVCardData(VCard vCard, long contactId, Context context) {
        VCardData vCardDataInDB = VCardData.getVCardData(contactId);
        try {
            VCard dbVCard = new VCardReader(vCardDataInDB.vcardDataAsString).readNext();
            dbVCard.setStructuredName(vCard.getStructuredName());
            setFormattedNameIfNotPresent(dbVCard);
            dbVCard.getTelephoneNumbers().clear();
            dbVCard.getTelephoneNumbers().addAll(vCard.getTelephoneNumbers());
            dbVCard.getEmails().clear();
            dbVCard.getEmails().addAll(vCard.getEmails());
            dbVCard.getAddresses().clear();
            dbVCard.getAddresses().addAll(vCard.getAddresses());
            dbVCard.getUrls().clear();
            dbVCard.getUrls().addAll(vCard.getUrls());
            dbVCard.setBirthday(vCard.getBirthday());
            addNotesToVCard(vCard, dbVCard);
            vCardDataInDB.vcardDataAsString = dbVCard.write();
            vCardDataInDB.status = vCardDataInDB.status == STATUS_CREATED ? STATUS_CREATED : STATUS_UPDATED;
            vCardDataInDB.save();
        } catch (IOException e) {
            e.printStackTrace();
            AndroidUtils.toastFromNonUIThread(R.string.error_while_saving_contact, LENGTH_SHORT, context);
        }
    }

    private static void addNotesToVCard(VCard vCard, VCard dbVCard) {
        if(vCard.getNotes().isEmpty()){
            if(!dbVCard.getNotes().isEmpty()) dbVCard.getNotes().remove(0);
        }
        else{
            if(dbVCard.getNotes().isEmpty()) dbVCard.getNotes().addAll(vCard.getNotes());
            else dbVCard.getNotes().set(0, vCard.getNotes().get(0));
        }
    }

}
