package opencontacts.open.com.opencontacts.fragments;

import static opencontacts.open.com.opencontacts.utils.ThemeUtils.applyOptedTheme;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class AppBaseFragment extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        applyOptedTheme(getActivity());
        super.onCreate(savedInstanceState);
    }

    public boolean handleBackPress() {
        return false;
    }
}
