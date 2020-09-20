package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.support.v4.util.Pair;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ezvcard.VCard;
import ezvcard.property.RawProperty;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.Favorite;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.Triplet;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

import static android.text.TextUtils.isEmpty;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_CREATED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_DELETED;
import static opencontacts.open.com.opencontacts.orm.VCardData.updateVCardData;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getSearchablePhoneNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getMobileNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getNameFromVCard;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.isFavorite;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.isPrimaryPhoneNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.markPrimaryPhoneNumberInVCard;

/**
 * Created by sultanm on 7/17/17.
 */

public class ContactsDBHelper {

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
        updateVCardDataForDeletion(VCardData.getVCardData(contactId));
        dbContact.delete();
    }

    private static void updateVCardDataForDeletion(VCardData vCardData) {
        if(vCardData.status == STATUS_CREATED){
            vCardData.delete();
            return;
        }
        vCardData.status = STATUS_DELETED;
        vCardData.vcardDataAsString = null;
        vCardData.save();
    }

    static Contact getContactFromDB(String phoneNumber) {
        String searchablePhoneNumber = getSearchablePhoneNumber(phoneNumber);
        if (searchablePhoneNumber == null) return null;
        List<PhoneNumber> matchingPhoneNumbers = PhoneNumber.find(PhoneNumber.class, "numeric_Phone_Number like ?", "%" + searchablePhoneNumber);
        if(matchingPhoneNumbers.isEmpty())
            return null;
        return matchingPhoneNumbers.get(0).contact;
    }

    static void replacePhoneNumbersInDB(Contact dbContact, VCard vcard, String primaryPhoneNumber) {
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        U.forEach(vcard.getTelephoneNumbers(),
                telephone -> {
                    String phoneNumberText = VCardUtils.getMobileNumber(telephone);
                    new PhoneNumber(phoneNumberText, dbContact, primaryPhoneNumber.equals(phoneNumberText)).save();
        });
        PhoneNumber.deleteInTx(dbPhoneNumbers);
    }

    static void updateContactInDBWith(long contactId, String primaryNumber, VCard vCard, Context context){
        Contact dbContact = ContactsDBHelper.getDBContactWithId(contactId);
        Pair<String, String> nameFromVCard = getNameFromVCard(vCard, context);
        dbContact.firstName = nameFromVCard.first;
        dbContact.lastName = nameFromVCard.second;
        dbContact.save();
        replacePhoneNumbersInDB(dbContact, vCard, primaryNumber);
        updateVCardData(vCard, dbContact.getId(), context);
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
        //for contacts without phone numbers
        List<PhoneNumber> emptyPhoneNumbersList = Collections.emptyList();
        new U.Chain<>(Contact.listAll(Contact.class))
                .filter(ormContact -> !contactsMap.containsKey(ormContact.getId()))
                .forEach(ormContact -> contactsMap.put(ormContact.getId(), createNewDomainContact(ormContact, emptyPhoneNumbersList)));

        return new ArrayList<>(contactsMap.values());
    }

    private static opencontacts.open.com.opencontacts.domain.Contact createNewDomainContact(opencontacts.open.com.opencontacts.orm.Contact contact, List<PhoneNumber> dbPhoneNumbers){
        if(dbPhoneNumbers == null || dbPhoneNumbers.isEmpty())
            return new opencontacts.open.com.opencontacts.domain.Contact(contact.getId(), contact.firstName, contact.lastName, dbPhoneNumbers, contact.lastAccessed, new PhoneNumber(""));
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
        markPrimaryPhoneNumberInVCard(contact, getVCard(contact.id).vcardDataAsString);
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

    public static void deleteAllContacts(){
        Contact.deleteAll(Contact.class);
        PhoneNumber.deleteAll(PhoneNumber.class);
        VCardData.deleteAll(VCardData.class);
        Favorite.deleteAll(Favorite.class);
        CallLogDataStore.removeAllContactsLinking();
    }

    public static Contact addContact(VCard vcard, Context context){
        Contact contact = createContactSaveInDBAndReturnIt(vcard, context);
        createMobileNumbersAndSaveInDB(vcard, contact);
        createVCardDataAndSaveInDB(vcard, contact);
        addToFavoritesInCaseIs(vcard, contact);
        return contact;
    }

    private static void addToFavoritesInCaseIs(VCard vcard, Contact contact) {
        if(isFavorite(vcard)) ContactsDataStore.addFavorite(contact);
    }

    public static Contact addContact(Triplet<String, String, VCard> hrefEtagAndVCard, Context context){
        Contact contact = createContactSaveInDBAndReturnIt(hrefEtagAndVCard.z, context);
        createMobileNumbersAndSaveInDB(hrefEtagAndVCard.z, contact);
        createVCardDataAndSaveInDB(hrefEtagAndVCard, contact);
        addToFavoritesInCaseIs(hrefEtagAndVCard.z, contact);
        return contact;
    }

    private static void createVCardDataAndSaveInDB(VCard vcard, Contact contact) {
        new VCardData(contact,
                vcard,
                vcard.getUid() == null ? UUID.randomUUID().toString() : vcard.getUid().getValue(),
                STATUS_CREATED,
                null
                ).save();
    }

    private static void createVCardDataAndSaveInDB(Triplet<String, String, VCard> hrefEtagAndVCard, Contact contact) {
        new VCardData(contact,
                hrefEtagAndVCard.z,
                hrefEtagAndVCard.z.getUid() == null ? UUID.randomUUID().toString() : hrefEtagAndVCard.z.getUid().getValue(),
                STATUS_CREATED,
                hrefEtagAndVCard.y,
                hrefEtagAndVCard.x
        ).save();
    }

    private static void createMobileNumbersAndSaveInDB(VCard vcard, Contact contact) {
        for (Telephone telephoneNumber : vcard.getTelephoneNumbers()) {
            try{//try block here to check if telephoneNumber.getUri is null. Do not want to check a lot of null combos. so try catch would help
                if(isEmpty(telephoneNumber.getText()) && isEmpty(telephoneNumber.getUri().getNumber()))
                    continue;
            }
            catch (Exception e){continue;}
            new PhoneNumber(getMobileNumber(telephoneNumber), contact, isPrimaryPhoneNumber(telephoneNumber)).save();
        }
    }

    private static Contact createContactSaveInDBAndReturnIt(VCard vcard, Context context) {
        Pair<String, String> name = getNameFromVCard(vcard, context);
        Contact contact = new Contact(name.first, name.second);
        contact.save();
        return contact;
    }

}
