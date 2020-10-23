package opencontacts.open.com.opencontacts.utils;


import com.github.underscore.U;
import com.thomashaertel.widget.MultiSpinner;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

public class MultiSpinnerUtil {
    public static void setSelection(List<String> itemsToSelect, List<String> allItems, MultiSpinner multiSpinner) {
        boolean[] selectionMatrix = ArrayUtils.toPrimitive(U.map(allItems, itemsToSelect::contains).toArray(new Boolean[]{}));
        multiSpinner.setSelected(selectionMatrix);
    }

    public static <T> List<T> getSelectedItems(MultiSpinner spinner, List<T> allItems) {
        boolean[] selectionMatrix = spinner.getSelected();
        return U.filterIndexed(allItems, (index, item) -> selectionMatrix[index]);
    }
}
