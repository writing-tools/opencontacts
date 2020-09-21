package opencontacts.open.com.opencontacts.utils.domain;

import com.github.underscore.U;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import opencontacts.open.com.opencontacts.orm.Contact;


public class ContactGroupsUtil {

    public static final String GROUPS_SEPERATOR_CHAR = ",";

    public static List<String> getGroups(Contact contact) {
        if(contact.groups == null) return Collections.emptyList();
        return Arrays.asList(contact.groups.split(GROUPS_SEPERATOR_CHAR));
    }

    public static String getGroupsAsString(List<String> groups) {
        return U.join(groups, GROUPS_SEPERATOR_CHAR);
    }
}
