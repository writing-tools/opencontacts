package opencontacts.open.com.opencontacts.activities;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.Toast;

import com.github.underscore.U;

import java.util.HashMap;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.components.TintedDrawablesStore;

import static opencontacts.open.com.opencontacts.utils.AndroidUtils.isWhatsappInstalled;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.showAlert;
import static opencontacts.open.com.opencontacts.utils.PhoneCallUtils.hasMultipleSims;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.COMMON_SHARED_PREFS_FILE_NAME;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.IS_DARK_THEME_ACTIVE_PREFERENCES_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.SIM_PREFERENCE_SHARED_PREF_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.T9_SEARCH_ENABLED_SHARED_PREF_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.disableWhatsappIntegration;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.enableWhatsappIntegration;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getDefaultWhatsAppCountryCode;

public class PreferencesActivity extends AppBaseActivity {

    public static final String PREFERENCE_FRAGMENT_TRANSACTION_TAG = "preference";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        if(!supportFragmentManager.getFragments().isEmpty()) return;
        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, new PreferencesFragment(), PREFERENCE_FRAGMENT_TRANSACTION_TAG)
                .commit();
    }

    @Override
    int getLayoutResource() {
        return R.layout.activity_preferences;
    }


    public static class PreferencesFragment extends PreferenceFragmentCompat
    {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setSharedPreferencesName(COMMON_SHARED_PREFS_FILE_NAME);
            addPreferencesFromResource(R.xml.app_preferences);
            if(hasMultipleSims(getContext())) addSimPreference();
            handlePreferenceUpdates();
        }

        private void addSimPreference() {
            ContextThemeWrapper contextThemeWrapper = getContextThemeWrapper();// crazy android hack to get theme wrapped content. Else the dynamically created preferences are failing to create.

            ListPreference listPreference = new ListPreference(contextThemeWrapper);
            listPreference.setEntries(R.array.sim_selection);
            listPreference.setTitle(R.string.default_sim_calls_preference_title);
            listPreference.setSummary("%s");
            listPreference.setEntryValues(R.array.sim_selection_values);
            listPreference.setDefaultValue("-2");
            listPreference.setKey(SIM_PREFERENCE_SHARED_PREF_KEY);
            getPreferenceScreen().addPreference(listPreference);
        }

        @NonNull
        private ContextThemeWrapper getContextThemeWrapper() {
            TypedValue themeTypedValue = new TypedValue();
            Context context = getContext();
            context.getTheme().resolveAttribute(R.attr.preferenceTheme, themeTypedValue, true);
            return new ContextThemeWrapper(context, themeTypedValue.resourceId);
        }

        private void handlePreferenceUpdates() {
            HashMap<String, Preference.OnPreferenceChangeListener> onPreferenceChangeHandlersMap = getIndividualPreferenceHandlersMap();
            U.forEach(onPreferenceChangeHandlersMap.keySet(),
                    preferenceKey -> findPreference(preferenceKey)
                            .setOnPreferenceChangeListener(
                                    onPreferenceChangeHandlersMap.get(preferenceKey)
                            ));
        }

        @NonNull
        private HashMap<String, Preference.OnPreferenceChangeListener> getIndividualPreferenceHandlersMap() {
            Activity activity = PreferencesFragment.this.getActivity();
            HashMap<String, Preference.OnPreferenceChangeListener> onPreferenceChangeHandlersMap = new HashMap<>();
            onPreferenceChangeHandlersMap.put(IS_DARK_THEME_ACTIVE_PREFERENCES_KEY, (preference, newValue) -> {
                TintedDrawablesStore.reset();
                activity.recreate();
                return true;
            });
            onPreferenceChangeHandlersMap.put(T9_SEARCH_ENABLED_SHARED_PREF_KEY, (preference, newValue) -> {
                activity.recreate();
                return true;
            });

            onPreferenceChangeHandlersMap.put(WHATSAPP_INTEGRATION_ENABLED_PREFERENCE_KEY, (preference, newValue) -> {
                if(newValue.equals(false)) return true;
                if(!isWhatsappInstalled(activity)) {
                    Toast.makeText(activity, R.string.whatsapp_not_installed, Toast.LENGTH_LONG).show();
                    return false;
                }
                showSetDefaultCountryCodeDialog(activity);
                return true;
            });
            return onPreferenceChangeHandlersMap;
        }

        private void showSetDefaultCountryCodeDialog(Context context) {
            AppCompatEditText countryCodeEditText = new AppCompatEditText(context);
            countryCodeEditText.setText(getDefaultWhatsAppCountryCode(context));
            countryCodeEditText.setInputType(InputType.TYPE_CLASS_PHONE);
            new AlertDialog.Builder(context)
                    .setView(countryCodeEditText)
                    .setTitle(R.string.input_country_calling_code_title)
                    .setMessage(R.string.input_country_calling_code_description)
                    .setPositiveButton(R.string.enable_whatsapp_integration, (dialogInterface, i) -> {
                        if(!isWhatsappInstalled(context)) {
                            showAlert(context, getString(R.string.whatsapp_not_installed), getString(R.string.enable_only_after_installing_whatsapp));
                            return;
                        }
                        enableWhatsappIntegration(countryCodeEditText.getText().toString(), context);
                    })
                    .setNegativeButton(R.string.disable_whatsapp_integration, (ignore_x, ignore_y) -> {
                        disableWhatsappIntegration(context);
                        getActivity().recreate();// recreating coz preference fragment is not able to read the disabled preference and still shows enable.
                    })
                    .show();
        }
    }
}
