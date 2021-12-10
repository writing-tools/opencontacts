package opencontacts.open.com.opencontacts.utils;

import com.github.underscore.Tuple;
import com.github.underscore.U;

import java.util.Arrays;
import java.util.Map;

public interface CARDDAVConstants {
    String HTTP_METHOD_PROPFIND = "PROPFIND";
    String HTTP_STATUS_NOT_FOUND = "404";
    String HTTP_HEADER_DEPTH = "depth";
    String XML_TAG_RESPONSE = "response";
    String XML_TAG_SYNC_TOKEN = "sync-token";
    String XML_TAG_HREF = "href";
    String XML_TAG_ADDRESSBOOK = "addressbook";
    String HTTP_METHOD_REPORT = "REPORT";
    String XML_TAG_GETETAG = "getetag";
    String XML_TAG_STATUS = "status";
    String XML_TAG_ADDRESS_DATA = "address-data";

    String XML_NAMESPACE_DAV = "DAV:";
    String XML_NAMESPACE_CARDDAV = "urn:ietf:params:xml:ns:carddav";

    CheekyCarddavServerStuff mailboxServerConstants = new CheekyCarddavServerStuff(
        "Mailbox",
        "https://dav.mailbox.org",
        "/carddav/32",
        "/carddav/32");

    CheekyCarddavServerStuff radicaleServerConstants = new CheekyCarddavServerStuff(
        "Radicale",
        "",
        "/",
        "/") {
        @Override
        public String getAddressBookUrl(String baseUrl, String username) {
            return baseUrl + addressBookUrlSuffix + username;
        }

        @Override
        public String getValidateServerUrl(String baseUrl, String username) {
            return baseUrl;
        }
    };

    CheekyCarddavServerStuff nextCloudConstants = new CheekyCarddavServerStuff(
        "Nextcloud",
        "",
        "/remote.php/dav/addressbooks/users/",
        "") {
        @Override
        public String getAddressBookUrl(String baseUrl, String username) {
            return baseUrl + addressBookUrlSuffix + username;
        }

        @Override
        public String getValidateServerUrl(String baseUrl, String username) {
            return getAddressBookUrl(baseUrl, username);
        }
    };

    CheekyCarddavServerStuff sogoConstants = new CheekyCarddavServerStuff(
        "Sogo",
        "",
        "/SOGo/dav/",
        "/SOGo/dav/") {
        @Override
        public String getAddressBookUrl(String baseUrl, String username) {
            return baseUrl + addressBookUrlSuffix + username + "/Contacts/personal/";
        }

        @Override
        public String getValidateServerUrl(String baseUrl, String username) {
            return baseUrl + validateServerUrlSuffix;
        }
    };
    CheekyCarddavServerStuff otherServerConstants = new CheekyCarddavServerStuff("Other");

    Map<String, CheekyCarddavServerStuff> carddavServersCheekyStuffMap = U.toMap(
        Arrays.asList(
            new Tuple<>(mailboxServerConstants.name, mailboxServerConstants),
            new Tuple<>(radicaleServerConstants.name, radicaleServerConstants),
            new Tuple<>(nextCloudConstants.name, nextCloudConstants),
            new Tuple<>(sogoConstants.name, sogoConstants),
            new Tuple<>(otherServerConstants.name, otherServerConstants)
        )
    );
}

