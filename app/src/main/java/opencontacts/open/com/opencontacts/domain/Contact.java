package opencontacts.open.com.opencontacts.domain;

import java.io.Serializable;
import java.util.List;

import opencontacts.open.com.opencontacts.utils.Common;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getNumericKeyPadNumberForString;

/**
 * Created by sultanm on 7/22/17.
 */

public class Contact implements Serializable{
    public final long id;
    public String firstName;
    public String lastName;
    public List<String> phoneNumbers;
    public String name;
    public String primaryPhoneNumber;

    public String lastAccessed;
    public String t9Text;

    public Contact(long id) {
        this.id = id;
    }

    public Contact(long id, String firstName, String lastName, List<String> phoneNumbers) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<String> phoneNumbers, String lastAccessed) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = phoneNumbers.get(0);
    }

    public Contact(long id, String firstName, String lastName, List<String> phoneNumbers, String lastAccessed, String primaryPhoneNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.name = firstName + " " + lastName;
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    @Override
    public String toString() {
        StringBuffer searchStringBuffer = new StringBuffer();
        searchStringBuffer.append(name).append(' ');
        for(String phoneNumber : phoneNumbers)
            searchStringBuffer.append(phoneNumber).append(' ');
        searchStringBuffer.append(getNumericKeyPadNumberForString(name));
        return searchStringBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof Contact))
            return false;
        return id == ((Contact)obj).id;
    }
}
