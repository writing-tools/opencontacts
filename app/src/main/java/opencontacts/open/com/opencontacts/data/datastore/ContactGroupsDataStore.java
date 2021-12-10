package opencontacts.open.com.opencontacts.data.datastore;


import static opencontacts.open.com.opencontacts.utils.AndroidUtils.processAsync;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.toastFromNonUIThread;
import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;
import static opencontacts.open.com.opencontacts.utils.VCardUtils.writeVCardToString;

import android.content.Context;
import android.widget.Toast;

import com.github.underscore.lodash.U;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ezvcard.VCard;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.domain.ContactGroup;
import opencontacts.open.com.opencontacts.orm.VCardData;
import opencontacts.open.com.opencontacts.utils.VCardUtils;

public class ContactGroupsDataStore {

    private static Map<String, ContactGroup> groupsMap = new HashMap<>();
    private static boolean init = false;

    public static void COMPUTE_INTENSIVE_computeGroups() {
        List<Contact> allContacts = ContactsDataStore.getAllContacts();
        groupsMap = new HashMap<>();
        for (Contact contact : allContacts) {
            U.chain(contact.getGroupNames())
                .map(groupName -> getOrDefault(groupsMap, groupName, new ContactGroup(groupName)))
                .map(group -> group.addContact(contact))
                .forEach(group -> groupsMap.put(group.getName(), group));
        }
    }

    public static List<ContactGroup> getGroupsOf(Contact contact) {
        return U.chain(contact.getGroupNames())
            .map(groupsMap::get)
            .reject(U::isNull)
            .value();
    }

    public static void initInCaseHasNot() {
        if (init) return;
        computeGroupsAsync();
        init = true;
    }

    public static void invalidateGroups() {
        init = false;
        groupsMap = new HashMap<>(0);
    }

    public static void computeGroupsAsync() {
        processAsync(ContactGroupsDataStore::COMPUTE_INTENSIVE_computeGroups);
    }

    public static List<ContactGroup> getAllGroups() {
        return new ArrayList<>(groupsMap.values());
    }


    public static void createNewGroup(List<Contact> contacts, String groupName) {
        ContactGroup newContactGroup = new ContactGroup(groupName);
        groupsMap.put(groupName, newContactGroup);
        U.forEach(contacts, contact -> addContactToGroup(newContactGroup, contact));
    }

    public static void updateGroup(List<Contact> newContacts, String newGroupName, ContactGroup group) {
        if (!newGroupName.equals(group.getName())) {
            destroyGroup(group);
            createNewGroup(newContacts, newGroupName);
            return;
        }
        Collection<Contact> removedContacts = U.reject(group.contacts, newContacts::contains);
        U.forEach(removedContacts, removedContact -> removeContactFromGroup(group, removedContact));

        //removing based on group name hence it should happen first
        group.updateName(newGroupName);

        Collection<Contact> onlyNewContacts = U.reject(newContacts, group.contacts::contains);
        U.forEach(onlyNewContacts, newContact -> addContactToGroup(group, newContact));
    }

    private static void destroyGroup(ContactGroup group) {
        //new array list coz of concurrent modification of same array group.contacts
        U.chain(group.contacts)
            .forEach(contact -> removeContactFromGroup(group, contact));
        groupsMap.remove(group.getName());
    }

    private static void addContactToGroup(ContactGroup group, Contact contact) {
        group.addContact(contact);
        List<String> allGroupsOfContact = contact.addGroup(group.getName());
        updateContactTable(contact);
        updateVCardTable(contact, allGroupsOfContact);
    }

    private static void removeContactFromGroup(ContactGroup group, Contact contact) {
        group.removeContact(contact);
        List<String> allGroupsOfContact = contact.removeGroup(group.getName());
        updateContactTable(contact);
        updateVCardTable(contact, allGroupsOfContact);
    }

    private static void updateVCardTable(Contact contact, List<String> allGroupsOfContact) {
        try {
            VCardData vCardData = ContactsDBHelper.getVCard(contact.id);
            VCard vcard = VCardUtils.getVCardFromString(vCardData.vcardDataAsString);
            vcard.setCategories(allGroupsOfContact.toArray(new String[]{}));
            vCardData.vcardDataAsString = writeVCardToString(vcard);
            vCardData.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void updateContactTable(Contact contact) {
        opencontacts.open.com.opencontacts.orm.Contact dbContact = ContactsDBHelper.getDBContactWithId(contact.id);
        dbContact.groups = contact.groups;
        dbContact.save();
    }

    @Nullable
    public static ContactGroup getGroup(String name) {
        return groupsMap.get(name);
    }

    public static void handleContactDeletion(Contact contact) {
        U.chain(groupsMap.values())
            .map(group -> group.contacts)
            .forEach(contactsList -> contactsList.remove(contact));
    }

    public static void handleContactUpdate(Contact contact) {
        List<String> newGroupAssociations = contact.getGroupNames();
        U.forEach(groupsMap.values(), group -> {
            if (newGroupAssociations.contains(group.getName())) group.addContact(contact);
            else group.removeContact(contact);
        });
    }

    public static void handleNewContactAddition(Contact contact) {
        List<String> groupAssociations = contact.getGroupNames();
        if (groupAssociations.isEmpty()) return;
        U.chain(groupsMap.values())
            .filter(group -> groupAssociations.contains(group.getName()))
            .forEach(group -> group.addContact(contact));
    }

    public static void PROCESS_INTENSIVE_delete(ContactGroup selectedGroup, Context context) {
        U.forEach(new ArrayList<>(selectedGroup.contacts), contact -> removeContactFromGroup(selectedGroup, contact));
        groupsMap.remove(selectedGroup.getName());
        toastFromNonUIThread(R.string.group_deleted, Toast.LENGTH_SHORT, context);
    }
}

