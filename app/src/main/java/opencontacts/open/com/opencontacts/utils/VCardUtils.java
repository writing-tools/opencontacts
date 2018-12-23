package opencontacts.open.com.opencontacts.utils;


import com.github.underscore.lodash.U;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ezvcard.parameter.TelephoneType;

import static opencontacts.open.com.opencontacts.utils.Common.getOrDefault;

public class VCardUtils {
    public static Map<Integer, TelephoneType> intToTelephoneTypeMap;
    public static Map<TelephoneType, Integer> telephoneTypeToIntMap;
    public static int defaultPhoneNumberType;
    static {
        intToTelephoneTypeMap = new HashMap<>(4);
        intToTelephoneTypeMap.put(0, TelephoneType.CELL);
        intToTelephoneTypeMap.put(1, TelephoneType.HOME);
        intToTelephoneTypeMap.put(2, TelephoneType.WORK);
        intToTelephoneTypeMap.put(3, TelephoneType.FAX);

        telephoneTypeToIntMap = new HashMap<>(4);
        telephoneTypeToIntMap = U.toMap(U.invert(intToTelephoneTypeMap));
        defaultPhoneNumberType = telephoneTypeToIntMap.get(TelephoneType.CELL);
    }

    public static int getTypeOfPhoneNumber(List<TelephoneType> telephoneTypes) {
        TelephoneType telephoneType = telephoneTypes.get(0);
        if(telephoneTypes.contains(TelephoneType.FAX))
            return telephoneTypeToIntMap.get(TelephoneType.FAX);
        return getOrDefault(telephoneTypeToIntMap, telephoneType, defaultPhoneNumberType);
    }
}
