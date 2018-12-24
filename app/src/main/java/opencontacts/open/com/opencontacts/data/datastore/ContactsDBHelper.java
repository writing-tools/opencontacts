package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.support.v4.util.Pair;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ezvcard.VCard;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getSearchablePhoneNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getNameFromVCard;

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
        VCardData.getVCardData(contactId).delete();
        dbContact.delete();
    }

    static Contact getContactFromDB(String phoneNumber) {
        String searchablePhoneNumber = getSearchablePhoneNumber(phoneNumber);
        if (searchablePhoneNumber == null) return null;
        List<PhoneNumber> matchingPhoneNumbers = PhoneNumber.find(PhoneNumber.class, "numeric_Phone_Number like ?", "%" + searchablePhoneNumber);
        if(matchingPhoneNumbers.isEmpty())
            return null;
        return matchingPhoneNumbers.get(0).contact;
    }

    static void replacePhoneNumbersInDB(Contact dbContact, VCard vcard, PhoneNumber primaryPhoneNumber) {
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        U.forEach(vcard.getTelephoneNumbers(),
                telephone -> {
                    String phoneNumberText = telephone.getText();
                    new PhoneNumber(phoneNumberText, dbContact, primaryPhoneNumber.phoneNumber.equals(phoneNumberText)).save();
        });
        PhoneNumber.deleteInTx(dbPhoneNumbers);
    }

    static void updateContactInDBWith(long contactId, PhoneNumber primaryNumber, VCard vCard, Context context){
        Contact dbContact = ContactsDBHelper.getDBContactWithId(contactId);
        Pair<String, String> nameFromVCard = getNameFromVCard(vCard, context);
        dbContact.firstName = nameFromVCard.first;
        dbContact.lastName = nameFromVCard.second;
        dbContact.save();
        replacePhoneNumbersInDB(dbContact, vCard, primaryNumber);
        updateVCardInDBWith(vCard, dbContact.getId());
    }

    static List<opencontacts.open.com.opencontacts.domain.Contact> getAllContactsFromDB(){
        List<PhoneNumber> dbPhoneNumbers = PhoneNumber.listAll(PhoneNumber.class);
        HashMap<Long, opencontacts.open.com.opencontacts.domain.Contact> contactsMap= new HashMap<>();
        opencontacts.open.com.opencontacts.domain.Contact tempContact;
        for(PhoneNumber dbPhoneNumber: dbPhoneNumbers){
            tempContact = contactsMap.get(dbPhoneNumber.contact.getId());
            if(tempContact == null){
                tempContact = createNewDomainContact(dbPhoneNumber.contact, Collections.singletonList(dbPhoneNumber));
                contactsMap.put(tempContact.id, tempContact);
            }
            else{
                tempContact.phoneNumbers = U.concat(tempContact.phoneNumbers, Collections.singletonList(dbPhoneNumber));
                if(dbPhoneNumber.isPrimaryNumber)
                    tempContact.primaryPhoneNumber = dbPhoneNumber;
            }

        }
        return new ArrayList<>(contactsMap.values());
    }

    private static opencontacts.open.com.opencontacts.domain.Contact createNewDomainContact(opencontacts.open.com.opencontacts.orm.Contact contact, List<PhoneNumber> dbPhoneNumbers){
        PhoneNumber primaryPhoneNumber = U.chain(dbPhoneNumbers)
                .filter(arg -> arg.isPrimaryNumber)
                .firstOrNull()
                .item();
        primaryPhoneNumber = primaryPhoneNumber == null ? dbPhoneNumbers.get(0) : primaryPhoneNumber;

        return new opencontacts.open.com.opencontacts.domain.Contact(contact.getId(), contact.firstName, contact.lastName, dbPhoneNumbers, contact.lastAccessed, primaryPhoneNumber);
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

    static void updateLastAccessed(long contactId, String callTimeStamp) {
        opencontacts.open.com.opencontacts.orm.Contact contact = ContactsDBHelper.getDBContactWithId(contactId);
        if (callTimeStamp.equals(contact.lastAccessed))
            return;
        contact.lastAccessed = callTimeStamp;
        contact.save();
    }

    public static VCardData getVCard(long contactId) {
        return VCardData.getVCardData(contactId);
    }

    public static void updateVCardInDBWith(VCard vCard, long contactId) {
        VCardData vCardDataInDB = VCardData.getVCardData(contactId);
        vCardDataInDB.vcardDataAsString = vCard.write();
        vCardDataInDB.save();
    }

    public static void deleteAllContacts(){
        Contact.deleteAll(Contact.class);
        PhoneNumber.deleteAll(PhoneNumber.class);
        VCardData.deleteAll(VCardData.class);
        CallLogDataStore.removeAllContactsLinking();
    }

    public static Contact addContact(VCard vcard, Context context){
        Contact contact = createContactSaveInDBAndReturnIt(vcard, context);
        createMobileNumbersAndSaveInDB(vcard, contact);
        createVCardDataAndSaveInDB(vcard, contact);
        return contact;
    }

    private static void createVCardDataAndSaveInDB(VCard vcard, Contact contact) {
        new VCardData(contact, vcard.write()).save();
    }

    private static void createMobileNumbersAndSaveInDB(VCard vcard, Contact contact) {
        for (Telephone telephoneNumber : vcard.getTelephoneNumbers()) {
            new PhoneNumber(telephoneNumber.getText(), contact, false).save();
        }
    }

    private static Contact createContactSaveInDBAndReturnIt(VCard vcard, Context context) {
        Pair<String, String> name = getNameFromVCard(vcard, context);
        Contact contact = new Contact(name.first, name.second);
        contact.save();
        return contact;
    }

}
