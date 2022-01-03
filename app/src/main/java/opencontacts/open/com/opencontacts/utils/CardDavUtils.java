package opencontacts.open.com.opencontacts.utils;

import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.HTTP_HEADER_DEPTH;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.HTTP_METHOD_PROPFIND;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.HTTP_METHOD_REPORT;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.HTTP_STATUS_NOT_FOUND;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_NAMESPACE_CARDDAV;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_NAMESPACE_DAV;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_ADDRESSBOOK;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_ADDRESS_DATA;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_GETETAG;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_HREF;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_RESPONSE;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_STATUS;
import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.XML_TAG_SYNC_TOKEN;
import static opencontacts.open.com.opencontacts.utils.Common.map;
import static opencontacts.open.com.opencontacts.utils.NetworkUtils.getHttpClientWithBasicAuth;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.createXMLDocument;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.getText;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.github.underscore.lodash.U;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ezvcard.VCard;
import ezvcard.io.text.VCardReader;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import opencontacts.open.com.opencontacts.orm.VCardData;

public class CardDavUtils {

    public static final String HTTP_HEADER_E_TAG = "ETag";

    public static String figureOutAddressBookUrl(String baseUrl, String username, CheekyCarddavServerStuff carddavServerType, Context context) {
        OkHttpClient okHttpClient = getHttpClientWithBasicAuth();
        Request request = new Request.Builder()
            .method(HTTP_METHOD_PROPFIND, null)
            .addHeader(HTTP_HEADER_DEPTH, String.valueOf(1))
            .url(carddavServerType.getAddressBookUrl(baseUrl, username))
            .build();


        try {
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
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
        if (xmlDocument == null)
            return null;

        NodeList responseElements = xmlDocument.getDocumentElement().getElementsByTagNameNS(XML_NAMESPACE_DAV, XML_TAG_RESPONSE);
        List<Node> listOfResourceNodesWithAddressBooks = new U<>(new NodeListIterable(responseElements))
            .filter(node -> ((Element) node).getElementsByTagNameNS(XML_NAMESPACE_CARDDAV, XML_TAG_ADDRESSBOOK).getLength() > 0);


        Node responseNodeOfAddressbookType = U.firstOrNull(listOfResourceNodesWithAddressBooks);
        if (responseNodeOfAddressbookType == null)
            return null;
        return getText(XML_TAG_HREF, XML_NAMESPACE_DAV, responseNodeOfAddressbookType);
    }

    public static List<Triplet<String, String, VCard>> downloadAddressBook(String addressBookUrl) throws Exception {
        String addressBookQueryAskingVCardData = "<card:addressbook-query xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">\n" +
            "    <d:prop>\n" +
            "        <d:getetag />\n" +
            "        <card:address-data />\n" +
            "    </d:prop>\n" +
            "    <card:filter />\n" +
            "</card:addressbook-query>";
        Request addressBookDownloadRequest = new Request.Builder()
            .method(HTTP_METHOD_REPORT, RequestBody.create(MediaType.get("text/xml"), addressBookQueryAskingVCardData))
            .header("depth", "1") //failing on nextcloud sabre dav server if depth is 0
            .url(addressBookUrl)
            .build();
        Response addressBookResponse = getHttpClientWithBasicAuth()
            .newCall(addressBookDownloadRequest)
            .execute();
        if (addressBookResponse.isSuccessful())
            return getVCardsOutOfAddressBookResponse(addressBookResponse.body().string());
        else throw new Exception(addressBookResponse.body().string());
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
        String newContactUrl = addressBookUrl + vcardData.uid + ".vcf";
        Request createContactRequest = new Request.Builder()
            .url(baseUrl + newContactUrl)
            .put(RequestBody.create(null, vcardData.vcardDataAsString))
            .build();
        try {
            Response response = httpClientWithBasicAuth.newCall(createContactRequest).execute();
            if (response.isSuccessful()) {
                String etag = getVCardEtag(baseUrl, newContactUrl);
                if (etag != null) return new Pair<>(newContactUrl, etag);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String getVCardEtag(String baseUrl, String newContactUrl) throws IOException {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        Response getVCardResponse = httpClientWithBasicAuth.newCall(new Request.Builder()
            .url(baseUrl + newContactUrl)
            .get()
            .build()).execute();
        if (getVCardResponse.isSuccessful()) {
            return getVCardResponse.header(HTTP_HEADER_E_TAG);
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
            if (response.isSuccessful()) {
                return getVCardEtag(baseUrl, vcardData.href);
            }
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

    public static boolean areNotValidDetails(String url, String username, String password, boolean shouldIgnoreSSL, CheekyCarddavServerStuff carddavServerType, String addressBookUrl) {
        String addressBookBasedUrl = addressBookUrl == null ? "" : url + addressBookUrl;
        String urlToTest = getBaseURL(addressBookBasedUrl).equals(url) ? addressBookBasedUrl : carddavServerType.getValidateServerUrl(url, username);
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth(username, password, shouldIgnoreSSL);
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                .method(HTTP_METHOD_PROPFIND, null)
                .addHeader(HTTP_HEADER_DEPTH, "0")
                .url(urlToTest)
                .build())
                .execute();
            return !response.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static String getBaseURL(String url) {
        String path = null;
        try {
            path = Uri.parse(url).getPath();
        } catch (Exception e) {
        }
        if (path == null) return url;
        return url.replace(path, "");
    }

    public static String getSyncToken(String baseUrl, String addressBookUrl) {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                .method(HTTP_METHOD_PROPFIND, RequestBody.create(null, "<d:propfind xmlns:d=\"DAV:\" xmlns:cs=\"http://calendarserver.org/ns/\">\n" +
                    "  <d:prop>\n" +
                    "     <d:displayname />\n" +
                    "     <cs:getctag />\n" +
                    "     <d:sync-token />\n" +
                    "  </d:prop>\n" +
                    "</d:propfind>"))
                .url(baseUrl + addressBookUrl)
                .addHeader(HTTP_HEADER_DEPTH, "0")
                .build())
                .execute();
            if (response.isSuccessful()) {
                Document xmlDocument = createXMLDocument(response.body().string());
                NodeList syncTokenNodeList = xmlDocument.getElementsByTagNameNS(XML_NAMESPACE_DAV, XML_TAG_SYNC_TOKEN);
                if (syncTokenNodeList.getLength() == 0)
                    return null;
                return syncTokenNodeList.item(0).getTextContent();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Pair<List<String>, List<String>> getChangesSinceSyncToken(String synctoken, String baseUrl, String addressBookUrl) throws Exception {
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        List<String> updatedOrAdded = new ArrayList<>(0);
        List<String> deleted = new ArrayList<>(0);
        Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
            .method(HTTP_METHOD_REPORT, RequestBody.create(MediaType.get("application/xml"), "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                "<d:sync-collection xmlns:d=\"DAV:\">\n" +
                "  <d:sync-token>" + synctoken + "</d:sync-token>\n" +
                "  <d:sync-level>1</d:sync-level>\n" +
                "  <d:prop>\n" +
                "    <d:getetag/>\n" +
                "  </d:prop>\n" +
                "</d:sync-collection>"))
            .url(baseUrl + addressBookUrl)
            .build())
            .execute();
        if (response.isSuccessful()) {
            Document xmlDocument = createXMLDocument(response.body().string());
            NodeList responseNodes = xmlDocument.getElementsByTagNameNS(XML_NAMESPACE_DAV, XML_TAG_RESPONSE);
            U.forEach(new NodeListIterable(responseNodes), node -> {
                String href = getText(XML_TAG_HREF, XML_NAMESPACE_DAV, node);
                String status = getText(XML_TAG_STATUS, XML_NAMESPACE_DAV, node);
                if (status.contains(HTTP_STATUS_NOT_FOUND)) deleted.add(href);
                else updatedOrAdded.add(href);

            });
        } else throw new Exception("failed fetching changes");
        return new Pair<>(updatedOrAdded, deleted);
    }

    public static List<Triplet<String, String, VCard>> getVcardsWithHrefs(List<String> hrefs, String baseUrl, String addressBookUrl) {
        if (hrefs.isEmpty()) return new ArrayList<>(0);
        OkHttpClient httpClientWithBasicAuth = getHttpClientWithBasicAuth();
        try {
            Response response = httpClientWithBasicAuth.newCall(new Request.Builder()
                .method(HTTP_METHOD_REPORT, RequestBody.create(null, getRequestBodyToFetchVCardsWithHrefs(hrefs)))
                .url(baseUrl + addressBookUrl)
                .build())
                .execute();
            if (response.isSuccessful()) {
                List<Triplet<String, String, VCard>> tripletOfHrefEtagAndVCard = getVCardsOutOfAddressBookResponse(response.body().string());
                return tripletOfHrefEtagAndVCard;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(0);
    }

    private static String getRequestBodyToFetchVCardsWithHrefs(List<String> hrefs) {
        String prefix = "<card:addressbook-multiget xmlns:d=\"DAV:\" xmlns:card=\"urn:ietf:params:xml:ns:carddav\">\n" +
            "    <d:prop>\n" +
            "        <d:getetag />\n" +
            "        <card:address-data />\n" +
            "    </d:prop>";

        String suffix = "</card:addressbook-multiget>";
        final StringBuilder hrefsInRequest = new StringBuilder();
        String hrefTagOpen = "<d:href>";
        String hrefTagClose = "</d:href>";
        U.forEach(hrefs, href -> hrefsInRequest.append(hrefTagOpen + href + hrefTagClose));
        return prefix + hrefsInRequest.toString() + suffix;
    }

}

class NodeListIterable implements Iterable<Node> {

    private final NodeListIterator nodeListIterator;

    NodeListIterable(NodeList nodeList) {
        this.nodeListIterator = new NodeListIterator(nodeList);
    }

    @NonNull
    @Override
    public NodeListIterator iterator() {
        return nodeListIterator;
    }
}

class NodeListIterator implements Iterator<Node> {

    private NodeList nodeList;
    private int currentIndex = -1;

    NodeListIterator(NodeList nodeList) {
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
