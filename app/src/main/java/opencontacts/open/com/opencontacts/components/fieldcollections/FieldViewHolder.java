package opencontacts.open.com.opencontacts.components.fieldcollections;

import android.support.annotation.NonNull;
import android.view.View;

public abstract class FieldViewHolder {
    public abstract String getValue();

    @NonNull
    public abstract View getView();
}
