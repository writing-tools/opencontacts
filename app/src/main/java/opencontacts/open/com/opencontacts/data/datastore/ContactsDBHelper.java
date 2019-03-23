package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.support.v4.util.Pair;
import android.widget.Toast;

import com.github.underscore.U;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.SimpleProperty;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.Triplet;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

import static android.text.TextUtils.isEmpty;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_CREATED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_DELETED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_UPDATED;
import static opencontacts.open.com.opencontacts.utils.DomainUtils.getSearchablePhoneNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getMobileNumber;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getNameFromVCard;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.setFormattedNameIfNotPresent;

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

    static void replacePhoneNumbersInDB(Contact dbContact, VCard vcard, PhoneNumber primaryPhoneNumber) {
        List<PhoneNumber> dbPhoneNumbers = dbContact.getAllPhoneNumbers();
        U.forEach(vcard.getTelephoneNumbers(),
                telephone -> {
                    String phoneNumberText = VCardUtils.getMobileNumber(telephone);
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
        updateVCardInDBWith(vCard, dbContact.getId(), context);
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

    private static void updateVCardInDBWith(VCard vCard, long contactId, Context context) {
        VCardData vCardDataInDB = VCardData.getVCardData(contactId);
        try {
            VCard dbVCard = new VCardReader(vCardDataInDB.vcardDataAsString).readNext();
            dbVCard.setStructuredName(vCard.getStructuredName());
            setFormattedNameIfNotPresent(dbVCard);
            dbVCard.getTelephoneNumbers().clear();
            dbVCard.getTelephoneNumbers().addAll(vCard.getTelephoneNumbers());
            dbVCard.getEmails().clear();
            dbVCard.getEmails().addAll(vCard.getEmails());
            dbVCard.getAddresses().clear();
            dbVCard.getAddresses().addAll(vCard.getAddresses());
            dbVCard.getUrls().clear();
            dbVCard.getUrls().addAll(vCard.getUrls());
            dbVCard.setBirthday(vCard.getBirthday());
            addNotesToDBVCard(vCard, dbVCard);
            vCardDataInDB.vcardDataAsString = dbVCard.write();
            updateStatusInVCardDataForUpdateOperation(vCardDataInDB);
            vCardDataInDB.save();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.error_while_saving_contact, Toast.LENGTH_SHORT).show();
        }
    }

    private static void updateStatusInVCardDataForUpdateOperation(VCardData vCardDataInDB) {
        vCardDataInDB.status = vCardDataInDB.status == STATUS_CREATED ? STATUS_CREATED : STATUS_UPDATED;
    }

    private static void addNotesToDBVCard(VCard vCard, VCard dbVCard) {
        if(vCard.getNotes().isEmpty()){
            if(!dbVCard.getNotes().isEmpty()) dbVCard.getNotes().remove(0);
        }
        else{
            if(dbVCard.getNotes().isEmpty()) dbVCard.getNotes().addAll(vCard.getNotes());
            else dbVCard.getNotes().set(0, vCard.getNotes().get(0));
        }
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

    public static Contact addContact(Triplet<String, String, VCard> vcardTriplet, Context context){
        Contact contact = createContactSaveInDBAndReturnIt(vcardTriplet.z, context);
        createMobileNumbersAndSaveInDB(vcardTriplet.z, contact);
        createVCardDataAndSaveInDB(vcardTriplet, contact);
        return contact;
    }

    private static void createVCardDataAndSaveInDB(VCard vcard, Contact contact) {
        VCardUtils.setFormattedNameIfNotPresent(vcard);
        new VCardData(contact,
                vcard.write(),
                vcard.getUid() == null ? UUID.randomUUID().toString() : vcard.getUid().getValue(),
                STATUS_CREATED,
                null
                ).save();
    }

    private static void createVCardDataAndSaveInDB(Triplet<String, String, VCard> vcardTriplet, Contact contact) {
        VCardUtils.setFormattedNameIfNotPresent(vcardTriplet.z);
        new VCardData(contact,
                vcardTriplet.z.write(),
                vcardTriplet.z.getUid() == null ? UUID.randomUUID().toString() : vcardTriplet.z.getUid().getValue(),
                STATUS_CREATED,
                vcardTriplet.y,
                vcardTriplet.x
        ).save();
    }

    private static void createMobileNumbersAndSaveInDB(VCard vcard, Contact contact) {
        for (Telephone telephoneNumber : vcard.getTelephoneNumbers()) {
            try{//try block here to check if telephoneNumber.getUri is null. Do not want to check a lot of null combos. so try catch would help
                if(isEmpty(telephoneNumber.getText()) && isEmpty(telephoneNumber.getUri().getNumber()))
                    continue;
            }
            catch (Exception e){continue;}
            new PhoneNumber(getMobileNumber(telephoneNumber), contact, false).save();
        }
    }

    private static Contact createContactSaveInDBAndReturnIt(VCard vcard, Context context) {
        Pair<String, String> name = getNameFromVCard(vcard, context);
        Contact contact = new Contact(name.first, name.second);
        contact.save();
        return contact;
    }

    public static void merge(Triplet<String, String, VCard> hrefEtagAndVCard, VCardData vCardData, Context context) {
        VCard vCardInDb = null;
        try {
            vCardInDb = new VCardReader(vCardData.vcardDataAsString).readNext();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Telephone> telephoneNumbersInDownloadedVCard = hrefEtagAndVCard.z.getTelephoneNumbers();

        List<Telephone> extraTelephoneNumbersInDb = U.chain(vCardInDb.getTelephoneNumbers())
                .filterFalse(telephoneNumbersInDownloadedVCard::contains)
                .value();

        List<Address> addressesInDownloadedCard = hrefEtagAndVCard.z.getAddresses();

        List<Address> extraAddressInDb = U.chain(vCardInDb.getAddresses())
                .filterFalse(addressesInDownloadedCard::contains)
                .value();

        List<String> notesInDownloadedCard = U.map(hrefEtagAndVCard.z.getNotes(), SimpleProperty::getValue);
        List<Note> extraNotesInDb = U.chain(vCardInDb.getNotes())
                .map(note -> new Pair<>(note, note.getValue()))
                .filterFalse(pair -> notesInDownloadedCard.contains(pair.second))
                .map(pair -> pair.first)
                .value();

        List<String> emailsInDownloadedCard = U.map(hrefEtagAndVCard.z.getEmails(), SimpleProperty::getValue);
        List<Email> extraEmailsInDb = U.chain(vCardInDb.getEmails())
                .map(email -> new Pair<>(email, email.getValue()))
                .filterFalse(pair -> emailsInDownloadedCard.contains(pair.second))
                .map(pair -> pair.first)
                .value();

        hrefEtagAndVCard.z.getTelephoneNumbers().addAll(extraTelephoneNumbersInDb);
        hrefEtagAndVCard.z.getEmails().addAll(extraEmailsInDb);
        hrefEtagAndVCard.z.getNotes().addAll(extraNotesInDb);
        hrefEtagAndVCard.z.getAddresses().addAll(extraAddressInDb);

        Contact dbContact = ContactsDBHelper.getDBContactWithId(vCardData.contact.getId());
        Pair<String, String> nameFromVCard = getNameFromVCard(hrefEtagAndVCard.z, context);
        dbContact.firstName = nameFromVCard.first;
        dbContact.lastName = nameFromVCard.second;
        dbContact.save();
        replacePhoneNumbersInDB(dbContact, hrefEtagAndVCard.z, getContact(vCardData.contact.getId()).primaryPhoneNumber);
        vCardData.vcardDataAsString = hrefEtagAndVCard.z.write();
        vCardData.etag = hrefEtagAndVCard.y;
        vCardData.href = hrefEtagAndVCard.x;
        vCardData.uid = hrefEtagAndVCard.z.getUid().getValue();
        vCardData.status = STATUS_UPDATED;
        vCardData.save();
    }

}
