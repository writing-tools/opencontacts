package opencontacts.open.com.opencontacts.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import opencontacts.open.com.opencontacts.utils.AndroidUtils;

public class AppBaseFragment extends Fragment{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidUtils.applyOptedTheme(getContext());
        super.onCreate(savedInstanceState);
    }

    public boolean handleBackPress() {
        return false;
    }
}
