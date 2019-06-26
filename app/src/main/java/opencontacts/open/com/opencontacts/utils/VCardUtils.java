package opencontacts.open.com.opencontacts.utils;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import java.util.List;

import ezvcard.VCard;
import ezvcard.property.FormattedName;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import opencontacts.open.com.opencontacts.R;

public class VCardUtils {

    private static String noNameString;

    @NonNull
    public static Pair<String, String> getNameFromVCard(VCard vcard, Context context) {
        if(noNameString == null) noNameString = context.getString(R.string.noname);
        Pair<String, String> name;
        StructuredName structuredName = vcard.getStructuredName();
        FormattedName formattedName = vcard.getFormattedName();
        if (structuredName == null)
            if (formattedName == null) {
                name = new Pair<>(noNameString, "");
            } else name = new Pair<>(formattedName.getValue(), "");
        else name = getNameFromStructureNameOfVcard(structuredName);
        return name;
    }

    private static Pair<String, String> getNameFromStructureNameOfVcard(StructuredName structuredName) {
        List<String> additionalNames = structuredName.getAdditionalNames();
        String lastName = structuredName.getFamily();
        if (additionalNames.size() > 0) {
            StringBuilder nameBuffer = new StringBuilder();
            for (String additionalName : additionalNames)
                nameBuffer.append(additionalName).append(" ");
            lastName = nameBuffer.append(structuredName.getFamily()).toString();
        }
        return new Pair<>(structuredName.getGiven(), lastName);
    }

    public static String getMobileNumber(Telephone telephone){
        String telephoneText = telephone.getText();
        return telephoneText == null ? telephone.getUri().getNumber() : telephoneText;
    }

    public static void setFormattedNameIfNotPresent(VCard vcard) {
        if(vcard.getFormattedName() != null) return;
        StructuredName structuredName = vcard.getStructuredName();
        if(structuredName == null) vcard.setFormattedName("");
        else vcard.setFormattedName(structuredName.getFamily() + " "  + structuredName.getGiven());
    }
}
