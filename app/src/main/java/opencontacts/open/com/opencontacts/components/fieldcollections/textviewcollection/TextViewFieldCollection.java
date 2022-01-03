package opencontacts.open.com.opencontacts.components.fieldcollections.textviewcollection;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.InputFieldCollection;

public class TextViewFieldCollection extends InputFieldCollection<TextViewViewHolder> {
    private Consumer<Integer> onClickForTextView;

    public TextViewFieldCollection(Context context) {
        this(context, null);
    }

    public TextViewFieldCollection(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextViewFieldCollection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public TextViewViewHolder createNewField() {
        View inflatedView = LayoutInflater.from(getContext()).inflate(R.layout.component_fieldcollection_textview, null);
        TextViewViewHolder fieldHolder = new TextViewViewHolder(inflatedView);
        fieldHolder.setOnEdit(v -> {
            if (onClickForTextView == null) return;
            onClickForTextView.accept(fieldViewHoldersList.indexOf(fieldHolder)); //index is calculated everytime coz fields might be deleted runtime
        });
        fieldHolder.setOnDelete(() -> removeField(fieldHolder));
        return fieldHolder;
    }

    public void setOnEdit(Consumer<Integer> onClickForTextView) {
        this.onClickForTextView = onClickForTextView;
    }

}
