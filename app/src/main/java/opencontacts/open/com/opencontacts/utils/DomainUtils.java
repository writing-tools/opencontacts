package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.StructuredName;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;

/**
 * Created by sultanm on 7/22/17.
 */

public class DomainUtils {
    public static final String EMPTY_STRING = "";
    public static final Pattern NON_NUMERIC_MATCHING_PATTERN = Pattern.compile("[^0-9]");
    public static final int MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS = 7;
    public static final int NUMBER_9 = 9;

    public static void exportAllContacts(Context context) throws IOException {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            AndroidUtils.showAlert(context, context.getString(R.string.error), context.getString(R.string.storage_not_mounted));
            return;
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yy hh-mm-ss");
        File file = new File(Environment.getExternalStorageDirectory(), "Contacts_" + simpleDateFormat.format(new Date()) + ".vcf");
        file.createNewFile();
        List<Contact> allContacts = ContactsDataStore.getAllContacts();
        VCardWriter vCardWriter = null;
        try{
            vCardWriter = new VCardWriter(new FileOutputStream(file), VCardVersion.V4_0);

            StructuredName structuredName = new StructuredName();

            for( Contact contact : allContacts){
                VCard vcard = new VCard();
                structuredName.setGiven(contact.firstName);
                structuredName.setFamily(contact.lastName);
                vcard.setStructuredName(structuredName);
                for(String phoneNumber : contact.phoneNumbers)
                    vcard.addTelephoneNumber(phoneNumber, TelephoneType.CELL);
                vCardWriter.write(vcard);
            }
        }
        finally {
            if(vCardWriter != null)
                vCardWriter.close();
        }

    }

    public static Contact getACopyOf(Contact contact){
        return new Contact(contact.id, contact.firstName, contact.lastName, new ArrayList<>(contact.phoneNumbers));
    }

    public static String getAllNumericPhoneNumber(String phoneNumber) {
        return NON_NUMERIC_MATCHING_PATTERN.matcher(phoneNumber).replaceAll(EMPTY_STRING);
    }

    @Nullable
    public static String getSearchablePhoneNumber(String phoneNumber) {
        String allNumericPhoneNumber = getAllNumericPhoneNumber(phoneNumber);
        if(allNumericPhoneNumber.length() < MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS)
            return null;
        return allNumericPhoneNumber.length() > NUMBER_9 ? allNumericPhoneNumber.substring(allNumericPhoneNumber.length() - NUMBER_9) : allNumericPhoneNumber;
    }

}
