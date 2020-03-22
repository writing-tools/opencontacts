package opencontacts.open.com.opencontacts.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import com.github.underscore.U;
import com.reginald.editspinner.EditSpinner;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.Common;

import static opencontacts.open.com.opencontacts.utils.Common.mapIndexes;

public class InputFieldCollection extends LinearLayout {

    private LayoutInflater layoutInflater;
    public List<String> fieldTypes;
    public List<FieldViewHolder> fieldViewHoldersList = new ArrayList<>(1);
    public String hint;
    public int inputType;
    private LinearLayout fieldsHolderLayout;

    public InputFieldCollection(Context context) {
        this(context, null);
    }

    public InputFieldCollection(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InputFieldCollection(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.layout_field_collection, this, true);
        View addMoreButton = findViewById(R.id.add_more);
        fieldsHolderLayout = findViewById(R.id.fields_holder);
        addMoreButton.setOnClickListener(this::addOneMoreView);
        processAttributesPassedThroughXML(context, attrs);
    }

    private void processAttributesPassedThroughXML(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.InputFieldCollection);
        CharSequence[] fieldTypesArray = typedArray.getTextArray(R.styleable.InputFieldCollection_android_entries);
        if(fieldTypesArray != null) fieldTypes = Common.mapIndexes(fieldTypesArray.length, index -> fieldTypesArray[index].toString());
        hint = typedArray.getString(R.styleable.InputFieldCollection_android_hint);
        inputType = typedArray.getInt(R.styleable.InputFieldCollection_android_inputType, 0);
        typedArray.recycle();
    }

    public void set(String hint, int inputType, List<String> fieldTypes){
        this.hint = hint;
        this.inputType = inputType;
        this.fieldTypes = fieldTypes;
    }

    public FieldViewHolder addOneMoreView(View view) {
        View inflatedView = layoutInflater.inflate(R.layout.layout_edit_field_and_type, fieldsHolderLayout, false);
        fieldsHolderLayout.addView(inflatedView);
        FieldViewHolder fieldViewHolder = new FieldViewHolder(hint, inputType, fieldTypes, inflatedView, getContext());
        fieldViewHoldersList.add(fieldViewHolder);
        return fieldViewHolder;
    }

    public boolean isEmpty(){
        int childCount = fieldViewHoldersList.size();
        if(childCount == 0)
            return true;
        return !U.any(fieldViewHoldersList, fieldViewHolder -> !TextUtils.isEmpty(fieldViewHolder.getValue()));
    }

    public FieldViewHolder getFieldAt(int index) {
        return fieldViewHoldersList.get(index);
    }

    public void addOneMoreView(String value, String type) {
        addOneMoreView(null).set(value, type);
    }

    public List<Pair<String, String>> getValuesAndTypes() {
        int childCount = fieldViewHoldersList.size();
        if(childCount == 0) return null;
        return U.chain(mapIndexes(childCount, index -> fieldViewHoldersList.get(index).getValueAndTypeAsPair()))
                .reject(valueAndTypePair -> TextUtils.isEmpty(valueAndTypePair.first))
                .value();
    }

    public class FieldViewHolder {
        public TextInputEditText editText;
        public EditSpinner spinner;
        private List<String> types;

        FieldViewHolder(String hint, int inputType, List<String> types, View fieldView, Context context){
            editText = fieldView.findViewById(R.id.edit_field);
            spinner = fieldView.findViewById(R.id.type_spinner);
            ((TextInputLayout)fieldView.findViewById(R.id.text_input_layout)).setHint(hint);
            editText.setInputType(inputType);
            this.types = types;
            spinner.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, this.types));
            if(types.size() > 0) spinner.selectItem(0);

        }

        public void set(String value, String type) {
            editText.setText(value);
            int indexOfType = types.indexOf(type);
            if(indexOfType == -1) spinner.setText(type);
            else spinner.selectItem(indexOfType);
        }

        public String getValue(){
            return editText.getText().toString();
        }

        public Pair<String, String> getValueAndTypeAsPair() {
            int indexOfSelectedValue = spinner.getListSelection();
            return new Pair<>(getValue(), indexOfSelectedValue == -1 ?
                    spinner.getText().toString()
                    : types.get(indexOfSelectedValue));
        }
    }
}
