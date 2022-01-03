package opencontacts.open.com.opencontacts.components;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.github.underscore.U;

import java.util.List;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.Common;

public class ExpandedList extends LinearLayout {
    private LayoutInflater layoutInflater;
    private EventListener onItemClickListener;
    private EventListener onItemLongClickListener;

    public ExpandedList(Context context) {
        this(context, null);
    }

    public ExpandedList(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandedList(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        layoutInflater = LayoutInflater.from(context);
        setOrientation(LinearLayout.VERTICAL);
    }

    private void onClick(View v) {
        onItemClickListener.eventOnItem((int) v.getTag());
    }

    public void setOnItemClickListener(EventListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        if (getChildCount() == 0)
            return;
        Common.forEachIndex(getChildCount(), index -> getChildAt(index).setOnClickListener(this::onClick));
    }

    public void setOnItemLongClickListener(EventListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
        if (getChildCount() == 0)
            return;
        Common.forEachIndex(getChildCount(), index -> getChildAt(index).setOnLongClickListener(this::onLongClick));
    }

    private boolean onLongClick(View view) {
        onItemLongClickListener.eventOnItem((int) view.getTag());
        return true;
    }

    public void setItems(List<Pair<String, String>> items) {
        removeAllViews();
        U.forEachIndexed(items, (index, item) -> {
            View inflatedView = layoutInflater.inflate(R.layout.layout_item_title_and_type, this, false);
            ((AppCompatTextView) inflatedView.findViewById(R.id.textview_content)).setText(item.first);
            ((AppCompatTextView) inflatedView.findViewById(R.id.textview_type)).setText(item.second);
            inflatedView.setTag(index);
            if (onItemClickListener != null)
                inflatedView.setOnClickListener(this::onClick);
            addView(inflatedView);
        });
    }

    public static class Builder {
        private ExpandedList expandedList;

        public Builder(Context context) {
            expandedList = new ExpandedList(context);
        }

        public Builder withItems(List<Pair<String, String>> items) {
            expandedList.setItems(items);
            return this;
        }

        public Builder withOnItemClickListener(EventListener onItemClickListener) {
            expandedList.setOnItemClickListener(onItemClickListener);
            return this;
        }

        public Builder withOnItemLongClickListener(EventListener onItemLongClickListener) {
            expandedList.setOnItemLongClickListener(onItemLongClickListener);
            return this;
        }

        public ExpandedList build() {
            return expandedList;
        }
    }

    public interface EventListener {
        void eventOnItem(int index);
    }

}
