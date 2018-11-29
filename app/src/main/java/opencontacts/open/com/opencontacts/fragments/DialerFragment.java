package opencontacts.open.com.opencontacts.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;

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

    private void linkDialerButtonsToHandlers() {
        dialPadEditText = (EditText) view.findViewById(R.id.editText_dialpad_number);
        view.findViewById(R.id.button_call).setOnClickListener(v -> {
            String phoneNumber = dialPadEditText.getText().toString();
            if(isInvalid(phoneNumber))
                dialPadEditText.setError(getString(R.string.invalid_number));
            else
                AndroidUtils.call(phoneNumber, context);
        });

        view.findViewById(R.id.button_message).setOnClickListener(v -> {
            String phoneNumber = dialPadEditText.getText().toString();
            if(isInvalid(phoneNumber))
                dialPadEditText.setError(getString(R.string.invalid_number));
            else
                AndroidUtils.message(dialPadEditText.getText().toString(), context);
        });

        view.findViewById(R.id.button_add_contact).setOnClickListener(v -> {
            String phoneNumber = dialPadEditText.getText().toString();
            if(isInvalid(phoneNumber))
                dialPadEditText.setError(getString(R.string.invalid_number));
            else
            AndroidUtils.getAlertDialogToAddContact(dialPadEditText.getText().toString(), context).show();
        });
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
