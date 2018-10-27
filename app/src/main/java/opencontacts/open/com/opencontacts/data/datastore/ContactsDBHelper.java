package opencontacts.open.com.opencontacts.data.datastore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;

/**
 * Created by sultanm on 7/17/17.
 */

class ContactsDBHelper {
    static Contact getDBContactWithId(Long id){
        return Contact.findById(Contact.class, id);
    }

    static void deleteContactInDB(Long contactId){
        Contact dbContact = Contact.findById(Contact.class, contactId);
        if(dbContact == null)
            return;
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        for(PhoneNumber dbPhoneNumber : dbPhoneNumbers)
            dbPhoneNumber.delete();
        List<CallLogEntry> callLogEntries = CallLogEntry.getCallLogEntriesFor(contactId);
        for(CallLogEntry callLogEntry : callLogEntries){
            callLogEntry.setId((long) -1);
            callLogEntry.save();
        }
        dbContact.delete();
    }

    static Contact getContactFromDB(String phoneNumber) {
        if(phoneNumber.length() < 8)
            return null; //TODO: find a better logic to determine how to compare mobile number excluding code.
        String phoneNumberToSearch = phoneNumber.length() > 10 ? phoneNumber.substring(phoneNumber.length() - 10) : phoneNumber;
        List<PhoneNumber> phoneNumbers = PhoneNumber.find(PhoneNumber.class, "phone_Number like ?", "%" + phoneNumberToSearch);
        if(phoneNumbers.size() == 0)
            return null;
        return phoneNumbers.get(0).contact;
    }

    static void replacePhoneNumbersInDB(Contact dbContact, List<String> phoneNumbers, String primaryPhoneNumber) {
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        for(String phoneNumber : phoneNumbers){
            new PhoneNumber(phoneNumber, dbContact, primaryPhoneNumber.equals(phoneNumber)).save();
        }
        PhoneNumber.deleteInTx(dbPhoneNumbers);
    }

    static void updateContactInDBWith(opencontacts.open.com.opencontacts.domain.Contact contact){
        opencontacts.open.com.opencontacts.orm.Contact dbContact = ContactsDBHelper.getDBContactWithId(contact.id);
        dbContact.firstName = contact.firstName;
        dbContact.lastName = contact.lastName;
        dbContact.save();
        replacePhoneNumbersInDB(dbContact, contact.phoneNumbers, contact.primaryPhoneNumber);
    }

    static List<opencontacts.open.com.opencontacts.domain.Contact> getAllContactsFromDB(){
        List<PhoneNumber> dbPhoneNumbers = PhoneNumber.listAll(PhoneNumber.class);
        HashMap<Long, opencontacts.open.com.opencontacts.domain.Contact> contactsMap= new HashMap<>();
        opencontacts.open.com.opencontacts.domain.Contact tempContact;
        for(PhoneNumber dbPhoneNumber: dbPhoneNumbers){
            tempContact = contactsMap.get(dbPhoneNumber.contact.getId());
            if(tempContact == null)
                tempContact = createNewDomainContact(dbPhoneNumber.contact, Collections.singletonList(dbPhoneNumber));
            else{
                tempContact.phoneNumbers.add(dbPhoneNumber.phoneNumber);
                if(dbPhoneNumber.isPrimaryNumber)
                    tempContact.primaryPhoneNumber = dbPhoneNumber.phoneNumber;
            }

            contactsMap.put(tempContact.id, tempContact);
        }
        return new ArrayList<>(contactsMap.values());
    }

    private static opencontacts.open.com.opencontacts.domain.Contact createNewDomainContact(opencontacts.open.com.opencontacts.orm.Contact contact, List<PhoneNumber> dbPhoneNumbers){
        List<String> phoneNumbers = new ArrayList<>(dbPhoneNumbers.size());
        String primaryPhoneNumber = dbPhoneNumbers.get(0).phoneNumber;
        for(PhoneNumber dbPhoneNumber : dbPhoneNumbers){
            if(dbPhoneNumber.isPrimaryNumber)
                primaryPhoneNumber = dbPhoneNumber.phoneNumber;
            phoneNumbers.add(dbPhoneNumber.phoneNumber);
        }
        return new opencontacts.open.com.opencontacts.domain.Contact(contact.getId(), contact.firstName, contact.lastName, phoneNumbers, contact.lastAccessed, primaryPhoneNumber);
    }

    static opencontacts.open.com.opencontacts.domain.Contact getContact(long id){
        if(id == -1)
            return null;
        opencontacts.open.com.opencontacts.orm.Contact contact = ContactsDBHelper.getDBContactWithId(id);
        if(contact == null)
            return null;
        return createNewDomainContact(contact, contact.getAllPhoneNumbers());
    }

    static void togglePrimaryNumber(String mobileNumber, opencontacts.open.com.opencontacts.domain.Contact contact) {
        List<PhoneNumber> allDbPhoneNumbersOfContact = PhoneNumber.find(PhoneNumber.class, "contact = ?", contact.id + "");
        if(allDbPhoneNumbersOfContact == null)
            return;
        for(PhoneNumber dbPhoneNumber : allDbPhoneNumbersOfContact){
            if(dbPhoneNumber.phoneNumber.equals(mobileNumber)){
                dbPhoneNumber.isPrimaryNumber = !dbPhoneNumber.isPrimaryNumber;
            }
            else
                dbPhoneNumber.isPrimaryNumber = false;
        }
        PhoneNumber.saveInTx(allDbPhoneNumbersOfContact);
    }
}
