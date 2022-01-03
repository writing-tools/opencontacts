package opencontacts.open.com.opencontacts.components.fieldcollections.textinputspinnerfieldcollection;

import static opencontacts.open.com.opencontacts.utils.SpinnerUtil.setItem;
import static opencontacts.open.com.opencontacts.utils.SpinnerUtil.setupSpinner;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.util.Pair;
import android.view.View;

import com.reginald.editspinner.EditSpinner;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.FieldViewHolder;


public class TextInputAndSpinnerViewHolder extends FieldViewHolder {
    public TextInputEditText editText;
    public EditSpinner spinner;
    private List<String> types;
    private View fieldView;

    TextInputAndSpinnerViewHolder(String hint, int inputType, List<String> types, View fieldView, Context context) {
        editText = fieldView.findViewById(R.id.edit_field);
        spinner = fieldView.findViewById(R.id.type_spinner);
        this.types = types;
        this.fieldView = fieldView;
        setupTextInput(hint, inputType, fieldView);
        setupSpinner(types, spinner, context);
    }

    private void setupTextInput(String hint, int inputType, View fieldView) {
        ((TextInputLayout) fieldView.findViewById(R.id.text_input_layout)).setHint(hint);
        editText.setInputType(inputType);
    }

    public void set(String value, String type) {
        editText.setText(value);
        setItem(type, types, spinner);
    }

    @Override
    public String getValue() {
        return editText.getText().toString();
    }

    @NonNull
    @Override
    public View getView() {
        return fieldView;
    }

    public Pair<String, String> getValueAndTypeAsPair() {
        int indexOfSelectedValue = spinner.getListSelection();
        return new Pair<>(getValue(), indexOfSelectedValue == -1 ?
            spinner.getText().toString()
            : types.get(indexOfSelectedValue));
    }
}
