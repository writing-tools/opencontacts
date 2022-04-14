// ContactsDataStoreService.aidl
package opencontacts.open.com.opencontacts;

// Declare any non-default types here with import statements

interface ContactsDataStoreService {
    // standard vcard for each contact
    List<String> getAllVCards();
    // more like csv file
    String getNameAndPhoneNumbers();
}
