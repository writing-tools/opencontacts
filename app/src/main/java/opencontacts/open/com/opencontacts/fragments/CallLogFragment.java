package opencontacts.open.com.opencontacts.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import opencontacts.open.com.opencontacts.CallLogListView;
import opencontacts.open.com.opencontacts.interfaces.EditNumberBeforeCallHandler;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;

public class CallLogFragment extends AppBaseFragment implements SelectableTab {
    private CallLogListView callLogListView;
    private EditNumberBeforeCallHandler editNumberBeforeCallHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callLogListView = new CallLogListView(getContext(), editNumberBeforeCallHandler);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.addView(callLogListView);
        return linearLayout;
    }

    @Override
    public void onDestroy() {
        callLogListView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSelect() {}

    @Override
    public void onUnSelect() {
        if(callLogListView == null) return;
        callLogListView.exitSelectionMode();
    }

    @Override
    public boolean handleBackPress() {
        if (!callLogListView.isInSelectionMode()) return false;
        callLogListView.exitSelectionMode();
        return true;
    }

    public void setEditNumberBeforeCallHandler(EditNumberBeforeCallHandler editNumberBeforeCallHandler) {
        this.editNumberBeforeCallHandler = editNumberBeforeCallHandler;
        if(callLogListView != null) callLogListView.setEditNumberBeforeCallHandler(editNumberBeforeCallHandler);
    }
}