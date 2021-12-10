package opencontacts.open.com.opencontacts.utils;

public class CheekyCarddavServerStuff {
    public String name;
    public String defaultUrl;
    String addressBookUrlSuffix;
    String validateServerUrlSuffix;

    public CheekyCarddavServerStuff(String name) {
        this(name, "", "", "");
    }

    public CheekyCarddavServerStuff(String name, String defaultUrl, String addressBookUrlSuffix, String validateServerUrlSuffix) {
        this.name = name;
        this.defaultUrl = defaultUrl;
        this.addressBookUrlSuffix = addressBookUrlSuffix;
        this.validateServerUrlSuffix = validateServerUrlSuffix;
    }

    public String getAddressBookUrl(String baseUrl, String username) {
        return baseUrl + addressBookUrlSuffix;
    }

    public String getValidateServerUrl(String baseUrl, String username) {
        return baseUrl + validateServerUrlSuffix;
    }
}
