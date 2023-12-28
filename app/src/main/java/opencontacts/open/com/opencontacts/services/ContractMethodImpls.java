package opencontacts.open.com.opencontacts.services;

import static open.com.opencontactsdatasourcecontract.Contract.formErrorResponseV1;
import static open.com.opencontactsdatasourcecontract.ContractMethod.methodKey;
import static open.com.opencontactsdatasourcecontract.ContractMethods.fetchAllNamesAndPhoneNumbersV1;
import static open.com.opencontactsdatasourcecontract.ContractMethods.fetchNamesForPhoneNumbersV1;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContact;
import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.getContactWithId;
import static opencontacts.open.com.opencontacts.utils.AIDLTranslationUtils.csvString;

import android.content.Context;
import android.text.TextUtils;

import com.github.underscore.Function;
import com.github.underscore.U;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import open.com.opencontactsdatasourcecontract.ContractMethod;
import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.AIDLTranslationUtils;
import opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils;

public class ContractMethodImpls {
    private static final HashMap<String, ContractMethodImpl> contractMethodImpls = new HashMap<>();

    public static String validateAndCall(String packageName, String[] argsWithMethodAndVersion, Context context) {
        Set<String> permissionOfPackage = SharedPreferencesUtils.permissions(context, packageName);
        String methodName = argsWithMethodAndVersion[0];
        String methodVersion = argsWithMethodAndVersion[1];
        List<String> args = U.drop(Arrays.asList(argsWithMethodAndVersion), 2);
        ContractMethodImpl contractMethodImpl = contractMethodImpls.get(methodKey(methodName, methodVersion));
        if(contractMethodImpl == null) {
            System.out.println("No such method found yolo");
            return formErrorResponseV1("no such method found");
        }
        if(!permissionOfPackage.containsAll(contractMethodImpl.contractMethod.permissionsRequired)) {
            System.out.println("Not enough permissions yolo");
            return formErrorResponseV1("Not enough permissions");
        }
        return contractMethodImpl.call(args, context);
    }


    public static void addContractMethodImpl(ContractMethod contractMethod, Function<List<String>, Function<Context, String>> impl) {
        contractMethodImpls.put(methodKey(contractMethod), new ContractMethodImpl(contractMethod, impl));
    }

    static {
       addContractMethodImpl(fetchAllNamesAndPhoneNumbersV1, argsAsList -> context -> {
           List<Contact> contacts = ContactsDataStore.getAllContactsSync();
           List<String[]> eachContactAsCSV = U.map(contacts, AIDLTranslationUtils::nameAndPhoneNumbersToCSV);
           return csvString(eachContactAsCSV);
       });

       addContractMethodImpl(fetchNamesForPhoneNumbersV1, phoneNumbers -> context -> {
           Function<String, String[]> pairNameAndPhoneNumber = phoneNumber -> {
               opencontacts.open.com.opencontacts.orm.Contact dbContact = getContact(phoneNumber);
               if(dbContact == null) return new String[]{"", phoneNumber};
               Contact contact = getContactWithId(dbContact.getId());
               String name = TextUtils.isEmpty(contact.pinyinName) ? contact.name : contact.pinyinName;
               return new String[]{name, phoneNumber};
           };

           List<String[]> nameAndPhoneNumbers = U.map(phoneNumbers, pairNameAndPhoneNumber);
           return csvString(nameAndPhoneNumbers);
       });
    }
}
