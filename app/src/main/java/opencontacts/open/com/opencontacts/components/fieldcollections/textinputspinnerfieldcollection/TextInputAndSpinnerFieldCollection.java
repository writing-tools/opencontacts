package opencontacts.open.com.opencontacts.components.fieldcollections.textinputspinnerfieldcollection;


import static opencontacts.open.com.opencontacts.utils.Common.mapIndexes;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.github.underscore.U;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.fieldcollections.InputFieldCollection;
import opencontacts.open.com.opencontacts.utils.Common;

public class TextInputAndSpinnerFieldCollection extends InputFieldCollection<TextInputAndSpinnerViewHolder> {
    public List<String> fieldTypes;
    public String hint;
    public int inputType;

    public TextInputAndSpinnerFieldCollection(Context context) {
        this(context, null);
    }

    public TextInputAndSpinnerFieldCollection(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextInputAndSpinnerFieldCollection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        processAttributesPassedThroughXML(context, attrs);
    }

    protected void processAttributesPassedThroughXML(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.InputFieldCollection);
        CharSequence[] fieldTypesArray = typedArray.getTextArray(R.styleable.InputFieldCollection_android_entries);
        if (fieldTypesArray != null)
            fieldTypes = Common.mapIndexes(fieldTypesArray.length, index -> fieldTypesArray[index].toString());
        hint = typedArray.getString(R.styleable.InputFieldCollection_android_hint);
        inputType = typedArray.getInt(R.styleable.InputFieldCollection_android_inputType, 0);
        typedArray.recycle();
    }

    @Override
    public TextInputAndSpinnerViewHolder createNewField() {
        View inflatedView = layoutInflater.inflate(R.layout.layout_edit_field_and_type, fieldsHolderLayout, false);
        return new TextInputAndSpinnerViewHolder(hint, inputType, fieldTypes, inflatedView, getContext());
    }

    public List<Pair<String, String>> getValuesAndTypes() {
        int childCount = fieldViewHoldersList.size();
        if (childCount == 0) return null;
        return U.chain(mapIndexes(childCount, index -> fieldViewHoldersList.get(index).getValueAndTypeAsPair()))
            .reject(valueAndTypePair -> TextUtils.isEmpty(valueAndTypePair.first))
            .value();
    }

    public void set(String hint, int inputType, List<String> fieldTypes) {
        this.hint = hint;
        this.inputType = inputType;
        this.fieldTypes = fieldTypes;
    }

    public void setFieldTypes(List<String> fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public void addOneMoreView(String value, String type) {
        addOneMoreView().set(value, type);
    }
}
