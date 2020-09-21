package opencontacts.open.com.opencontacts.domain;

import android.support.annotation.NonNull;

import com.github.underscore.U;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
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
    public String groups;
    public static final String GROUPS_SEPERATOR_CHAR = ",";

    private Contact(long id) {
        this.id = id;
    }

    private Contact(long id, String firstName, String lastName, List<PhoneNumber> phoneNumbers, String lastAccessed, PhoneNumber primaryPhoneNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumbers = phoneNumbers;
        this.name = getName(firstName, lastName);
        this.lastAccessed = lastAccessed;
        this.primaryPhoneNumber = primaryPhoneNumber;
    }

    private Contact(String firstName, String lastName, String number) {
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

    public Contact setGroups(String groups){
        this.groups = groups;
        return this;
    }

    public static Contact createNewDomainContact(opencontacts.open.com.opencontacts.orm.Contact contact, List<PhoneNumber> dbPhoneNumbers){
        List<PhoneNumber> safePhoneNumbersList = dbPhoneNumbers == null ? Collections.emptyList() : dbPhoneNumbers;
        return new opencontacts.open.com.opencontacts.domain.Contact(contact.getId(), contact.firstName,
                contact.lastName, safePhoneNumbersList, contact.lastAccessed,
                getPrimaryPhoneNumber(safePhoneNumbersList))
                .setGroups(contact.groups);
    }

    private static PhoneNumber getPrimaryPhoneNumber(List<PhoneNumber> dbPhoneNumbers) {
        if(dbPhoneNumbers.isEmpty()) return new PhoneNumber("");
        PhoneNumber primaryPhoneNumber = U.chain(dbPhoneNumbers)
                .filter(arg -> arg.isPrimaryNumber)
                .firstOrNull()
                .item();
        return primaryPhoneNumber == null ? dbPhoneNumbers.get(0) : primaryPhoneNumber;
    }

    public static Contact createDummyContact(long id){
        return new Contact(id);
    }

    public static Contact createDummyContact(String firstName, String lastName, String number){
        return new Contact(firstName, lastName, number);
    }

    public List<String> getGroupNames() {
        if(groups == null) return Collections.emptyList();
        return Arrays.asList(groups.split(GROUPS_SEPERATOR_CHAR));
    }

    public List<String> addGroup(String newGroupName){
        List<String> groupNames = getGroupNames();
        if (groupNames.contains(newGroupName)) return groupNames;
        groupNames = U.concat(groupNames, Collections.singleton(newGroupName));
        this.groups = getGroupsNamesCSVString(groupNames);
        return groupNames;
    }

    public List<String> removeGroup(String groupNameToRemove){
        List<String> groupNames = getGroupNames();
        List<String> finalGroupNames = U.reject(groupNames, groupNameToRemove::equals);
        if (finalGroupNames.size() == groupNames.size()) return groupNames;
        this.groups = getGroupsNamesCSVString(finalGroupNames);
        return finalGroupNames;
    }

    private static String getGroupsNamesCSVString(List<String> groups) {
        return U.join(groups, GROUPS_SEPERATOR_CHAR);
    }

}
