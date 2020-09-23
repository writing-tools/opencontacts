package opencontacts.open.com.opencontacts.components.fieldcollections.spinnercollection;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;

import com.reginald.editspinner.EditSpinner;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.FieldViewHolder;

public class SpinnerFieldHolder extends FieldViewHolder {

    private final EditSpinner spinner;
    private final List<String> options;

    SpinnerFieldHolder(List<String> options, boolean editDisabled, View fieldView, Context context) {
        spinner = fieldView.findViewById(R.id.spinner);
        if(editDisabled) spinner.setEditable(false);
        this.options = options;
        setupSpinner(options, context);
    }

    private void setupSpinner(List<String> options, Context context) {
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, this.options));
        if (options.size() > 0) spinner.selectItem(0);
    }

    public void set(String option) {
        int indexOfType = options.indexOf(option);
        if (indexOfType == -1) spinner.setText(option);
        else spinner.selectItem(indexOfType);
    }

    @Override
    public String getValue() {
        return spinner.getText().toString();
    }
}
