package opencontacts.open.com.opencontacts.activities.services;

import static android.telecom.Call.Details.DIRECTION_OUTGOING;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isCallFilteringEnabled;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.shouldBlockCalls;

import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import opencontacts.open.com.opencontacts.data.datastore.ContactsDataStore;
import opencontacts.open.com.opencontacts.orm.Contact;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class CallScreeningServiceImplementation extends CallScreeningService {
    CallResponse reject = new CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build();

    CallResponse silence = new CallResponse.Builder()
        .setDisallowCall(false)
        .setSilenceCall(true)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build();

    CallResponse allow = new CallResponse.Builder()
        .build();

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        if (callDetails.getCallDirection() == DIRECTION_OUTGOING || !isCallFilteringEnabled(this)) {
            respondToCall(callDetails, allow);
            return;
        }
        String callingPhonenumber = callDetails.getHandle().getSchemeSpecificPart();
        Contact probableContact = ContactsDataStore.getContact(callingPhonenumber);
        if (probableContact == null)
            respondToCall(callDetails, shouldBlockCalls(this) ? reject : silence);
        else respondToCall(callDetails, allow);
    }
}
