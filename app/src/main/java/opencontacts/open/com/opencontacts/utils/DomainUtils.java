package opencontacts.open.com.opencontacts.utils;

import android.content.Context;
import android.os.Environment;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.pm.ShortcutInfoCompat;
import android.support.v4.content.pm.ShortcutManagerCompat;

import com.github.underscore.U;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;
import static opencontacts.open.com.opencontacts.utils.Common.replaceAccentedCharactersWithEnglish;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getEncryptingContactsKey;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasEncryptingContactsKey;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.is12HourFormatEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldSortUsingFirstName;

/**
 * Created by sultanm on 7/22/17.
 */

public class DomainUtils {
    public static final String EMPTY_STRING = "";
    public static final Pattern NON_NUMERIC_EXCEPT_PLUS_MATCHING_PATTERN = Pattern.compile("[^0-9+]");
    public static final int MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS = 7;
    public static final int NUMBER_8 = 8;

    private static Map<Character, Integer> characterToIntegerMappingForKeyboardLayout;
    private static Map<TelephoneType, String> mobileNumberTypeToTranslatedText;
    private static Map<String, TelephoneType> translatedTextToMobileNumberType;
    private static Map<AddressType, String> addressTypeToTranslatedText;
    private static Map<String, AddressType> translatedTextToAddressType;
    private static Map<EmailType, String> emailTypeToTranslatedText;
    private static Map<String, EmailType> translatedTextToEmailType;
    private static Map<String, String> stringValueOfCallTypeIntToTextMapping;
    public static String defaultPhoneNumberTypeTranslatedText;
    public static String defaultAddressTypeTranslatedText;
    public static String defaultEmailTypeTranslatedText;
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

    private static void createCallTypeIntToTextMapping(Context context){
        stringValueOfCallTypeIntToTextMapping = new HashMap<>(3);
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.INCOMING_TYPE), context.getString(R.string.incoming_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.MISSED_TYPE), context.getString(R.string.missed_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.OUTGOING_TYPE), context.getString(R.string.outgoing_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.REJECTED_TYPE), context.getString(R.string.rejected_call));
    }
    public static void exportAllContacts(Context context) throws IOException {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            AndroidUtils.showAlert(context, context.getString(R.string.error), context.getString(R.string.storage_not_mounted));
            return;
        }

        byte[] plainTextExportBytes = getVCFExportBytes(ContactsDataStore.getAllContacts(), ContactsDataStore.getFavorites());
        createOpenContactsDirectoryIfItDoesNotExist();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH-mm-ss");
        if (hasEncryptingContactsKey(context)) exportAsEncryptedZip(context, plainTextExportBytes, simpleDateFormat);
        else exportAsPlainTextVCFFile(plainTextExportBytes, simpleDateFormat);
    }

    private static void createOpenContactsDirectoryIfItDoesNotExist() {
        File openContactsDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenContacts");
        if(openContactsDirectory.exists()) return;
        openContactsDirectory.mkdir();
    }

    private static void exportAsPlainTextVCFFile(byte[] plainTextExportBytes, SimpleDateFormat simpleDateFormat) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenContacts", "Contacts_" + simpleDateFormat.format(new Date()) + ".vcf");
            file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(plainTextExportBytes);
        }
        finally {
            if(fileOutputStream != null) fileOutputStream.close();
        }
    }

    private static void exportAsEncryptedZip(Context context, byte[] plainTextExportBytes, SimpleDateFormat simpleDateFormat) throws IOException {
        String exportFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenContacts/Contacts_" + simpleDateFormat.format(new Date()) + ".zip";
        ZipUtils.exportZip(getEncryptingContactsKey(context), plainTextExportBytes, exportFilePath);
    }

    private static byte[] getVCFExportBytes(List<Contact> allContacts, List<Contact> favorites) throws IOException {
        ByteArrayOutputStream contactsPlainTextExportStream = new ByteArrayOutputStream();
        VCardWriter vCardWriter = new VCardWriter(contactsPlainTextExportStream, VCardVersion.V4_0);
        StructuredName structuredName = new StructuredName();
        for( Contact contact : allContacts){
            VCardData vCardData = ContactsDataStore.getVCardData(contact.id);
            if(vCardData == null)
                createVCardAndWrite(vCardWriter, structuredName, contact);
            else {
                try{
                    VCard vcard = new VCardReader(vCardData.vcardDataAsString).readNext();
                    vcard.setExtendedProperty("X-FAVORITE", String.valueOf(favorites.contains(contact)));
                    vCardWriter.write(vcard);
                }
                catch (IOException e){
                    e.printStackTrace();
                    createVCardAndWrite(vCardWriter, structuredName, contact);
                }
            }
        }
        vCardWriter.flush();
        vCardWriter.close();
        return contactsPlainTextExportStream.toByteArray();
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
        if(phoneNumber == null) return "";
        return NON_NUMERIC_EXCEPT_PLUS_MATCHING_PATTERN.matcher(phoneNumber).replaceAll(EMPTY_STRING);
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
        return getOrDefault(mobileNumberTypeToTranslatedTextMap, U.first(telephoneTypes), U.first(telephoneTypes).getValue());
    }

    public static TelephoneType getMobileNumberType(String translatedText, Context context){
        if(translatedTextToMobileNumberType == null) translatedTextToMobileNumberType = U.toMap(U.invert(getMobileNumberTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToMobileNumberType, translatedText, TelephoneType.get(translatedText));
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
        return getOrDefault(getAddressTypeToTranslatedTextMap(context), U.first(types), U.first(types).getValue());
    }

    public static AddressType getAddressType(String translatedText, Context context){
        if(translatedTextToAddressType == null) translatedTextToAddressType = U.toMap(U.invert(getAddressTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToAddressType, translatedText, AddressType.get(translatedText));
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
        return getOrDefault(getEmailTypeToTranslatedTextMap(context), U.first(types), U.first(types).getValue());
    }

    public static EmailType getEmailType(String translatedText, Context context){
        if(translatedTextToEmailType == null) translatedTextToEmailType = U.toMap(U.invert(getEmailTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToEmailType, translatedText, EmailType.get(translatedText));
    }

    @NonNull
    public static SimpleDateFormat getTimestampPattern(Context context) {
        return new SimpleDateFormat(is12HourFormatEnabled(context) ? "dd/MM  hh:mm a" : "dd/MM HH:mm", Locale.getDefault());
    }

    public static void exportCallLog(Context context) throws IOException{
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            AndroidUtils.showAlert(context, context.getString(R.string.error), context.getString(R.string.storage_not_mounted));
            return;
        }
        createCallTypeIntToTextMapping(context);
        createOpenContactsDirectoryIfItDoesNotExist();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH-mm-ss");
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/OpenContacts", "CallLog_" + simpleDateFormat.format(new Date()) + ".csv");
        ICSVWriter csvWriter = null;
        try{
            file.createNewFile();
            //below is a crazy hack for java lambda
            ICSVWriter finalCsvWriter = csvWriter = new CSVWriterBuilder(new FileWriter(file))
                    .build();
            SimpleDateFormat callTimeStampFormat = new SimpleDateFormat(is12HourFormatEnabled(context) ? "dd/MM/yyyy  hh:mm a" : "dd/MM/yyyy HH:mm", Locale.getDefault());
            List<CallLogEntry> entireCallLog = CallLogEntry.listAll(CallLogEntry.class);
            writeCallLogCSVHeader(csvWriter);
            U.forEach(entireCallLog, callLogEntry -> writeCallLogEntryToFile(callLogEntry, callTimeStampFormat, finalCsvWriter));
        }
        finally {
            if(csvWriter != null) csvWriter.flushQuietly();
        }

    }

    private static void writeCallLogCSVHeader(ICSVWriter csvWriter) {
        csvWriter.writeNext(new String[]{"Name", "Phone number", "Call Type",
                "Timestamp", "Duration", "Sim used"
        });
    }

    private static void writeCallLogEntryToFile(CallLogEntry callLogEntry, SimpleDateFormat callTimeStampFormat, ICSVWriter writer) {
        writer.writeNext(new String[]{
                callLogEntry.name, callLogEntry.getPhoneNumber(),
                getOrDefault(stringValueOfCallTypeIntToTextMapping, callLogEntry.getCallType(), callLogEntry.getCallType()),
                callTimeStampFormat.format(new Date(Long.parseLong(callLogEntry.getDate()))),
                Common.getDurationInMinsAndSecs(Integer.valueOf(callLogEntry.getDuration())),
                String.valueOf(callLogEntry.getSimId())
        });
    }

    public static boolean addContactAsShortcut(Contact contact, Context context){
        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, contact.id + "")
                .setIntent(AndroidUtils.getIntentToCall(contact.primaryPhoneNumber.phoneNumber, context))
                .setShortLabel(contact.name)
                .setLongLabel(contact.name)
                .build();
        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null);
    }

    public static List<Contact> sortContacts(List<Contact> contacts, Context context) {
        List<Contact> newContactsList = U.copyOf(contacts);
        Collections.sort(newContactsList, getContactComparator(context));
        return newContactsList;
    }

    @NonNull
    public static Comparator<Contact> getContactComparator(Context context) {
        if(shouldSortUsingFirstName(context))
            return (contact1, contact2) -> contact1.firstName.compareToIgnoreCase(contact2.firstName);
        else
            return (contact1, contact2) -> contact1.lastName.compareToIgnoreCase(contact2.lastName);
    }

    public static List<Contact> filterContactsBasedOnT9Text(CharSequence t9Text, List<Contact> contacts) {
        ArrayList<Contact> filteredContacts = new ArrayList<>();
        for (Contact contact : contacts) {
            if(contact.t9Text == null){
                contact.setT9Text();
            }
            if (contact.t9Text.contains(t9Text.toString().toUpperCase())) {
                filteredContacts.add(contact);
            }
        }
        return filteredContacts;
    }
}
