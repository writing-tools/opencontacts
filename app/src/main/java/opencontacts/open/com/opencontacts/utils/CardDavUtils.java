package opencontacts.open.com.opencontacts.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

import static opencontacts.open.com.opencontacts.utils.CARDDAVConstants.*;
import static opencontacts.open.com.opencontacts.utils.Common.map;
import static opencontacts.open.com.opencontacts.utils.NetworkUtils.getHttpClientWithBasicAuth;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.createXMLDocument;
import static opencontacts.open.com.opencontacts.utils.XMLParsingUtils.getText;

public class CardDavUtils {
    public static String figureOutAddressBookUrl(String url, String username, String password){
        OkHttpClient okHttpClient = getHttpClientWithBasicAuth(username, password);

        Request request = new Request.Builder()
                .method(HTTP_METHOD_PROPFIND, null)
                .addHeader(HTTP_HEADER_DEPTH, String.valueOf(1))
//                .url(url + "/carddav")
                .url(url + "/admin")
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
            else {
                System.out.println(response.code() + " yolo failed");
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

    public static List<Triplet<String ,String, VCard>> downloadAddressBook(String addressBookUrl, String username, String password) {
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
            Response addressBookResponse = getHttpClientWithBasicAuth(username, password)
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