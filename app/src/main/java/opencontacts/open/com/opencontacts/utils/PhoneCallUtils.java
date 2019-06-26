package opencontacts.open.com.opencontacts.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.List;

import static android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.getCallIntent;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;

public class PhoneCallUtils {

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void callUsingSim(String number, int simIndex, Context context) {
        context.startActivity(getIntentToCallUsingSim(number, simIndex, context));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static Intent getIntentToCallUsingSim(String number, int simIndex, Context context){
        Intent callIntent = getCallIntent(number, context);
        PhoneAccountHandle phoneAccountHandleToCallWith = getPhoneAccountHandleToCallWith(simIndex, context);
        if(phoneAccountHandleToCallWith == null) return callIntent;
        return callIntent.putExtra(EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandleToCallWith);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static PhoneAccountHandle getPhoneAccountHandleToCallWith(int simIndex, Context context){
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE, context) || telecomManager == null) return null;
        List<PhoneAccountHandle> callCapablePhoneAccounts = telecomManager.getCallCapablePhoneAccounts();
        if(callCapablePhoneAccounts.size() < 2) return null;
        int absoluteSimIndex = Math.abs(simIndex);
        //added permission check above using util intellij wasn't able to identify it
        return callCapablePhoneAccounts.get(absoluteSimIndex > 1 ? 0 : absoluteSimIndex);
    }

}
