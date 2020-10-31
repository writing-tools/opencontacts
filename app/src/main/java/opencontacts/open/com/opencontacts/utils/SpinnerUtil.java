package opencontacts.open.com.opencontacts.utils;


import com.github.underscore.U;
import com.reginald.editspinner.EditSpinner;
import com.thomashaertel.widget.MultiSpinner;

import java.util.List;

import static opencontacts.open.com.opencontacts.utils.PrimitiveDataTypeUtils.toPrimitiveBools;

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
}
