package opencontacts.open.com.opencontacts;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.github.underscore.Tuple;
import com.github.underscore.U;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDBHelper;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.CardDavUtils;
import opencontacts.open.com.opencontacts.utils.Triplet;

import static android.widget.Toast.LENGTH_SHORT;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_CREATED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_DELETED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_NONE;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_UPDATED;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.areNotValidDetails;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.downloadAddressBook;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.figureOutAddressBookUrl;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.getChangesSinceSyncToken;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.ADDRESSBOOK_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.BASE_SYNC_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SYNC_TOKEN_SHARED_PREF_KEY;

public class CardDavSyncActivity extends AppCompatActivity {

    private String savedBaseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_dav_sync);
        savedBaseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        ((TextInputEditText) findViewById(R.id.url)).setText(savedBaseUrl);
        new AlertDialog.Builder(this)
                .setTitle("Warning!")
                .setMessage("Sync is experimental yet, please use this only if you have read the issue status in gitlab")
                .setPositiveButton("Okay", null)
                .show();
    }

    public void sync(View view) {
        String url = ((TextInputEditText) findViewById(R.id.url)).getText().toString();
        String username = ((TextInputEditText) findViewById(R.id.username)).getText().toString();
        String password = ((TextInputEditText) findViewById(R.id.password)).getText().toString();
        AndroidUtils.hideSoftKeyboard(findViewById(R.id.username), this);
        if(TextUtils.isEmpty(username) || TextUtils.isEmpty(password)){
            Toast.makeText(this, R.string.input_username_and_password, LENGTH_SHORT).show();
            return;
        }
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("Please wait...")
                .show();
        processAsync(() -> {
            sync(url, username, password);
            runOnUiThread(loadingDialog::dismiss);
        });
    }

    private void sync(String urlFromView, String username, String password) {
        if(areNotValidDetails(urlFromView, username, password)){
            toastFromNonUIThread(R.string.invalid_username_or_password_or_url, LENGTH_SHORT, this);
            return;
        }
        String addressBookUrl = getStringFromPreferences(ADDRESSBOOK_URL_SHARED_PREFS_KEY, this);
        if(addressBookUrl == null || !urlFromView.equals(savedBaseUrl))
            addressBookUrl = figureOutAddressBookUrl(urlFromView, username);
        if (addressBookUrl == null) {
            showError(R.string.no_addressbook_found);
            return;
        }
        updatePreference(ADDRESSBOOK_URL_SHARED_PREFS_KEY, addressBookUrl, this);
        updatePreference(BASE_SYNC_URL_SHARED_PREFS_KEY, urlFromView, this);
        String syncToken = getStringFromPreferences(SYNC_TOKEN_SHARED_PREF_KEY, this);
        if(TextUtils.isEmpty(syncToken))
            fullSync(urlFromView, username, password, addressBookUrl);
        else
            partialSync(urlFromView, username, password, addressBookUrl, syncToken);
        ContactsDataStore.refreshStoreAsync();
        toastFromNonUIThread(R.string.sync_complete, Toast.LENGTH_LONG, this);
        finish();
    }

    private void partialSync(String baseUrl, String username, String password, String addressBookUrl, String syncToken) {
        Pair<List<String>, List<String>> pairOfListOfAddedUpdatedAndDeleted = getChangesSinceSyncToken(syncToken, baseUrl, addressBookUrl);
        deleteContactsLocallyAsTheyWereDeletedOnServer(pairOfListOfAddedUpdatedAndDeleted.second);
        syncChanges(baseUrl, username, password, addressBookUrl, CardDavUtils.getVcardsWithHrefs(pairOfListOfAddedUpdatedAndDeleted.first, baseUrl, addressBookUrl));
    }

    private void deleteContactsLocallyAsTheyWereDeletedOnServer(List<String> hrefsDeleted) {
        if(hrefsDeleted.isEmpty()) return;
        List<VCardData> allVCardDataList = VCardData.listAll(VCardData.class);
        Map<String, VCardData> allVCardsAsHREFMap = getAllVCardsAsHREFMap(allVCardDataList);

        U.forEach(hrefsDeleted, href -> {
            VCardData vCardData = allVCardsAsHREFMap.get(href);
            if(vCardData == null) return;
            ContactsDataStore.removeContact(vCardData.contact.getId());
            vCardData.delete();
        });
    }

    private void fullSync(String urlFromView, String username, String password, String addressBookUrl) {
        List<Triplet<String, String, VCard>> hrefEtagAndVCardList = downloadAddressBook(urlFromView + addressBookUrl);
        syncChanges(urlFromView, username, password, addressBookUrl, hrefEtagAndVCardList);
    }

    private void syncChanges(String urlFromView, String username, String password, String addressBookUrl, List<Triplet<String, String, VCard>> hrefEtagAndVCardList) {
        List<VCardData> allVCardDataList = VCardData.listAll(VCardData.class);
        if(!hrefEtagAndVCardList.isEmpty()) updateLocal(hrefEtagAndVCardList, allVCardDataList);
        updateServer(allVCardDataList, true, username, password, addressBookUrl);
        updatePreference(SYNC_TOKEN_SHARED_PREF_KEY, CardDavUtils.getSyncToken(urlFromView, addressBookUrl), this);
    }

    private void updateLocal(List<Triplet<String, String, VCard>> hrefEtagAndVCardList, List<VCardData> allVCardDataList) {
        Map<String, VCardData> allVCardsAsHREFMap = getAllVCardsAsHREFMap(allVCardDataList);
        U.forEach(hrefEtagAndVCardList, hrefEtagAndVCard -> {
            String href = hrefEtagAndVCard.x;
            if(allVCardsAsHREFMap.containsKey(href)) {
                VCardData vcardDataFromDB = allVCardsAsHREFMap.get(href);
                if(hrefEtagAndVCard.y.equals(vcardDataFromDB.etag)) return;
                processExistingVCard(hrefEtagAndVCard, vcardDataFromDB);
            }
            else createContact(hrefEtagAndVCard);
        });
    }

    private void createContact(Triplet<String, String, VCard> hrefEtagAndVCard) {
        Contact contact = ContactsDBHelper.addContact(hrefEtagAndVCard, this);
        VCardData vCardData = ContactsDBHelper.getVCard(contact.getId());
        vCardData.status = STATUS_NONE;
        vCardData.save();
    }

    private void updateServer(List<VCardData> allVCardDataList, boolean oldServer, String username, String password, String addressBookUrl) {
        String baseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        U.forEach(allVCardDataList, vcardData -> {
            switch (vcardData.status){
                case STATUS_NONE:
                    if(oldServer) break;
                case STATUS_CREATED:
                    Pair<String, String> hrefAndEtag = CardDavUtils.createContactOnServer(vcardData, addressBookUrl, baseUrl);
                    if(hrefAndEtag == null) break;
                    vcardData.href = hrefAndEtag.first;
                    vcardData.etag = hrefAndEtag.second;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_UPDATED:
                    String etag= CardDavUtils.updateContactOnServer(vcardData, baseUrl);
                    if(etag == null) break;
                    vcardData.etag = etag;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_DELETED:
                    boolean deletionOnServerSuccessful = CardDavUtils.deleteVCardOnServer(vcardData, baseUrl);
                    if(deletionOnServerSuccessful) vcardData.delete();
                    break;
            }
        });
    }

    private void processExistingVCard(Triplet<String, String, VCard> hrefEtagAndVCard, VCardData vcardDataFromDB) {
        if(vcardDataFromDB.status == VCardData.STATUS_DELETED) {
            vcardDataFromDB.href = hrefEtagAndVCard.x;
            return;
        }
        ContactsDBHelper.merge(hrefEtagAndVCard, vcardDataFromDB, this);
    }

    private void showError(int messageRes) {
        runOnUiThread(() -> ((AppCompatTextView) findViewById(R.id.error)).setText(messageRes));
    }


    private static Map<String, VCardData> getAllVCardsAsUIDMap(List<VCardData> allVCardDataList){
        if(allVCardDataList.isEmpty())
            return new HashMap<>(0);

        List<Tuple<String, VCardData>> listOfTuples = U.map(allVCardDataList, vCardData -> new Tuple<>(vCardData.uid, vCardData));
        return U.toMap(listOfTuples);
    }

    private static Map<String, VCardData> getAllVCardsAsHREFMap(List<VCardData> allVCardDataList){
        if(allVCardDataList.isEmpty())
            return new HashMap<>(0);

        return U.reduce(allVCardDataList, (hashMap, VCardData) -> {
            if(VCardData.href == null)
                return hashMap;
            hashMap.put(VCardData.href, VCardData);
            return hashMap;
        }, new HashMap<>());
    }
}
