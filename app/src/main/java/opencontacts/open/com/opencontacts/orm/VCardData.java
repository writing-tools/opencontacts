package opencontacts.open.com.opencontacts.orm;

import com.orm.SugarRecord;

public class VCardData extends SugarRecord {
    public Contact contact;
    public String vcardDataAsString;

    public VCardData(Contact contact, String vcardDataAsString) {
        this.contact = contact;
        this.vcardDataAsString = vcardDataAsString;
    }

    public VCardData(){}
}
