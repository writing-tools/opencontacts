package opencontacts.open.com.opencontacts.orm;

import com.orm.SugarRecord;

/**
 * Created by sultanm on 7/22/17.
 */

public class PhoneNumber extends SugarRecord{
    public String phoneNumber;
    public Contact contact;
    public boolean isPrimaryNumber = false;

    public PhoneNumber(){

    }
    public PhoneNumber(String mobileNumber, Contact contact, boolean isPrimaryNumber) {
        this.phoneNumber = mobileNumber;
        this.contact = contact;
        this.isPrimaryNumber = isPrimaryNumber;
    }
}
