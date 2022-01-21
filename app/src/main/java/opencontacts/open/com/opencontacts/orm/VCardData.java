package opencontacts.open.com.opencontacts.orm;

import static opencontacts.open.com.opencontacts.utils.VCardUtils.writeVCardToString;

import android.content.Context;

import androidx.annotation.Nullable;

import com.github.underscore.U;
import com.orm.SugarRecord;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

public class VCardData extends SugarRecord {
    public static final int STATUS_NONE = 0,
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
        VCardUtils.setUidIfNotPresent(vCard, uid);
        this.contact = contact;
        this.vcardDataAsString = writeVCardToString(vCard);
        this.uid = uid;
        this.status = status;
        this.etag = etag;
    }

    public VCardData(Contact contact, VCard vCard, String uid, int status, String etag, String href) {
        VCardUtils.setFormattedNameIfNotPresent(vCard);
        VCardUtils.setUidIfNotPresent(vCard, uid);
        this.contact = contact;
        this.vcardDataAsString = writeVCardToString(vCard);
        this.uid = uid;
        this.status = status;
        this.etag = etag;
        this.href = href;
    }

    public VCardData() {
    }

    @Nullable
    public static VCardData getVCardData(long contactId) {
        return U.firstOrNull(find(VCardData.class, "contact = ?", "" + contactId));
    }

    public static void updateVCardData(VCard vCard, long contactId, Context context) {
        VCardData vCardDataInDB = VCardData.getVCardData(contactId);
        VCardUtils.setFormattedNameIfNotPresent(vCard);
        vCardDataInDB.vcardDataAsString = writeVCardToString(vCard);
        vCardDataInDB.status = vCardDataInDB.status == STATUS_CREATED ? STATUS_CREATED : STATUS_UPDATED;
        vCardDataInDB.save();
    }

    private static void addNotesToVCard(VCard vCard, VCard dbVCard) {
        if (vCard.getNotes().isEmpty()) {
            if (!dbVCard.getNotes().isEmpty()) dbVCard.getNotes().remove(0);
        } else {
            if (dbVCard.getNotes().isEmpty()) dbVCard.getNotes().addAll(vCard.getNotes());
            else dbVCard.getNotes().set(0, vCard.getNotes().get(0));
        }
    }

}
