package opencontacts.open.com.opencontacts.orm;

import static android.text.TextUtils.isEmpty;

import static java.util.Collections.emptyList;

import com.orm.SugarRecord;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.utils.DomainUtils;

/**
 * Created by sultanm on 7/22/17.
 */

public class PhoneNumber extends SugarRecord implements Serializable {
    @Nullable
    public String phoneNumber;
    public Contact contact;
    public boolean isPrimaryNumber = false;
    public String numericPhoneNumber; // for comparision during calls

    public PhoneNumber() {

    }

    public PhoneNumber(String mobileNumber, Contact contact, boolean isPrimaryNumber) {
        this.phoneNumber = mobileNumber;
        this.contact = contact;
        this.isPrimaryNumber = isPrimaryNumber;
        this.numericPhoneNumber = DomainUtils.getAllNumericPhoneNumber(mobileNumber);
    }

    public PhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public static List<PhoneNumber> getMatchingNumbers(String numericPhoneNumber) {
        if(isEmpty(numericPhoneNumber)) return emptyList();
        return PhoneNumber.find(PhoneNumber.class, "numeric_Phone_Number like ?", "%" + numericPhoneNumber);
    }
}
