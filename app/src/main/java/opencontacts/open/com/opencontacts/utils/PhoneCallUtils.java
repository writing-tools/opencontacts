package opencontacts.open.com.opencontacts.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.List;

import static android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE;
import static android.text.TextUtils.isEmpty;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.callWithSystemDefaultSim;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getIntentToCall;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.DEFAULT_SIM_SELECTION_ALWAYS_ASK;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.DEFAULT_SIM_SELECTION_SYSTEM_DEFAULT;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getPreferredSim;

public class PhoneCallUtils {

    public static String sim1Name = "Sim 1";
    public static String sim2Name = "Sim 2";
    public static void callUsingSim(String number, int simIndex, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callWithSystemDefaultSim(number, context);
            return;
        }
        context.startActivity(getIntentToCallUsingSim(number, simIndex, context));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static Intent getIntentToCallUsingSim(String number, int simIndex, Context context){
        Intent callIntent = getIntentToCall(number, context);
        PhoneAccountHandle phoneAccountHandleToCallWith = getPhoneAccountHandleToCallWith(simIndex, context);
        if(phoneAccountHandleToCallWith == null) return callIntent;
        return callIntent.putExtra(EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleToCallWith);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static PhoneAccountHandle getPhoneAccountHandleToCallWith(int simIndex, Context context){
        if(!hasMultipleSims(context)) return null;
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
        if(callCapablePhoneAccounts.size() < 2) return null;
        //added permission check above using util intellij wasn't able to identify it
        return callCapablePhoneAccounts.get(simIndex == 1 ? 1 : 0);
    }

    public static boolean hasMultipleSims(Context context){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false;
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        if(telecomManager == null || !hasPermission(Manifest.permission.READ_PHONE_STATE, context)) return false;
        //added permission check above using util intellij wasn't able to identify it
        return telecomManager.getCallCapablePhoneAccounts().size() > 1;
    }

    public static void handleMultiSimCalling(String number, Context context) {
        String preferredSim = getPreferredSim(context);
        if (DEFAULT_SIM_SELECTION_SYSTEM_DEFAULT.equals(preferredSim)) {
            callWithSystemDefaultSim(number, context);
            return;
        }
        if (DEFAULT_SIM_SELECTION_ALWAYS_ASK.equals(preferredSim)) {
            showCallUsingSimDialogAndCall(number, context);
            return;
        }
        callUsingSim(number, Integer.parseInt(preferredSim), context);
    }

    private static void showCallUsingSimDialogAndCall(String number, Context context) {
        String[] simNames = getSimNames(context);
        new AlertDialog.Builder(context)
                .setItems(simNames,
                        (dialog, which) -> callUsingSim(number, which, context)
                ).show();
    }

    @SuppressLint(value = {"MissingPermission", "NewApi"})
    // supressing these as hasMultipleSims method takes care of these
    public static String[] getSimNames(Context context) {
        String[] simNames = new String[]{sim1Name, sim2Name};
        if(!hasMultipleSims(context)) return simNames;
        SubscriptionManager localSubscriptionManager = SubscriptionManager.from(context);
        if (localSubscriptionManager.getActiveSubscriptionInfoCount() <= 1) return simNames;
        List localList = localSubscriptionManager.getActiveSubscriptionInfoList();
        SubscriptionInfo simInfo = (SubscriptionInfo) localList.get(0);
        SubscriptionInfo simInfo1 = (SubscriptionInfo) localList.get(1);

        final String sim1Name = simInfo.getDisplayName().toString();
        final String sim2Name = simInfo1.getDisplayName().toString();
        return new String[]{isEmpty(sim1Name)? "Sim 1" : sim1Name, isEmpty(sim2Name)? "Sim 2" : sim2Name};
    }

}
