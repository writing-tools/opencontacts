package opencontacts.open.com.opencontacts.data.datastore;


import com.github.underscore.lodash.U;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.ContactGroup;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;

public class ContactGroupsStore {

    private static Map<String, ContactGroup> groupsMap = new HashMap<>();

    public static void COMPUTE_INTENSIVE_computeGroups() {
        List<Contact> allContacts = ContactsDataStore.getAllContacts();
        groupsMap = new HashMap<>();
        for(Contact contact : allContacts) {
            U.chain(contact.getGroupNames())
                    .map(groupName -> getOrDefault(groupsMap, groupName, new ContactGroup(groupName)))
                    .map(group -> group.addContact(contact))
                    .forEach(group -> groupsMap.put(group.name, group));
        }
    }

    public static List<ContactGroup> getGroupsOf(Contact contact) {
        return U.chain(contact.getGroupNames())
                .map(groupName -> groupsMap.get(groupName))
                .reject(U::isNull)
                .value();
    }

    public static void computeGroupsAsync(){
        processAsync(ContactGroupsStore::COMPUTE_INTENSIVE_computeGroups);
    }

    public static List<ContactGroup> getAllGroups() {
        return new ArrayList<>(groupsMap.values());
    }
}
