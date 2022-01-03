package opencontacts.open.com.opencontacts.components.fieldcollections.textviewcollection;

import static opencontacts.open.com.opencontacts.utils.DomainUtils.formatAddressToAMultiLineString;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import android.view.View;
import android.view.View.OnClickListener;

import ezvcard.property.Address;
import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.FieldViewHolder;

public class TextViewViewHolder extends FieldViewHolder {

    private final AppCompatTextView contentView;
    private final View inflatedView;
    private final View editIcon;
    private final View deleteIcon;
    private final AppCompatTextView titleView;

    public TextViewViewHolder(View inflatedView) {
        this.inflatedView = inflatedView;
        contentView = inflatedView.findViewById(R.id.content);
        titleView = inflatedView.findViewById(R.id.title);
        editIcon = inflatedView.findViewById(R.id.edit_image);
        deleteIcon = inflatedView.findViewById(R.id.delete);
    }

    @Override
    public String getValue() {
        return contentView.getText().toString();
    }

    public TextViewViewHolder setValue(Address address) {
        contentView.setText(formatAddressToAMultiLineString(address, inflatedView.getContext()));
        return this;
    }

    public TextViewViewHolder setTitle(String title) {
        titleView.setText(title);
        return this;
    }

    @NonNull
    @Override
    public View getView() {
        return inflatedView;
    }

    public void setOnEdit(OnClickListener onClickListener) {
        editIcon.setOnClickListener(onClickListener);
    }

    public void setOnDelete(Runnable onDelete) {
        deleteIcon.setOnClickListener(v -> onDelete.run());
    }
}
