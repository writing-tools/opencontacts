package opencontacts.open.com.opencontacts.orm;

import com.orm.SugarRecord;

import java.io.Serializable;

import opencontacts.open.com.opencontacts.utils.DomainUtils;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

/**
 * Created by sultanm on 7/22/17.
 */

public class PhoneNumber extends SugarRecord implements Serializable {
    public String phoneNumber;
    public Contact contact;
    public boolean isPrimaryNumber = false;
    public String numericPhoneNumber; // for comparision during calls
    public int type;
    public PhoneNumber(){

    }
    public PhoneNumber(String mobileNumber, Contact contact, boolean isPrimaryNumber, int type) {
        this.phoneNumber = mobileNumber;
        this.contact = contact;
        this.isPrimaryNumber = isPrimaryNumber;
        this.numericPhoneNumber = DomainUtils.getAllNumericPhoneNumber(mobileNumber);
        this.type = type;
    }

    public PhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
        this.type = VCardUtils.defaultPhoneNumberType;
    }
    public PhoneNumber(String phoneNumber, int type){
        this.phoneNumber = phoneNumber;
        this.type = type;
    }
}
