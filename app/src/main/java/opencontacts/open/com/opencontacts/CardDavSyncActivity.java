package opencontacts.open.com.opencontacts;

import static android.widget.Toast.LENGTH_SHORT;
import static opencontacts.open.com.opencontacts.data.datastore.ContactGroupsDataStore.invalidateGroups;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsSyncHelper.merge;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsSyncHelper.replaceContactWithServers;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_CREATED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_DELETED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_NONE;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_UPDATED;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.blockUIUntil;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.carddavServersCheekyStuffMap;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.otherServerConstants;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.areNotValidDetails;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.downloadAddressBook;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.figureOutAddressBookUrl;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.getBaseURL;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.getChangesSinceSyncToken;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.ADDRESSBOOK_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.BASE_SYNC_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.CARD_DAV_SERVER_TYPE_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SYNC_TOKEN_SHARED_PREF_KEY;

import android.os.Bundle;
import com.google.android.material.textfield.TextInputEditText;
import androidx.core.util.Pair;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.underscore.Tuple;
import com.github.underscore.U;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDBHelper;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.orm.Contact;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.CardDavUtils;
import opencontacts.open.com.opencontacts.utils.CheekyCarddavServerStuff;
import opencontacts.open.com.opencontacts.utils.Triplet;

public class CardDavSyncActivity extends AppCompatActivity {

    private String savedBaseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_dav_sync);
        savedBaseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        AppCompatSpinner carddavServerTypeSpinner = findViewById(R.id.carddav_server_type);
        TextInputEditText urlTextInputEditTextView = findViewById(R.id.url);
        setupServerTypeDropdown(carddavServerTypeSpinner, urlTextInputEditTextView);
        ((TextInputEditText) findViewById(R.id.url)).setText(savedBaseUrl);
        new AlertDialog.Builder(this)
            .setTitle("Warning!")
            .setMessage("Sync is experimental yet, please use this only if you have read the issue status in gitlab")
            .setPositiveButton("Okay", null)
            .show();
    }

    private void setupServerTypeDropdown(AppCompatSpinner carddavServerTypeSpinner, TextInputEditText urlTextInputEditTextView) {
        Set<String> cardDavServerNames = carddavServersCheekyStuffMap.keySet();
        ArrayAdapter<Object> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cardDavServerNames.toArray());
        spinnerAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        carddavServerTypeSpinner.setAdapter(spinnerAdapter);
        carddavServerTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object selectedCarddavServer = carddavServerTypeSpinner.getSelectedItem();
                String defaultUrl = carddavServersCheekyStuffMap.get(selectedCarddavServer).defaultUrl;
                if (!defaultUrl.equals("")) urlTextInputEditTextView.setText(defaultUrl);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int lastSavedCarddavServerTypeIndex = U.indexOf(cardDavServerNames.toArray(),
            getStringFromPreferences(CARD_DAV_SERVER_TYPE_SHARED_PREFS_KEY, otherServerConstants.name, this));
        carddavServerTypeSpinner.setSelection(lastSavedCarddavServerTypeIndex);
    }

    public void sync(View view) {
        String url = ((TextInputEditText) findViewById(R.id.url)).getText().toString();
        String username = ((TextInputEditText) findViewById(R.id.username)).getText().toString();
        String password = ((TextInputEditText) findViewById(R.id.password)).getText().toString();
        boolean shouldIgnoreSSL = ((SwitchCompat) findViewById(R.id.ignore_ssl)).isChecked();
        String carddavServerType = (String) ((AppCompatSpinner) findViewById(R.id.carddav_server_type)).getSelectedItem();
        AndroidUtils.hideSoftKeyboard(findViewById(R.id.username), this);
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.input_username_and_password, LENGTH_SHORT).show();
            return;
        }
        blockUIUntil(() -> sync(url, username, password, shouldIgnoreSSL, carddavServersCheekyStuffMap.get(carddavServerType)), this);
    }

    private void sync(String urlFromView, String username, String password, boolean shouldIgnoreSSL, CheekyCarddavServerStuff carddavServerType) {
        boolean hasServerChangedFromEarlier = !urlFromView.equals(savedBaseUrl);

        String addressBookUrl = getStringFromPreferences(ADDRESSBOOK_URL_SHARED_PREFS_KEY, this);
        if (areNotValidDetails(urlFromView, username, password, shouldIgnoreSSL, carddavServerType, addressBookUrl)) {
            toastFromNonUIThread(R.string.invalid_username_or_password_or_url, LENGTH_SHORT, this);
            return;
        }
        if (addressBookUrl == null || hasServerChangedFromEarlier)
            addressBookUrl = figureOutAddressBookUrl(urlFromView, username, carddavServerType, this);
        if (addressBookUrl == null) {
            showError(R.string.no_addressbook_found);
            return;
        }
        updatePreference(ADDRESSBOOK_URL_SHARED_PREFS_KEY, addressBookUrl, this);
        String baseURL = getBaseURL(urlFromView);
        updatePreference(BASE_SYNC_URL_SHARED_PREFS_KEY, baseURL, this);
        updatePreference(CARD_DAV_SERVER_TYPE_SHARED_PREFS_KEY, carddavServerType.name, this);
        String syncToken = getStringFromPreferences(SYNC_TOKEN_SHARED_PREF_KEY, this);
        ContactsDataStore.requestPauseOnUpdates();
        try {
            if (TextUtils.isEmpty(syncToken) || hasServerChangedFromEarlier)
                fullSync(baseURL, username, password, addressBookUrl);
            else
                partialSync(baseURL, username, password, addressBookUrl, syncToken);
        } catch (Exception e) {
            showError(R.string.sync_failed);
            return;
        }
        invalidateGroups();
        ContactsDataStore.requestResumeUpdates();
        ContactsDataStore.refreshStoreAsync();
        toastFromNonUIThread(R.string.sync_complete, Toast.LENGTH_LONG, this);
        finish();
    }

    private void partialSync(String baseUrl, String username, String password, String addressBookUrl, String syncToken) throws Exception {
        Pair<List<String>, List<String>> pairOfListOfAddedUpdatedAndDeleted = getChangesSinceSyncToken(syncToken, baseUrl, addressBookUrl);
        deleteContactsLocallyAsTheyWereDeletedOnServer(pairOfListOfAddedUpdatedAndDeleted.second);
        syncChanges(baseUrl, username, password, addressBookUrl, CardDavUtils.getVcardsWithHrefs(pairOfListOfAddedUpdatedAndDeleted.first, baseUrl, addressBookUrl), true);
    }

    private void deleteContactsLocallyAsTheyWereDeletedOnServer(List<String> hrefsDeleted) {
        if (hrefsDeleted.isEmpty()) return;
        List<VCardData> allVCardDataList = VCardData.listAll(VCardData.class);
        Map<String, VCardData> allVCardsAsHREFMap = getAllVCardsAsHREFMap(allVCardDataList);

        U.forEach(hrefsDeleted, href -> {
            VCardData vCardData = allVCardsAsHREFMap.get(href);
            if (vCardData == null) return;
            ContactsDataStore.removeContact(vCardData.contact.getId());
            vCardData.delete();
        });
    }

    private void fullSync(String urlFromView, String username, String password, String addressBookUrl) throws Exception {
        List<Triplet<String, String, VCard>> hrefEtagAndVCardList = downloadAddressBook(urlFromView + addressBookUrl);
        syncChanges(urlFromView, username, password, addressBookUrl, hrefEtagAndVCardList, false);
    }

    private void syncChanges(String urlFromView, String username, String password, String addressBookUrl, List<Triplet<String, String, VCard>> hrefEtagAndVCardList, boolean syncFromLastCheckPoint) {
        List<VCardData> allVCardDataList = VCardData.listAll(VCardData.class);
        if (!hrefEtagAndVCardList.isEmpty()) updateLocal(hrefEtagAndVCardList, allVCardDataList);
        updateServer(allVCardDataList, syncFromLastCheckPoint, username, password, addressBookUrl);
        updatePreference(SYNC_TOKEN_SHARED_PREF_KEY, CardDavUtils.getSyncToken(urlFromView, addressBookUrl), this);
    }

    private void updateLocal(List<Triplet<String, String, VCard>> hrefEtagAndVCardList, List<VCardData> allVCardDataList) {
        Map<String, VCardData> allVCardsAsHREFMap = getAllVCardsAsHREFMap(allVCardDataList);
        U.forEach(hrefEtagAndVCardList, hrefEtagAndVCard -> {
            String href = hrefEtagAndVCard.x;
            if (allVCardsAsHREFMap.containsKey(href)) {
                VCardData vcardDataFromDB = allVCardsAsHREFMap.get(href);
                if (hrefEtagAndVCard.y.equals(vcardDataFromDB.etag)) return;
                processExistingVCard(hrefEtagAndVCard, vcardDataFromDB);
            } else createContact(hrefEtagAndVCard);
        });
    }

    private void createContact(Triplet<String, String, VCard> hrefEtagAndVCard) {
        Contact contact = ContactsDBHelper.addContact(hrefEtagAndVCard, this);
        VCardData vCardData = ContactsDBHelper.getVCard(contact.getId());
        vCardData.status = STATUS_NONE;
        vCardData.save();
    }

    private void updateServer(List<VCardData> allVCardDataList, boolean syncFromLastCheckPoint, String username, String password, String addressBookUrl) {
        String baseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        U.forEach(allVCardDataList, vcardData -> {
            switch (vcardData.status) {
                case STATUS_NONE:
                    if (syncFromLastCheckPoint) break;
                case STATUS_CREATED:
                    Pair<String, String> hrefAndEtag = CardDavUtils.createContactOnServer(vcardData, addressBookUrl, baseUrl);
                    if (hrefAndEtag == null) break;
                    vcardData.href = hrefAndEtag.first;
                    vcardData.etag = hrefAndEtag.second;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_UPDATED:
                    String etag = CardDavUtils.updateContactOnServer(vcardData, baseUrl);
                    if (etag == null) break;
                    vcardData.etag = etag;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_DELETED:
                    boolean deletionOnServerSuccessful = CardDavUtils.deleteVCardOnServer(vcardData, baseUrl);
                    if (deletionOnServerSuccessful) vcardData.delete();
                    break;
            }
        });
    }

    private void processExistingVCard(Triplet<String, String, VCard> hrefEtagAndVCard, VCardData vcardDataFromDB) {
        if (vcardDataFromDB.status == STATUS_DELETED) {
            vcardDataFromDB.href = hrefEtagAndVCard.x;
            return;
        }
        if (vcardDataFromDB.status == STATUS_NONE) {
            replaceContactWithServers(hrefEtagAndVCard, vcardDataFromDB, this);
            return;
        }
        merge(hrefEtagAndVCard, vcardDataFromDB, this);
    }

    private void showError(int messageRes) {
        runOnUiThread(() -> ((AppCompatTextView) findViewById(R.id.error)).setText(messageRes));
    }


    private static Map<String, VCardData> getAllVCardsAsUIDMap(List<VCardData> allVCardDataList) {
        if (allVCardDataList.isEmpty())
            return new HashMap<>(0);

        List<Tuple<String, VCardData>> listOfTuples = U.map(allVCardDataList, vCardData -> new Tuple<>(vCardData.uid, vCardData));
        return U.toMap(listOfTuples);
    }

    private static Map<String, VCardData> getAllVCardsAsHREFMap(List<VCardData> allVCardDataList) {
        if (allVCardDataList.isEmpty())
            return new HashMap<>(0);

        return U.reduce(allVCardDataList, (hashMap, VCardData) -> {
            if (VCardData.href == null)
                return hashMap;
            hashMap.put(VCardData.href, VCardData);
            return hashMap;
        }, new HashMap<>());
    }
}
