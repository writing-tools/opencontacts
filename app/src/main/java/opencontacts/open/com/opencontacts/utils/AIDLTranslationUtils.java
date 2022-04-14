package opencontacts.open.com.opencontacts.utils;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.domain.Contact;

public class AIDLTranslationUtils {

    public static String[] tempStringArray = new String[]{};
    public static String[] contactToCSV(Contact contact) {
        List<String> csvRow = new ArrayList<>();
        csvRow.add(contact.name);
        csvRow.addAll(U.map(contact.phoneNumbers, ph -> ph.phoneNumber));
        return csvRow.toArray(tempStringArray);
    }
}
