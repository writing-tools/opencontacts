package opencontacts.open.com.opencontacts.activities;

import static java.util.Arrays.asList;
import static opencontacts.open.com.opencontacts.utils.AndroidUtils.goToUrl;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import opencontacts.open.com.opencontacts.BuildConfig;
import opencontacts.open.com.opencontacts.R;

public class AboutActivity extends AppBaseActivity {
    @Override
    int getLayoutResource() {
        return R.layout.activity_about;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((AppCompatTextView) findViewById(R.id.version)).setText(BuildConfig.VERSION_NAME);
        ListView contributorsList = findViewById(R.id.contributors_list);
        List<String> names = asList(getResources().getStringArray(R.array.contributor_names));
        List<String> urls = asList(getResources().getStringArray(R.array.contributor_urls));
        contributorsList.setAdapter(new ArrayAdapter<String>(this, R.layout.layout_clickable_text_view_item, names) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                AppCompatTextView textView = (AppCompatTextView) super.getView(position, convertView, parent);
                textView.setText(names.get(position));
                textView.setOnClickListener(v -> goToUrl(urls.get(position), AboutActivity.this));
                return textView;
            }
        });
    }
}
