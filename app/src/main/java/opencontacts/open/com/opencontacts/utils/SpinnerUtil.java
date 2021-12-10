package opencontacts.open.com.opencontacts.utils;


import static opencontacts.open.com.opencontacts.components.TintedDrawablesStore.getTintedDrawable;
import static opencontacts.open.com.opencontacts.utils.PrimitiveDataTypeUtils.toPrimitiveBools;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.github.underscore.U;
import com.reginald.editspinner.EditSpinner;
import com.thomashaertel.widget.MultiSpinner;

import java.util.List;

import opencontacts.open.com.opencontacts.R;

public class SpinnerUtil {
    public static <T> void setSelection(List<T> itemsToSelect, List<T> allItems, MultiSpinner multiSpinner) {
        boolean[] selectionMatrix = toPrimitiveBools(U.map(allItems, itemsToSelect::contains));
        multiSpinner.setSelected(selectionMatrix);
    }

    public static <T> List<T> getSelectedItems(MultiSpinner spinner, List<T> allItems) {
        boolean[] selectionMatrix = spinner.getSelected();
        return U.filterIndexed(allItems, (index, item) -> selectionMatrix[index]);
    }

    public static void setItem(String text, List<String> items, EditSpinner editSpinner) {
        int indexOfType = items.indexOf(text);
        if (indexOfType == -1) editSpinner.setText(text);
        else editSpinner.selectItem(indexOfType);
    }

    public static void setupSpinner(List<String> items, EditSpinner spinner, Context context) {
        spinner.setDropDownDrawable(getTintedDrawable(R.drawable.ic_arrow_drop_down_black_24dp, context));
        spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items));
        if (items.size() > 0) spinner.selectItem(0);
    }
}
