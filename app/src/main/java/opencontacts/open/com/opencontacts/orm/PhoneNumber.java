package opencontacts.open.com.opencontacts.orm;

import com.orm.SugarRecord;

import opencontacts.open.com.opencontacts.utils.DomainUtils;

/**
 * Created by sultanm on 7/22/17.
 */

public class PhoneNumber extends SugarRecord{
    public String phoneNumber;
    public Contact contact;
    public boolean isPrimaryNumber = false;
    public String numericPhoneNumber; // for comparision during calls

    public PhoneNumber(){

    }
    public PhoneNumber(String mobileNumber, Contact contact, boolean isPrimaryNumber) {
        this.phoneNumber = mobileNumber;
        this.contact = contact;
        this.isPrimaryNumber = isPrimaryNumber;
        this.numericPhoneNumber = DomainUtils.getAllNumericPhoneNumber(mobileNumber);
    }
}
