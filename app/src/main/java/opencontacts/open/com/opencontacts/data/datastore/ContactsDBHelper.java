package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.getSearchablePhoneNumber;

/**
 * Created by sultanm on 7/17/17.
 */

class ContactsDBHelper {

    private static String noNameString;

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

    static void replacePhoneNumbersInDB(Contact dbContact, List<PhoneNumber> phoneNumbers, PhoneNumber primaryPhoneNumber) {
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        for(PhoneNumber phoneNumber : phoneNumbers){
            new PhoneNumber(phoneNumber.phoneNumber, dbContact, primaryPhoneNumber.phoneNumber.equals(phoneNumber.phoneNumber)).save();
        }
        PhoneNumber.deleteInTx(dbPhoneNumbers);
    }

    static void updateContactInDBWith(opencontacts.open.com.opencontacts.domain.Contact contact, VCard vCard){
        opencontacts.open.com.opencontacts.orm.Contact dbContact = ContactsDBHelper.getDBContactWithId(contact.id);
        dbContact.firstName = contact.firstName;
        dbContact.lastName = contact.lastName;
        dbContact.save();
        replacePhoneNumbersInDB(dbContact, contact.phoneNumbers, contact.primaryPhoneNumber);
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

    public static Contact addContact(String firstName, String lastName, List<PhoneNumber> phoneNumbers, VCard vCard){
        Contact dbContact = new Contact(firstName, lastName);
        dbContact.save();
        ContactsDBHelper.replacePhoneNumbersInDB(dbContact, phoneNumbers, U.first(phoneNumbers));
        VCardData newVCardData = new VCardData(dbContact, vCard.write());
        newVCardData.save();
        return dbContact;
    }

    public static void deleteAllContacts(){
        Contact.deleteAll(Contact.class);
        PhoneNumber.deleteAll(PhoneNumber.class);
        VCardData.deleteAll(VCardData.class);
    }

    public static boolean addContact(VCard vcard, Context context){
        Contact contact = createContactSaveInDBAndReturnIt(vcard, context);
        createMobileNumbersAndSaveInDB(vcard, contact);
        createVCardDataAndSaveInDB(vcard, contact);
        return true;
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
        if(noNameString == null) noNameString = context.getString(R.string.noname);
        Contact contact;
        StructuredName structuredName = vcard.getStructuredName();
        FormattedName formattedName = vcard.getFormattedName();
        if (structuredName == null)
            if (formattedName == null) {
                contact = new Contact(noNameString, "");
            } else contact = new Contact(formattedName.getValue(), "");
        else contact = createContactWithStructuredName(structuredName);
        contact.save();
        return contact;
    }

    private static Contact createContactWithStructuredName(StructuredName structuredName) {
        List<String> additionalNames = structuredName.getAdditionalNames();
        String lastName = structuredName.getFamily();
        if (additionalNames.size() > 0) {
            StringBuilder nameBuffer = new StringBuilder();
            for (String additionalName : additionalNames)
                nameBuffer.append(additionalName).append(" ");
            lastName = nameBuffer.append(structuredName.getFamily()).toString();
        }
        return new Contact(structuredName.getGiven(), lastName);
    }

}
