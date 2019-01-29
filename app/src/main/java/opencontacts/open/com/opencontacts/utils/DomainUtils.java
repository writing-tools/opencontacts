package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.github.underscore.U;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardReader;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.StructuredName;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;
import static opencontacts.open.com.opencontacts.utils.Common.replaceAccentedCharactersWithEnglish;

/**
 * Created by sultanm on 7/22/17.
 */

public class DomainUtils {
    public static final String EMPTY_STRING = "";
    public static final Pattern NON_NUMERIC_MATCHING_PATTERN = Pattern.compile("[^0-9]");
    public static final int MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS = 7;
    public static final int NUMBER_8 = 8;

    private static Map<Character, Integer> characterToIntegerMappingForKeyboardLayout;
    private static Map<TelephoneType, String> mobileNumberTypeToTranslatedText;
    private static Map<String, TelephoneType> translatedTextToMobileNumberType;
    private static Map<AddressType, String> addressTypeToTranslatedText;
    private static Map<String, AddressType> translatedTextToAddressType;
    private static Map<EmailType, String> emailTypeToTranslatedText;
    private static Map<String, EmailType> translatedTextToEmailType;
    private static String defaultPhoneNumberTypeTranslatedText;
    private static String defaultAddressTypeTranslatedText;
    private static String defaultEmailTypeTranslatedText;
    public static TelephoneType defaultPhoneNumberType = TelephoneType.CELL;
    public static AddressType defaultAddressType = AddressType.HOME;
    public static EmailType defaultEmailType = EmailType.HOME;

    static {
        characterToIntegerMappingForKeyboardLayout = new HashMap<>();
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
                VCardData vCardData = ContactsDataStore.getVCardData(contact.id);
                if(vCardData == null)
                    createVCardAndWrite(vCardWriter, structuredName, contact);
                else {
                    try{
                        vCardWriter.write(new VCardReader(vCardData.vcardDataAsString).readNext());
                    }
                    catch (IOException e){
                        e.printStackTrace();
                        createVCardAndWrite(vCardWriter, structuredName, contact);
                    }
                }
            }
        }
        finally {
            if(vCardWriter != null)
                vCardWriter.close();
        }

    }

    private static void createVCardAndWrite(VCardWriter vCardWriter, StructuredName structuredName, Contact contact) throws IOException {
        VCard vcard = new VCard();
        structuredName.setGiven(contact.firstName);
        structuredName.setFamily(contact.lastName);
        vcard.setStructuredName(structuredName);
        for(PhoneNumber phoneNumber : contact.phoneNumbers)
            vcard.addTelephoneNumber(phoneNumber.phoneNumber, TelephoneType.CELL);
        vCardWriter.write(vcard);
    }

    public static String getAllNumericPhoneNumber(String phoneNumber) {
        return NON_NUMERIC_MATCHING_PATTERN.matcher(phoneNumber).replaceAll(EMPTY_STRING);
    }

    @Nullable
    public static String getSearchablePhoneNumber(String phoneNumber) {
        String allNumericPhoneNumber = getAllNumericPhoneNumber(phoneNumber);
        if(allNumericPhoneNumber.length() < MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS)
            return null;
        return allNumericPhoneNumber.length() > NUMBER_8 ? allNumericPhoneNumber.substring(allNumericPhoneNumber.length() - NUMBER_8) : allNumericPhoneNumber;
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

    private static Map<TelephoneType, String> getMobileNumberTypeToTranslatedTextMap(Context context){
        if(mobileNumberTypeToTranslatedText != null)
            return mobileNumberTypeToTranslatedText;
        mobileNumberTypeToTranslatedText = new HashMap<>(4);
        mobileNumberTypeToTranslatedText.put(TelephoneType.CELL, context.getString(R.string.cell));
        mobileNumberTypeToTranslatedText.put(TelephoneType.WORK, context.getString(R.string.work));
        mobileNumberTypeToTranslatedText.put(TelephoneType.FAX, context.getString(R.string.fax));
        mobileNumberTypeToTranslatedText.put(TelephoneType.HOME, context.getString(R.string.home));
        return mobileNumberTypeToTranslatedText;
    }

    public static String getMobileNumberTypeTranslatedText(List<TelephoneType> telephoneTypes, Context context){
        if(defaultPhoneNumberTypeTranslatedText == null) defaultPhoneNumberTypeTranslatedText = getMobileNumberTypeToTranslatedTextMap(context).get(defaultPhoneNumberType);
        if(telephoneTypes.isEmpty()) return defaultPhoneNumberTypeTranslatedText;
        Map<TelephoneType, String> mobileNumberTypeToTranslatedTextMap = getMobileNumberTypeToTranslatedTextMap(context);
        if(telephoneTypes.contains(TelephoneType.FAX)) {
            return mobileNumberTypeToTranslatedTextMap.get(TelephoneType.FAX);
        }
        return getOrDefault(mobileNumberTypeToTranslatedTextMap, U.first(telephoneTypes), defaultPhoneNumberTypeTranslatedText);
    }

    public static TelephoneType getMobileNumberType(String translatedText, Context context){
        if(translatedTextToMobileNumberType == null) translatedTextToMobileNumberType = U.toMap(U.invert(getMobileNumberTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToMobileNumberType, translatedText, defaultPhoneNumberType);
    }

    private static Map<AddressType, String> getAddressTypeToTranslatedTextMap(Context context){
        if(addressTypeToTranslatedText != null)
            return addressTypeToTranslatedText;
        addressTypeToTranslatedText = new HashMap<>(2);
        addressTypeToTranslatedText.put(AddressType.HOME, context.getString(R.string.home));
        addressTypeToTranslatedText.put(AddressType.WORK, context.getString(R.string.work));
        return addressTypeToTranslatedText;
    }

    public static String getAddressTypeTranslatedText(List<AddressType> types, Context context){
        if(defaultAddressTypeTranslatedText == null) defaultAddressTypeTranslatedText = getAddressTypeToTranslatedTextMap(context).get(defaultAddressType);
        if(types.isEmpty()) return defaultAddressTypeTranslatedText;
        return getOrDefault(getAddressTypeToTranslatedTextMap(context), U.first(types), defaultAddressTypeTranslatedText);
    }

    public static AddressType getAddressType(String translatedText, Context context){
        if(translatedTextToAddressType == null) translatedTextToAddressType = U.toMap(U.invert(getAddressTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToAddressType, translatedText, defaultAddressType);
    }


    private static Map<EmailType, String> getEmailTypeToTranslatedTextMap(Context context){
        if(emailTypeToTranslatedText != null)
            return emailTypeToTranslatedText;
        emailTypeToTranslatedText = new HashMap<>(2);
        emailTypeToTranslatedText.put(EmailType.HOME, context.getString(R.string.home));
        emailTypeToTranslatedText.put(EmailType.WORK, context.getString(R.string.work));
        return emailTypeToTranslatedText;
    }

    public static String getEmailTypeTranslatedText(List<EmailType> types, Context context){
        if(defaultEmailType == null) defaultEmailTypeTranslatedText = getEmailTypeToTranslatedTextMap(context).get(defaultEmailType);
        if(types.isEmpty()) return defaultEmailTypeTranslatedText;
        return getOrDefault(getEmailTypeToTranslatedTextMap(context), U.first(types), defaultEmailTypeTranslatedText);
    }

    public static EmailType getEmailType(String translatedText, Context context){
        if(translatedTextToEmailType == null) translatedTextToEmailType = U.toMap(U.invert(getEmailTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToEmailType, translatedText, defaultEmailType);
    }

    public static boolean isStillOnOldDB() {
        return VCardData.count(VCardData.class) == 0 && opencontacts.open.com.opencontacts.orm.Contact.count(opencontacts.open.com.opencontacts.orm.Contact.class) > 0;
    }
}
