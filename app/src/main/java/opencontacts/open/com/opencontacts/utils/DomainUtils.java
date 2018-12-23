package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.github.underscore.U;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.StructuredName;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;

import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;
import static opencontacts.open.com.opencontacts.utils.Common.replaceAccentedCharactersWithEnglish;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.intToTelephoneTypeMap;

/**
 * Created by sultanm on 7/22/17.
 */

public class DomainUtils {
    public static final String EMPTY_STRING = "";
    public static final Pattern NON_NUMERIC_MATCHING_PATTERN = Pattern.compile("[^0-9]");
    public static final int MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS = 7;
    public static final int NUMBER_9 = 9;

    static Map<Character, Integer> characterToIntegerMappingForKeyboardLayout;
    static Map<Integer, String> mobileNumberTypeToTranslatedText;
    static Map<String, Integer> translatedTextToMobileNumberType;
    private static String defaultPhoneNumberType;

    static {
        characterToIntegerMappingForKeyboardLayout = new HashMap();
        int[] numericsMappingForAlphabetsInNumberKeypad = { 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9};
        for(int i=0, charCodeForA = 65; i<26; i++){
            characterToIntegerMappingForKeyboardLayout.put((char) (charCodeForA + i), numericsMappingForAlphabetsInNumberKeypad[i]);
        }
    }

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
                for(PhoneNumber phoneNumber : contact.phoneNumbers)
                    vcard.addTelephoneNumber(phoneNumber.phoneNumber, intToTelephoneTypeMap.get(phoneNumber.type));
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

    public static String getNumericKeyPadNumberForString(String string){
        String nonAccentedText = replaceAccentedCharactersWithEnglish(string);
        StringBuffer numericString = new StringBuffer();
        for(char c: nonAccentedText.toCharArray()){
            if(Character.isSpaceChar(c)){
                numericString.append(" ");
                continue;
            }
            Integer numericCode = characterToIntegerMappingForKeyboardLayout.get(Character.toUpperCase(c));
            if(numericCode != null)
                numericString.append(characterToIntegerMappingForKeyboardLayout.get(Character.toUpperCase(c)));
        }
        return numericString.toString();
    }

    public static Map<Integer, String> getMobileNumberTypeToTranslatedTextMap(Context context){
        if(mobileNumberTypeToTranslatedText != null)
            return mobileNumberTypeToTranslatedText;
        mobileNumberTypeToTranslatedText = new HashMap<>(4);
        mobileNumberTypeToTranslatedText.put(VCardUtils.telephoneTypeToIntMap.get(TelephoneType.CELL), context.getString(R.string.cell));
        mobileNumberTypeToTranslatedText.put(VCardUtils.telephoneTypeToIntMap.get(TelephoneType.WORK), context.getString(R.string.work));
        mobileNumberTypeToTranslatedText.put(VCardUtils.telephoneTypeToIntMap.get(TelephoneType.FAX), context.getString(R.string.fax));
        mobileNumberTypeToTranslatedText.put(VCardUtils.telephoneTypeToIntMap.get(TelephoneType.HOME), context.getString(R.string.home));
        return mobileNumberTypeToTranslatedText;
    }

    public static String getMobileNumberTypeTranslatedText(int type, Context context){
        if(defaultPhoneNumberType == null) defaultPhoneNumberType = getMobileNumberTypeToTranslatedTextMap(context).get(VCardUtils.defaultPhoneNumberType);
        return getOrDefault(getMobileNumberTypeToTranslatedTextMap(context), type, defaultPhoneNumberType);
    }

    public static int getMobileNumberType(String translatedText, Context context){
        if(translatedTextToMobileNumberType == null) translatedTextToMobileNumberType = U.toMap(U.invert(getMobileNumberTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToMobileNumberType, translatedText, VCardUtils.defaultPhoneNumberType);
    }
}
