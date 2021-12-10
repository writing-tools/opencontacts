package opencontacts.open.com.opencontacts.data.datastore;

import static opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore.cautiouslyGetContactFromDatabase;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getStringFromPreferences;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.updatePreference;
import static opencontacts.open.com.opencontacts.utils.Common.checkNotNull;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING;

import android.content.Context;

import com.github.underscore.lodash.U;

import java.util.Map;

import opencontacts.open.com.opencontacts.domain.Contact;
import opencontacts.open.com.opencontacts.utils.Common;

public class SingleContactWidgetDataStore {

    public static void saveSingleContactWidget(int widgetId, long contactId, Context context) {
        Map<String, Long> singleContactWidgetToContactMapping = getSingleContactWidgetIdToContactMap(context);
        singleContactWidgetToContactMapping.put(String.valueOf(widgetId), contactId);
        updatePreference(SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING, U.toJson(singleContactWidgetToContactMapping), context);
    }

    public static Contact getContactForSingleContactWidget(int widgetId, Context context) throws Exception {
        Map<String, Long> singleContactWidgetToContactMapping = getSingleContactWidgetIdToContactMap(context);
        Long contactId = checkNotNull(singleContactWidgetToContactMapping.get(String.valueOf(widgetId)));
        return cautiouslyGetContactFromDatabase(contactId);
    }

    public static void removeSingleContactWidgets(int[] widgetIds, Context context) {
        Map<String, Long> singleContactWidgetIdToContactMap = getSingleContactWidgetIdToContactMap(context);
        Common.forEach(widgetIds, id -> singleContactWidgetIdToContactMap.remove(String.valueOf(id)));
        updatePreference(SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING, U.toJson(singleContactWidgetIdToContactMap), context);
    }

    public static void replaceOldWithNewWidgetIds(int[] oldWidgetIds, int[] newWidgetIds, Context context) {
        Map<String, Long> singleContactWidgetIdToContactMap = getSingleContactWidgetIdToContactMap(context);
        Common.forEachIndex(oldWidgetIds.length, index -> {
            String oldWidgetId = String.valueOf(oldWidgetIds[index]);
            Long contactId = singleContactWidgetIdToContactMap.get(oldWidgetId);
            if (contactId == null) return;
            singleContactWidgetIdToContactMap.remove(oldWidgetId);
            singleContactWidgetIdToContactMap.put(String.valueOf(newWidgetIds[index]), contactId);
        });
    }

    private static Map<String, Long> getSingleContactWidgetIdToContactMap(Context context) {
        String singleContactWidgetToContactMappingString = getStringFromPreferences(SINGLE_CONTACT_WIDGET_TO_CONTACT_MAPPING, "{}", context);
        return (Map<String, Long>) U.fromJson(singleContactWidgetToContactMappingString);
    }

}
