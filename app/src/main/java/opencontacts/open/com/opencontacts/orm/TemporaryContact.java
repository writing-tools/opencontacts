package opencontacts.open.com.opencontacts.orm;

import com.orm.SugarRecord;

import java.util.Date;

public class TemporaryContact extends SugarRecord {
    public Contact contact;
    public Date markedTemporaryOn;

    public TemporaryContact() {
    }

    public TemporaryContact(Contact contact, Date markedTemporaryOn) {
        this.contact = contact;
        this.markedTemporaryOn = markedTemporaryOn;
    }
}
