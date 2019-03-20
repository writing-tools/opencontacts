package opencontacts.open.com.opencontacts.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.github.underscore.lodash.U;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import opencontacts.open.com.opencontacts.orm.VCardData;

import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.*;
import static opencontacts.open.com.opencontacts.utils.Common.map;
import static opencontacts.open.com.opencontacts.utils.NetworkUtils.getHttpClientWithBasicAuth;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.createXMLDocument;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.getText;

public class CardDavUtils {

    public static final String HTTP_HEADER_E_TAG = "ETag";

    public static String figureOutAddressBookUrl(String url, String username){
        OkHttpClient okHttpClient = getHttpClientWithBasicAuth();

        Request request = new Request.Builder()
                .method(HTTP_METHOD_PROPFIND, null)
                .addHeader(HTTP_HEADER_DEPTH, String.valueOf(1))
                .url(url + "/" + username)
                .build();


        try {
            Response response = okHttpClient.newCall(request).execute();
            if(response.isSuccessful()){
                try {
                    return getAddressBookUrlOutOfXML(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String getAddressBookUrlOutOfXML(String xml) {
        Document xmlDocument = createXMLDocument(xml);
        if(xmlDocument == null)
            return null;

        NodeList responseElements = xmlDocument.getDocumentElement().getElementsByTagNameNS(XML_NAMESPACE_DAV, XML_TAG_RESPONSE);
        List<Node> listOfResourceNodesWithAddressBooks = new U<>(new NodeListIterable(responseElements))
                .filter(node -> ((Element) node).getElementsByTagNameNS(XML_NAMESPACE_CARDDAV, XML_TAG_ADDRESSBOOK).getLength() > 0);


        Node responseNodeOfAddressbookType = U.firstOrNull(listOfResourceNodesWithAddressBooks);
        if(responseNodeOfAddressbookType == null)
            return null;
        return getText(XML_TAG_HREF, XML_NAMESPACE_DAV, responseNodeOfAddressbookType);
    }

    public static List<Triplet<String ,String, VCard>> downloadAddressBook(String addressBookUrl) {
        String addressBookQueryAskingVCardData = "<card:addressbook-query xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">\n" +
                "    <d:prop>\n" +
                "        <d:getetag />\n" +
                "        <card:address-data />\n" +
                "    </d:prop>\n" +
                "</card:addressbook-query>";
        Request addressBookDownloadRequest = new Request.Builder()
                .method(HTTP_METHOD_REPORT, RequestBody.create(null, addressBookQueryAskingVCardData))
                .url(addressBookUrl)
                .build();
        try {
            Response addressBookResponse = getHttpClientWithBasicAuth()
                    .newCall(addressBookDownloadRequest)
                    .execute();
            if(addressBookResponse.isSuccessful()) return getVCardsOutOfAddressBookResponse(addressBookResponse.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(0);
    }

    private static List<Triplet<String, String, VCard>> getVCardsOutOfAddressBookResponse(String xml) {
        Document xmlDocument = createXMLDocument(xml);
        NodeList responseNodes = xmlDocument.getElementsByTagNameNS(XML_NAMESPACE_DAV, XML_TAG_RESPONSE);
        return map(new NodeListIterable(responseNodes), node -> {
            try {
                return new Triplet<>(
                        getText(XML_TAG_HREF, XML_NAMESPACE_DAV, node),
                        getText(XML_TAG_GETETAG, XML_NAMESPACE_DAV, node),
                        new VCardReader(getText(XML_TAG_ADDRESS_DATA, XML_NAMESPACE_CARDDAV, node)).readNext()
                );
            } catch (IOException e) {
                e.printStackTrace();
                return new Triplet<>(null, null, null);
            }
        });
    }

    public static Pair<String, String> createContactOnServer(VCardData vcardData, String addressBookUrl, String baseUrl) {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        String newContactUrl = baseUrl + addressBookUrl + vcardData.uid;
        Request createContactRequest = new Request.Builder()
                .url(newContactUrl)
                .put(RequestBody.create(null, vcardData.vcardDataAsString))
                .build();
        try {
            Response response = httpClientWithBasicAuth.newCall(createContactRequest).execute();
            if(response.isSuccessful()){
                Response getVCardResponse = httpClientWithBasicAuth.newCall(new Request.Builder()
                        .url(newContactUrl)
                        .get()
                        .build()).execute();
                if(getVCardResponse.isSuccessful()){
                    return new Pair<>(addressBookUrl + vcardData.uid, response.header(HTTP_HEADER_E_TAG));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String updateContactOnServer(VCardData vcardData, String baseUrl) {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                    .put(RequestBody.create(null, vcardData.vcardDataAsString))
                    .url(baseUrl + vcardData.href)
                    .build())
                    .execute();
            return response.header(HTTP_HEADER_E_TAG);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean deleteVCardOnServer(VCardData vcardData, String baseUrl) {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                    .delete()
                    .url(baseUrl + vcardData.href)
                    .build())
                    .execute();
            return response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean areNotValidDetails(String baseUrl, String username, String password){
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth(username, password);
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                    .method(HTTP_METHOD_PROPFIND, null)
                    .url(baseUrl + "/" + username)
                    .build())
                    .execute();
            return !response.isSuccessful();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}

class NodeListIterable implements Iterable<Node>{

    private final NodeListIterator nodeListIterator;

    NodeListIterable(NodeList nodeList){
        this.nodeListIterator = new NodeListIterator(nodeList);
    }
    @NonNull
    @Override
    public NodeListIterator iterator() {
        return nodeListIterator;
    }
}

class NodeListIterator implements Iterator<Node>{

    private NodeList nodeList;
    private int currentIndex = -1;

    NodeListIterator(NodeList nodeList){
        this.nodeList = nodeList;
    }
    @Override
    public boolean hasNext() {
        return currentIndex < nodeList.getLength() - 1;
    }

    @Override
    public Node next() {
        return nodeList.item(++currentIndex);
    }
}