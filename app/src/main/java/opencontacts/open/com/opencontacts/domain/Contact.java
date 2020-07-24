package opencontacts.open.com.opencontacts.domain;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.List;

import opencontacts.open.com.opencontacts.orm.PhoneNumber;

import static opencontacts.open.com.opencontacts.utils.Common.getEmptyStringIfNull;
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
    public String textSearchTarget;

    public Contact(long id) {
        this.id = id;
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = getName(firstName, lastName);
        this.phoneNumbers = phoneNumbers;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers, String lastAccessed) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = getName(firstName, lastName);
        this.phoneNumbers = phoneNumbers;
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers, String lastAccessed, PhoneNumber primaryPhoneNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.name = getName(firstName, lastName);
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    public Contact(String firstName, String lastName, String number) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = getName(firstName, lastName);
        this.primaryPhoneNumber = new PhoneNumber(number);
        id = -1;
    }

    public void setT9Text() {
        StringBuilder searchStringBuffer = new StringBuilder();
        searchStringBuffer.append(name).append(' ');
        for(PhoneNumber phoneNumber : phoneNumbers)
            searchStringBuffer.append(phoneNumber.numericPhoneNumber).append(' ');
        searchStringBuffer.append(getNumericKeyPadNumberForString(name));
        t9Text = searchStringBuffer.toString().toUpperCase();
    }

    public void setTextSearchTarget(){
        StringBuilder searchStringBuffer = new StringBuilder();
        searchStringBuffer.append(name).append(' ');
        for(PhoneNumber phoneNumber : phoneNumbers)
            searchStringBuffer.append(phoneNumber.numericPhoneNumber).append(' ');
        searchStringBuffer.append(name);
        textSearchTarget = searchStringBuffer.toString().toUpperCase();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Contact))
            return false;
        return id == ((Contact)obj).id;
    }

    @NonNull
    private String getName(String firstName, String lastName) {
        return getEmptyStringIfNull(firstName) + " " + getEmptyStringIfNull(lastName);
    }

}
