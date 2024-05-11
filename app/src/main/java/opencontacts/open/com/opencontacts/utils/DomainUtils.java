package opencontacts.open.com.opencontacts.utils;

import static android.app.Notification.EXTRA_TITLE;
import static android.text.TextUtils.isEmpty;
import static android.widget.Toast.LENGTH_LONG;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.isValidDirectory;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.Common.appendNewLineIfNotEmpty;
import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;
import static opencontacts.open.com.opencontacts.utils.Common.mapIndexes;
import static opencontacts.open.com.opencontacts.utils.Common.replaceAccentedCharactersWithEnglish;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.exportLocation;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getEncryptingContactsKey;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasEncryptingContactsKey;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.hasExportLocation;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.is12HourFormatEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldSortUsingFirstName;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.getVCardFromString;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.CallLog;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import com.github.underscore.U;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardWriter;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.StructuredName;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.orm.CallLogEntry;
import opencontacts.open.com.opencontacts.orm.PhoneNumber;
import opencontacts.open.com.opencontacts.orm.VCardData;

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
    private static HanyuPinyinOutputFormat hanyuPinyinOutputFormat = new HanyuPinyinOutputFormat();
    private static PhoneNumberUtil phoneNumberUtil;
    private static String dateFormatOnlyMonthAndDatePerLocale;
    private static String countryCodeInUpperCase;
    public static String defaultPhoneNumberTypeTranslatedText;
    public static String defaultAddressTypeTranslatedText;
    public static String defaultEmailTypeTranslatedText;
    public static TelephoneType defaultPhoneNumberType = TelephoneType.CELL;
    public static AddressType defaultAddressType = AddressType.HOME;
    public static EmailType defaultEmailType = EmailType.HOME;

    static {
        initializeT9Mapping();
        initPinyinOutputFormat();

    }


    public static void init(Context context) {
        processAsync(() -> {
            phoneNumberUtil = PhoneNumberUtil.createInstance(context);
            countryCodeInUpperCase = AndroidUtils.getCountryCode(context).toUpperCase();
            int countryCallingCode = phoneNumberUtil.getCountryCodeForRegion(countryCodeInUpperCase);
            dateFormatOnlyMonthAndDatePerLocale = computeDateFormat(context);
        });
    }

    private static String computeDateFormat(Context context) {
        char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
        char dateChar = 'd', monthChar = 'M';
        for (char c : dateFormatOrder) {
            if (c == dateChar) return "dd/MM";
            else if (c == monthChar) return "MM/dd";
        }
        return "dd/MM";
    }

    private static void initPinyinOutputFormat() {
        hanyuPinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        hanyuPinyinOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
    }

    private static void initializeT9Mapping() {
        characterToIntegerMappingForKeyboardLayout = new HashMap<>();
        int[] numericsMappingForAlphabetsInNumberKeypad = {2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 9};
        for (int i = 0, charCodeForA = 65; i < 26; i++) {
            characterToIntegerMappingForKeyboardLayout.put((char) (charCodeForA + i), numericsMappingForAlphabetsInNumberKeypad[i]);
        }
        characterToIntegerMappingForKeyboardLayout.put('.', 1);
        characterToIntegerMappingForKeyboardLayout.put(' ', 0);
        for (int i = 0, numericAsciiCodeStartingCode = 48; i < 10; i++)
            characterToIntegerMappingForKeyboardLayout.put((char) (numericAsciiCodeStartingCode + i), i);
    }

    private static void createCallTypeIntToTextMapping(Context context) {
        stringValueOfCallTypeIntToTextMapping = new HashMap<>(3);
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.INCOMING_TYPE), context.getString(R.string.incoming_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.MISSED_TYPE), context.getString(R.string.missed_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.OUTGOING_TYPE), context.getString(R.string.outgoing_call));
        stringValueOfCallTypeIntToTextMapping.put(String.valueOf(CallLog.Calls.REJECTED_TYPE), context.getString(R.string.rejected_call));
    }

    public static void exportAllContacts(Context context) throws Exception {
        if (!hasExportLocation(context) || !isValidDirectory(exportLocation(context), context)) {
            AndroidUtils.runOnMainDelayed(() -> AndroidUtils.toastFromNonUIThread(R.string.no_valid_export_location, LENGTH_LONG, context), 0);
            throw new Exception("Valid export location not set");
        }

        byte[] plainTextExportBytes = getVCFExportBytes(ContactsDataStore.getAllContacts(), ContactsDataStore.getFavorites());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH-mm-ss");
        if (hasEncryptingContactsKey(context))
            exportAsEncryptedZip(context, plainTextExportBytes, simpleDateFormat);
        else exportAsPlainTextVCFFile(plainTextExportBytes, simpleDateFormat, context);
    }

    private static void exportAsPlainTextVCFFile(byte[] plainTextExportBytes, SimpleDateFormat simpleDateFormat, Context context) throws Exception {
        OutputStream fileOutputStream = null;
        try {
            String fileName = "/Contacts_" + simpleDateFormat.format(new Date()) + ".vcf";
            fileOutputStream = getExportFileOutStream(fileName, context);
            fileOutputStream.write(plainTextExportBytes);
        } finally {
            if (fileOutputStream != null) fileOutputStream.close();
        }
    }

    private static void exportAsEncryptedZip(Context context, byte[] plainTextExportBytes, SimpleDateFormat simpleDateFormat) throws Exception {
        OutputStream exportFileOutStream = getExportFileOutStream("Contacts_" + simpleDateFormat.format(new Date()) + ".zip", context);
        ZipUtils.exportZip(getEncryptingContactsKey(context), plainTextExportBytes, exportFileOutStream);
    }

    private static OutputStream getExportFileOutStream(String fileName, Context context) throws Exception {
        DocumentFile documentFile = DocumentFile.fromTreeUri(context, Uri.parse(exportLocation(context)));
        Uri exportFileUri = documentFile.createFile("", fileName).getUri();
        return context.getContentResolver().openOutputStream(exportFileUri);
    }

    private static byte[] getVCFExportBytes(List<Contact> allContacts, List<Contact> favorites) throws IOException {
        ByteArrayOutputStream contactsPlainTextExportStream = new ByteArrayOutputStream();
        VCardWriter vCardWriter = new VCardWriter(contactsPlainTextExportStream, VCardVersion.V4_0);
        vCardWriter.setCaretEncodingEnabled(true);
        StructuredName structuredName = new StructuredName();
        for (Contact contact : allContacts) {
            VCardData vCardData = ContactsDataStore.getVCardData(contact.id);
            if (vCardData == null)
                createVCardAndWrite(vCardWriter, structuredName, contact);
            else {
                try {
                    VCard vcard = getVCardFromString(vCardData.vcardDataAsString);
                    vCardWriter.write(vcard);
                } catch (IOException e) {
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
        for (PhoneNumber phoneNumber : contact.phoneNumbers)
            vcard.addTelephoneNumber(phoneNumber.phoneNumber, TelephoneType.CELL);
        vCardWriter.write(vcard);
    }

    public static String getAllNumericPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return NON_NUMERIC_EXCEPT_PLUS_MATCHING_PATTERN.matcher(phoneNumber).replaceAll(EMPTY_STRING);
    }

    public static boolean matchesNumber(String numericNumber1, String numericNumber2) {
      if(numericNumber1 == null || numericNumber2 == null) return false;
      if(numericNumber1.length() < MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS || numericNumber2.length() < MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS) return numericNumber1.equals(numericNumber2);
      return numericNumber1.contains(numericNumber2) || numericNumber2.contains(numericNumber1);
    }

    private static String getPhoneNumberWithoutCountryCodeAndFormatting(String phoneNumber) {
        try {
            return String.valueOf(U.first(phoneNumberUtil.findNumbers(phoneNumber, countryCodeInUpperCase)).number().getNationalNumber());
        }
        catch(Exception e) {
            System.out.println("fallback to old method of max last digits to match");
            String allNumericPhoneNumber = getAllNumericPhoneNumber(phoneNumber);
            if (allNumericPhoneNumber.length() < MINIMUM_NUMBER_OF_DIGITS_IN_MOST_COUNTRIES_PHONE_NUMBERS) {
              return allNumericPhoneNumber;
            }
            return allNumericPhoneNumber.length() > NUMBER_8 ? allNumericPhoneNumber.substring(allNumericPhoneNumber.length() - NUMBER_8) : allNumericPhoneNumber;
        }
    }

    public static String getSearchablePhoneNumber(String phoneNumber) {
        return getPhoneNumberWithoutCountryCodeAndFormatting(phoneNumber);
    }

    public static List<String> cross(List<String> firstSetOfWords, Set<String> secondSetOfWords) {
        return U.flatten(
            U.map(firstSetOfWords, wordFromFirstSet -> Common.map(secondSetOfWords, wordFromSecondSet -> wordFromFirstSet.concat(" ").concat(wordFromSecondSet)))
        );
    }

    public static String getPinyinTextFromChinese(String text) {
        char[] chineseCharacters = text.toCharArray();
        if (isEmpty(text)) return "";
        List<Set<String>> pinyinFormsForEachCharacter = U.filter(
            mapIndexes(chineseCharacters.length, index -> getPinyinRepresentations(chineseCharacters[index]))
            , it -> it != null);
        if (pinyinFormsForEachCharacter.isEmpty()) return "";
        return U.join(
            U.reduce(pinyinFormsForEachCharacter, DomainUtils::cross, Collections.singletonList("")),
            " ");
    }

    @Nullable
    private static Set<String> getPinyinRepresentations(char chineseCharacter) {
        try {
            String[] pinyinRepresentations = PinyinHelper.toHanyuPinyinStringArray(chineseCharacter, hanyuPinyinOutputFormat);
            if (pinyinRepresentations == null) return null;
            return new LinkedHashSet<>(Arrays.asList(pinyinRepresentations));
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getNumericKeyPadNumberForString(String string) {
        String nonAccentedCharacters = replaceAccentedCharactersWithEnglish(string);
        String finalString = nonAccentedCharacters + " " + getInitialsOfEachWord(nonAccentedCharacters);
        StringBuffer numericString = new StringBuffer();
        for (char c : finalString.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                numericString.append(0);
                continue;
            }
            Integer numericCode = characterToIntegerMappingForKeyboardLayout.get(Character.toUpperCase(c));
            if (numericCode != null)
                numericString.append(characterToIntegerMappingForKeyboardLayout.get(Character.toUpperCase(c)));
        }
        return numericString.toString();
    }

    private static String getInitialsOfEachWord(String text) {
        if (TextUtils.isEmpty(text)) return "";
        return U.join(Common.map(text.split(" "), word -> TextUtils.isEmpty(word) ? "" : word.charAt(0)), "");
    }

    private static Map<TelephoneType, String> getMobileNumberTypeToTranslatedTextMap(Context context) {
        if (mobileNumberTypeToTranslatedText != null)
            return mobileNumberTypeToTranslatedText;
        mobileNumberTypeToTranslatedText = new HashMap<>(4);
        mobileNumberTypeToTranslatedText.put(TelephoneType.CELL, context.getString(R.string.cell));
        mobileNumberTypeToTranslatedText.put(TelephoneType.WORK, context.getString(R.string.work));
        mobileNumberTypeToTranslatedText.put(TelephoneType.FAX, context.getString(R.string.fax));
        mobileNumberTypeToTranslatedText.put(TelephoneType.HOME, context.getString(R.string.home));
        return mobileNumberTypeToTranslatedText;
    }

    public static String getMobileNumberTypeTranslatedText(List<TelephoneType> telephoneTypes, Context context) {
        if (defaultPhoneNumberTypeTranslatedText == null)
            defaultPhoneNumberTypeTranslatedText = getMobileNumberTypeToTranslatedTextMap(context).get(defaultPhoneNumberType);
        if (telephoneTypes.isEmpty()) return defaultPhoneNumberTypeTranslatedText;
        Map<TelephoneType, String> mobileNumberTypeToTranslatedTextMap = getMobileNumberTypeToTranslatedTextMap(context);
        if (telephoneTypes.contains(TelephoneType.FAX)) {
            return mobileNumberTypeToTranslatedTextMap.get(TelephoneType.FAX);
        }
        return getOrDefault(mobileNumberTypeToTranslatedTextMap, U.first(telephoneTypes), U.first(telephoneTypes).getValue());
    }

    public static TelephoneType getMobileNumberType(String translatedText, Context context) {
        if (translatedTextToMobileNumberType == null)
            translatedTextToMobileNumberType = U.toMap(U.invert(getMobileNumberTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToMobileNumberType, translatedText, TelephoneType.get(translatedText));
    }

    private static Map<AddressType, String> getAddressTypeToTranslatedTextMap(Context context) {
        if (addressTypeToTranslatedText != null)
            return addressTypeToTranslatedText;
        addressTypeToTranslatedText = new HashMap<>(2);
        addressTypeToTranslatedText.put(AddressType.HOME, context.getString(R.string.home));
        addressTypeToTranslatedText.put(AddressType.WORK, context.getString(R.string.work));
        return addressTypeToTranslatedText;
    }

    public static String getAddressTypeTranslatedText(Address address, Context context) {
        if (defaultAddressTypeTranslatedText == null)
            defaultAddressTypeTranslatedText = getAddressTypeToTranslatedTextMap(context).get(defaultAddressType);
        List<AddressType> types = address.getTypes();
        if (types.isEmpty()) return defaultAddressTypeTranslatedText;
        return getOrDefault(getAddressTypeToTranslatedTextMap(context), U.first(types), U.first(types).getValue());
    }

    public static AddressType getAddressType(String translatedText, Context context) {
        if (translatedTextToAddressType == null)
            translatedTextToAddressType = U.toMap(U.invert(getAddressTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToAddressType, translatedText, AddressType.get(translatedText));
    }


    private static Map<EmailType, String> getEmailTypeToTranslatedTextMap(Context context) {
        if (emailTypeToTranslatedText != null)
            return emailTypeToTranslatedText;
        emailTypeToTranslatedText = new HashMap<>(2);
        emailTypeToTranslatedText.put(EmailType.HOME, context.getString(R.string.home));
        emailTypeToTranslatedText.put(EmailType.WORK, context.getString(R.string.work));
        return emailTypeToTranslatedText;
    }

    public static String getEmailTypeTranslatedText(List<EmailType> types, Context context) {
        if (defaultEmailType == null)
            defaultEmailTypeTranslatedText = getEmailTypeToTranslatedTextMap(context).get(defaultEmailType);
        if (types.isEmpty()) return defaultEmailTypeTranslatedText;
        return getOrDefault(getEmailTypeToTranslatedTextMap(context), U.first(types), U.first(types).getValue());
    }

    public static EmailType getEmailType(String translatedText, Context context) {
        if (translatedTextToEmailType == null)
            translatedTextToEmailType = U.toMap(U.invert(getEmailTypeToTranslatedTextMap(context)));
        return getOrDefault(translatedTextToEmailType, translatedText, EmailType.get(translatedText));
    }

    @NonNull
    public static SimpleDateFormat getTimestampPattern(Context context) {
        return new SimpleDateFormat(dateFormatOnlyMonthAndDatePerLocale + (is12HourFormatEnabled(context) ? "  hh:mm a" : " HH:mm"), Locale.getDefault());
    }

    @NonNull
    public static SimpleDateFormat getFullDateTimestampPattern(Context context) {
        return new SimpleDateFormat(dateFormatOnlyMonthAndDatePerLocale + (is12HourFormatEnabled(context) ? "/yyyy  hh:mm a" : "/yyyy HH:mm"), Locale.getDefault());
    }

    public static void exportCallLog(Context context) throws Exception {
        if (!hasExportLocation(context) || !isValidDirectory(exportLocation(context), context)) {
            AndroidUtils.runOnMainDelayed(() -> AndroidUtils.toastFromNonUIThread(R.string.no_valid_export_location, LENGTH_LONG, context), 0);
            throw new Exception("Valid export location not set");
        }
        createCallTypeIntToTextMapping(context);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH-mm-ss");
        String fileName = "CallLog_" + simpleDateFormat.format(new Date()) + ".csv";
        ICSVWriter csvWriter = null;
        try {
            //below is a crazy hack for java lambda
            ICSVWriter finalCsvWriter = csvWriter = new CSVWriterBuilder(new OutputStreamWriter(getExportFileOutStream(fileName, context)))
                .build();
            SimpleDateFormat callTimeStampFormat = getFullDateTimestampPattern(context);
            List<CallLogEntry> entireCallLog = CallLogEntry.listAll(CallLogEntry.class);
            writeCallLogCSVHeader(csvWriter);
            U.forEach(entireCallLog, callLogEntry -> writeCallLogEntryToFile(callLogEntry, callTimeStampFormat, finalCsvWriter));
        } finally {
            if (csvWriter != null) csvWriter.flushQuietly();
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

    public static boolean addContactAsShortcut(Contact contact, Context context) {
        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(context, contact.id + "")
            .setIntent(AndroidUtils.getIntentToCall(contact.primaryPhoneNumber.phoneNumber, context))
            .setShortLabel(contact.name)
            .setLongLabel(contact.name)
            .build();
        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null);
    }

    public static List<Contact> sortContactsBasedOnName(Collection<Contact> contacts, Context context) {
        List<Contact> newContactsList = U.copyOf(contacts);
        Collections.sort(newContactsList, getContactComparatorBasedOnName(context));
        return newContactsList;
    }

    public static String getLastNameOrFullInCaseEmpty(Contact contact) {
        return contact.lastName == null || isEmpty(contact.lastName.trim()) ? contact.name : contact.lastName;
    }

    public static int comparatorToMoveContactsWithoutPhoneNumbersToTheBottom(Contact cont1, Contact cont2){
        int phoneNumbersInCont1 = cont1.phoneNumbers.size();
        int phoneNumbersInCont2 = cont2.phoneNumbers.size();
//        not easily readable code below for comparator performance, reduces number of checks/lines
        if(phoneNumbersInCont1 == 0){
            if(phoneNumbersInCont2 == 0) return 0;
            else return 1;
        } else if(phoneNumbersInCont2 == 0) return -1;
        else return 0;
//        less performant but readable example below
//        if(phoneNumbersInCont1 == 0 && phoneNumbersInCont2 == 0) return 0;
//        if(phoneNumbersInCont1 == 0) return 1;
//        if(phoneNumbersInCont2 == 0) return -1;
//        return 0;
    }
    @NonNull
    public static Comparator<Contact> getContactComparatorBasedOnName(Context context) {
        if (shouldSortUsingFirstName(context))
            return (contact1, contact2) -> {
                int noPhonenumbersComparisonResult = comparatorToMoveContactsWithoutPhoneNumbersToTheBottom(contact1, contact2);
                return noPhonenumbersComparisonResult == 0 ? contact1.name.compareToIgnoreCase(contact2.name) : noPhonenumbersComparisonResult;
            };
        else
            return (contact1, contact2) -> {
                int noContactsComparisonResult = comparatorToMoveContactsWithoutPhoneNumbersToTheBottom(contact1, contact2);
                return noContactsComparisonResult == 0 ? getLastNameOrFullInCaseEmpty(contact1).compareToIgnoreCase(getLastNameOrFullInCaseEmpty(contact2)) : noContactsComparisonResult;
            };
    }

    @NonNull
    public static Comparator<Contact> getContactComparatorBasedOnLastAccessed() {
        return (contact1, contact2) -> {
            String lastAccessedDate1 = contact1.lastAccessed;
            String lastAccessedDate2 = contact2.lastAccessed;
            if (lastAccessedDate1 == null && lastAccessedDate2 == null)
                return 0;
            else if (lastAccessedDate1 == null)
                return 1;
            else if (lastAccessedDate2 == null)
                return -1;
            else
                return lastAccessedDate2.compareTo(lastAccessedDate1);
        };
    }

    public static List<Contact> filterContactsBasedOnT9Text(CharSequence t9Text, List<Contact> contacts) {
        ArrayList<Contact> filteredContacts = new ArrayList<>();
        for (Contact contact : contacts) {
            if (contact.t9Text == null) {
                contact.setT9Text();
            }
            if (contact.t9Text.contains(t9Text.toString().toUpperCase())) {
                filteredContacts.add(contact);
            }
        }
        return filteredContacts;
    }

    public static String formatAddressToAMultiLineString(Address address, Context context) {
        StringBuffer addressBuffer = new StringBuffer(6)
            .append(appendNewLineIfNotEmpty(address.getPoBox()))
            .append(appendNewLineIfNotEmpty(address.getStreetAddress()))
            .append(appendNewLineIfNotEmpty(address.getExtendedAddress()))
            .append(appendNewLineIfNotEmpty(address.getPostalCode()))
            .append(appendNewLineIfNotEmpty(address.getLocality()))
            .append(appendNewLineIfNotEmpty(address.getRegion()))
            .append(appendNewLineIfNotEmpty(address.getCountry()));
        return addressBuffer.toString();
    }

    public static void deleteAllContacts(Context context) {
        ContactsDataStore.deleteAllContacts(context);
        SharedPreferencesUtils.removeSyncProgress(context);
    }

    public static void shareContact(long contactId, Context context) {
        AndroidUtils.shareContact(ContactsDataStore.getVCardData(contactId).vcardDataAsString, context);
    }

    public static String getMissedcallNotificationTitle(Context context) {
        return context.getString(R.string.missed_call);
    }

    public static void removeAnyMissedCallNotifications(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        U.chain(notificationManager.getActiveNotifications())
            .filter(notification -> isMissedCallNotification(context, notification))
            .forEach(notification -> notificationManager.cancel(notification.getId()));
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isMissedCallNotification(Context context, StatusBarNotification notification) {
        return getMissedcallNotificationTitle(context).equals(notification.getNotification().extras.getString(EXTRA_TITLE));
    }

    public static void shareContactAsText(long contactId, Context context) {
        Contact contactToShare = ContactsDataStore.getContactWithId(contactId);
        StringBuffer contactAsText = new StringBuffer();
        contactAsText.append(String.format("%s: %s\n", context.getString(R.string.name), contactToShare.name));
        U.chain(contactToShare.phoneNumbers)
            .map(phoneNumber -> phoneNumber.phoneNumber)
            .forEach(numberAsString -> contactAsText.append(numberAsString + "\n"));
        AndroidUtils.shareText(contactAsText.toString(), context);
    }
}
