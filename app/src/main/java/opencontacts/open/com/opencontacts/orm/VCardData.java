package opencontacts.open.com.opencontacts.orm;

import com.github.underscore.U;
import com.orm.SugarRecord;

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

    public VCardData(Contact contact, String vcardDataAsString, String uid, int status, String etag) {
        this.contact = contact;
        this.vcardDataAsString = vcardDataAsString;
        this.uid = uid;
        this.status = status;
        this.etag = etag;
    }

    public VCardData(Contact contact, String vcardDataAsString, String uid, int status, String etag, String href) {
        this.contact = contact;
        this.vcardDataAsString = vcardDataAsString;
        this.uid = uid;
        this.status = status;
        this.etag = etag;
        this.href = href;
    }

    public VCardData(){}

    public static VCardData getVCardData(long contactId){
        return U.firstOrNull(find(VCardData.class, "contact = ?", "" + contactId));
    }
}
