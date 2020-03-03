package opencontacts.open.com.opencontacts.data.datastore;

import android.content.Context;
import android.support.v4.util.Pair;

import com.github.underscore.U;

import java.io.IOException;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.SimpleProperty;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.Triplet;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDBHelper.getContact;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDBHelper.replacePhoneNumbersInDB;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.updateContact;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_NONE;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_UPDATED;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getNameFromVCard;

public class ContactsSyncHelper {
    public static void replaceContactWithServers(Triplet<String, String, VCard> hrefEtagAndVCard, VCardData vCardData, Context context) {
        updateContact(vCardData.contact.getId(), "", hrefEtagAndVCard.z, context);
        VCardData updatedVCardData = VCardData.getVCardData(vCardData.contact.getId());
        updatedVCardData.href = hrefEtagAndVCard.x;
        updatedVCardData.status = STATUS_NONE;
        updatedVCardData.save();
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
        replacePhoneNumbersInDB(dbContact, hrefEtagAndVCard.z, getContact(vCardData.contact.getId()).primaryPhoneNumber.phoneNumber);
        vCardData.vcardDataAsString = hrefEtagAndVCard.z.write();
        vCardData.etag = hrefEtagAndVCard.y;
        vCardData.href = hrefEtagAndVCard.x;
        vCardData.uid = hrefEtagAndVCard.z.getUid().getValue();
        vCardData.status = STATUS_UPDATED;
        vCardData.save();
    }
}
