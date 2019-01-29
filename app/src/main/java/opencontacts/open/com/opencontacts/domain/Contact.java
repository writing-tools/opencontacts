package opencontacts.open.com.opencontacts.domain;

import java.io.Serializable;
import java.util.List;

import opencontacts.open.com.opencontacts.orm.PhoneNumber;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getNumericKeyPadNumberForString;

/**
 * Created by sultanm on 7/22/17.
 */

public class Contact implements Serializable{
    public final long id;
    public String firstName;
    public String lastName;
    public List<PhoneNumber> phoneNumbers;
    public String name;
    public PhoneNumber primaryPhoneNumber;

    public String lastAccessed;
    public String t9Text;

    public Contact(long id) {
        this.id = id;
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers, String lastAccessed) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers, String lastAccessed, PhoneNumber primaryPhoneNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.name = firstName + " " + lastName;
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    public String setT9Text() {
        StringBuilder searchStringBuffer = new StringBuilder();
        searchStringBuffer.append(name).append(' ');
        for(PhoneNumber phoneNumber : phoneNumbers)
            searchStringBuffer.append(phoneNumber.numericPhoneNumber).append(' ');
        searchStringBuffer.append(getNumericKeyPadNumberForString(name));
        t9Text = searchStringBuffer.toString().toUpperCase();
        return t9Text;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Contact))
            return false;
        return id == ((Contact)obj).id;
    }
}
