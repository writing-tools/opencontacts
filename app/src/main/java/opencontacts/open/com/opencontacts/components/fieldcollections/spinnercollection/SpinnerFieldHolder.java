package opencontacts.open.com.opencontacts.components.fieldcollections.spinnercollection;

import static opencontacts.open.com.opencontacts.utils.SpinnerUtil.setItem;
import static opencontacts.open.com.opencontacts.utils.SpinnerUtil.setupSpinner;

import android.content.Context;
import android.view.View;

import com.reginald.editspinner.EditSpinner;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.ImageButtonWithTint;
import opencontacts.open.com.opencontacts.components.fieldcollections.FieldViewHolder;

public class SpinnerFieldHolder extends FieldViewHolder {

    private final EditSpinner spinner;
    private final List<String> options;
    private final ImageButtonWithTint deleteButton;
    private View fieldView;

    SpinnerFieldHolder(List<String> options, boolean editDisabled, View fieldView, Context context) {
        spinner = fieldView.findViewById(R.id.spinner);
        deleteButton = fieldView.findViewById(R.id.delete);
        this.fieldView = fieldView;
        if (editDisabled) spinner.setEditable(false);
        this.options = options;
        setupSpinner(options, spinner, context);
    }

    public void set(String option) {
        setItem(option, options, spinner);
    }

    public void setOnDelete(View.OnClickListener onClickListener) {
        deleteButton.setOnClickListener(onClickListener);
    }

    @Override
    public String getValue() {
        return spinner.getText().toString();
    }

    @Override
    public View getView() {
        return fieldView;
    }
}
