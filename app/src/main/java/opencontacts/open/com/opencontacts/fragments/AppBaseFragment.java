package opencontacts.open.com.opencontacts.fragments;

import static opencontacts.open.com.opencontacts.utils.ThemeUtils.applyOptedTheme;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
