package opencontacts.open.com.opencontacts.components.fieldcollections;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.underscore.U;

import java.util.ArrayList;
import java.util.List;

import opencontacts.open.com.opencontacts.R;

public abstract class InputFieldCollection<H extends FieldViewHolder> extends LinearLayout {

    protected LayoutInflater layoutInflater;
    public List<H> fieldViewHoldersList = new ArrayList<>();
    protected LinearLayout fieldsHolderLayout;
    private View addMoreButton;
    private Runnable onAddMoreClick;

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
        addMoreButton = findViewById(R.id.add_more);
        fieldsHolderLayout = findViewById(R.id.fields_holder);
        addMoreButton.setOnClickListener(x -> {
            addOneMoreView();
            if (onAddMoreClick != null) onAddMoreClick.run();
        });
        consumeAttributes(context, attrs);
    }

    public void removeField(H fieldHolderToRemove) {
        if (!fieldViewHoldersList.contains(fieldHolderToRemove)) {
            Toast.makeText(getContext(), R.string.error, LENGTH_SHORT).show();
            return;
        }
        fieldsHolderLayout.removeView(fieldHolderToRemove.getView());
        fieldViewHoldersList.remove(fieldHolderToRemove);
    }

    private void consumeAttributes(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.InputFieldCollection);
        String title = typedArray.getString(R.styleable.InputFieldCollection_android_title);
        if (!TextUtils.isEmpty(title)) setupTitle(title);
        typedArray.recycle();
    }

    protected void setupTitle(String title) {
        AppCompatTextView titleTextView = findViewById(R.id.title);
        titleTextView.setText(title);
        titleTextView.setVisibility(VISIBLE);
    }

    public H addOneMoreView() {
        H newField = createNewField();
        fieldsHolderLayout.addView(newField.getView());
        fieldViewHoldersList.add(newField);
        return newField;
    }

    public abstract H createNewField();

    public boolean isEmpty() {
        int childCount = fieldViewHoldersList.size();
        if (childCount == 0)
            return true;
        return !U.any(fieldViewHoldersList, fieldViewHolder -> !TextUtils.isEmpty(fieldViewHolder.getValue()));
    }

    public void setOnAddMoreClick(Runnable onAddMoreClick) {
        this.onAddMoreClick = onAddMoreClick;
    }

    public H getFieldAt(int index) {
        return fieldViewHoldersList.get(index);
    }

}
