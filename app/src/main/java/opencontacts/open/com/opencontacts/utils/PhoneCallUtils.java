package opencontacts.open.com.opencontacts.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.List;

import static android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.callWithSystemDefaultSim;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getCallIntent;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getPreferredSim;

public class PhoneCallUtils {

    public static void callUsingSim(String number, int simIndex, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            callWithSystemDefaultSim(number, context);
            return;
        }
        context.startActivity(getIntentToCallUsingSim(number, simIndex, context));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static Intent getIntentToCallUsingSim(String number, int simIndex, Context context){
        Intent callIntent = getCallIntent(number, context);
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
        int preferredSim = getPreferredSim(context);
        if(preferredSim == -2) {
            callWithSystemDefaultSim(number, context);
            return;
        }
        if(preferredSim == -1) {
            showCallUsingSimDialogAndCall(number, context);
            return;
        }
        callUsingSim(number, preferredSim, context);
    }

    private static void showCallUsingSimDialogAndCall(String number, Context context) {
        new AlertDialog.Builder(context)
                .setItems(new String[]{"Sim 1", "Sim 2"},
                        (dialog, which) -> callUsingSim(number, which, context)
                ).show();
    }

}
