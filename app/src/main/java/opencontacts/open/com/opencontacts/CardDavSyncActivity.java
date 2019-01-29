package opencontacts.open.com.opencontacts;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
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
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.CardDavUtils;
import opencontacts.open.com.opencontacts.utils.Triplet;

import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_CREATED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_DELETED;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_NONE;
import static opencontacts.open.com.opencontacts.orm.VCardData.STATUS_UPDATED;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.ADDRESSBOOK_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.BASE_SYNC_URL_SHARED_PREFS_KEY;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.downloadAddressBook;
import static opencontacts.open.com.opencontacts.utils.CardDavUtils.figureOutAddressBookUrl;

public class CardDavSyncActivity extends AppCompatActivity {

    private String savedBaseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_dav_sync);
        savedBaseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        ((TextInputEditText) findViewById(R.id.url)).setText(savedBaseUrl);
    }

    public void sync(View view) {
        String url = ((TextInputEditText) findViewById(R.id.url)).getText().toString();
        String username = ((TextInputEditText) findViewById(R.id.username)).getText().toString();
        String password = ((TextInputEditText) findViewById(R.id.password)).getText().toString();
        processAsync(() -> sync(url, username, password));
    }

    private void sync(String urlFromView, String username, String password) {
        String addressBookUrl = getStringFromPreferences(ADDRESSBOOK_URL_SHARED_PREFS_KEY, this);
        if(addressBookUrl == null || !urlFromView.equals(savedBaseUrl))
            addressBookUrl = figureOutAddressBookUrl(urlFromView, username, password);
        if (addressBookUrl == null) {
            showError(R.string.no_addressbook_found);
            return;
        }
        updatePreference(ADDRESSBOOK_URL_SHARED_PREFS_KEY, addressBookUrl, this);
        updatePreference(BASE_SYNC_URL_SHARED_PREFS_KEY, urlFromView, this);
        List<Triplet<String, String, VCard>> hrefEtagAndVCardList = downloadAddressBook(urlFromView + addressBookUrl, username, password);
        List<VCardData> allVCardDataList = VCardData.listAll(VCardData.class);
        Map<String, VCardData> allVCardsFromDBAsMap = getAllVCardsAsUIDMap(allVCardDataList);
        U.forEach(hrefEtagAndVCardList, hrefEtagAndVCard -> {
            String uid = hrefEtagAndVCard.z.getUid().getValue();
            if(allVCardsFromDBAsMap.containsKey(uid)) processExistingVCard(hrefEtagAndVCard, allVCardsFromDBAsMap.get(uid));
            else ContactsDBHelper.addContact(hrefEtagAndVCard, this);
        });
        updateServer(allVCardDataList, true, username, password, addressBookUrl);
        ContactsDBHelper.deleteVCardsWithStatusDeleted();
        ContactsDataStore.refreshStoreAsync();
        toastFromNonUIThread(R.string.sync_complete, Toast.LENGTH_LONG, this);
    }

    private void updateServer(List<VCardData> allVCardDataList, boolean oldServer, String username, String password, String addressBookUrl) {
        String baseUrl = getStringFromPreferences(BASE_SYNC_URL_SHARED_PREFS_KEY, this);
        U.forEach(allVCardDataList, vcardData -> {
            switch (vcardData.status){
                case STATUS_NONE:
                    if(oldServer) break;
                case STATUS_CREATED:
                    Pair<String, String> hrefAndEtag = CardDavUtils.createContactOnServer(vcardData, username, password, addressBookUrl, baseUrl);
                    if(hrefAndEtag == null) break;
                    vcardData.href = hrefAndEtag.first;
                    vcardData.etag = hrefAndEtag.second;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_UPDATED:
                    String etag= CardDavUtils.updateContactOnServer(vcardData);
                    if(etag == null) break;
                    vcardData.etag = etag;
                    vcardData.status = STATUS_NONE;
                    vcardData.save();
                    break;
                case STATUS_DELETED:
                    CardDavUtils.deleteVCardOnServer(vcardData, username, password, addressBookUrl, baseUrl);
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
