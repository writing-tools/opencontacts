package opencontacts.open.com.opencontacts.components.fieldcollections;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.R;

public abstract class InputFieldCollection<H extends FieldViewHolder> extends LinearLayout {

    protected LayoutInflater layoutInflater;
    public List<H> fieldViewHoldersList = new ArrayList<>();
    protected LinearLayout fieldsHolderLayout;

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
        addMoreButton.setOnClickListener(x -> addOneMoreView());
        processAttributesPassedThroughXML(context, attrs);
    }

    protected abstract void processAttributesPassedThroughXML(Context context, @Nullable AttributeSet attrs);

    public abstract FieldViewHolder addOneMoreView();

    public boolean isEmpty(){
        int childCount = fieldViewHoldersList.size();
        if(childCount == 0)
            return true;
        return !U.any(fieldViewHoldersList, fieldViewHolder -> !TextUtils.isEmpty(fieldViewHolder.getValue()));
    }

    public H getFieldAt(int index){
        return fieldViewHoldersList.get(index);
    }

}
