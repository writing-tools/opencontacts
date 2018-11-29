package opencontacts.open.com.opencontacts.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


import opencontacts.open.com.opencontacts.CallLogListView;
import opencontacts.open.com.opencontacts.data.datastore.CallLogDataStore;
import opencontacts.open.com.opencontacts.interfaces.SelectableTab;

public class CallLogFragment extends AppBaseFragment implements SelectableTab {
    private CallLogListView callLogListView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callLogListView = new CallLogListView(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        LinearLayout linearLayout = new LinearLayout(context);
        final SwipeRefreshLayout swipeRefreshLayout = new SwipeRefreshLayout(context);
        callLogListView.setId(android.R.id.list);
        swipeRefreshLayout.addView(callLogListView);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if(callLogListView.getCount() == 0)
                callLogListView.reload();
            else
                CallLogDataStore.loadRecentCallLogEntriesAsync(context);
            swipeRefreshLayout.setRefreshing(false);
        });
        linearLayout.addView(swipeRefreshLayout);
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

    }
}