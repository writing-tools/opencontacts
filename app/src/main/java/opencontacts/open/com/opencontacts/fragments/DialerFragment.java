package opencontacts.open.com.opencontacts.fragments;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.github.underscore.Consumer;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.PhoneCallUtils;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.hasPermission;

public class DialerFragment extends AppBaseFragment implements SelectableTab {
    private Context context;
    private View view;
    private EditText dialPadEditText;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        this.context = getContext();
        this.view = view;
        linkDialerButtonsToHandlers();
        super.onViewCreated(view, savedInstanceState);
    }

    public void setNumber(String number) {
        dialPadEditText.setText(number);
    }

    private void linkDialerButtonsToHandlers() {
        dialPadEditText = view.findViewById(R.id.editText_dialpad_number);

        view.findViewById(R.id.button_call).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.call(phoneNumber, context)));

        view.findViewById(R.id.button_whatsapp).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.whatsapp(phoneNumber, context)));

        view.findViewById(R.id.button_message).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.message(phoneNumber, context)));

        view.findViewById(R.id.button_add_contact).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> AndroidUtils.getAlertDialogToAddContact(phoneNumber, context).show()));

        view.findViewById(R.id.button_call_sim1).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 0, context)));

        view.findViewById(R.id.button_call_sim2).setOnClickListener(v -> performActionIfPhoneNumberIsValidElseShowError(phoneNumber -> PhoneCallUtils.callUsingSim(phoneNumber, 1, context)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
            if(telecomManager == null || !hasPermission(Manifest.permission.READ_PHONE_STATE, context)) return;
            //added permission check above using util intellij wasn't able to identify it
            if(telecomManager.getCallCapablePhoneAccounts().size() < 2) return;
            showMultiSimDialingButtons();
        }
    }

    private void showMultiSimDialingButtons() {
        view.findViewById(R.id.button_call_sim1).setVisibility(View.VISIBLE);
        view.findViewById(R.id.button_call_sim2).setVisibility(View.VISIBLE);
        view.findViewById(R.id.text_call_sim1).setVisibility(View.VISIBLE);
        view.findViewById(R.id.text_call_sim2).setVisibility(View.VISIBLE);
    }

    private void performActionIfPhoneNumberIsValidElseShowError(Consumer<String> action) {
        String phoneNumber = dialPadEditText.getText().toString();
        if(isInvalid(phoneNumber))
            dialPadEditText.setError(getString(R.string.invalid_number));
        else
            action.accept(phoneNumber);
    }

    private boolean isInvalid(String phoneNumber) {
        return TextUtils.isEmpty(phoneNumber) || TextUtils.getTrimmedLength(phoneNumber) == 0;
    }


    @Override
    public void onSelect() {
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.showSoftKeyboard(editText, context);
    }

    @Override
    public void onUnSelect() {
        EditText editText = dialPadEditText == null ? (EditText) view.findViewById(R.id.editText_dialpad_number) : dialPadEditText;
        AndroidUtils.hideSoftKeyboard(editText, context);
    }
}
