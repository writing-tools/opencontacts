package opencontacts.open.com.opencontacts.actions;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;

import opencontacts.open.com.opencontacts.R;
import opencontacts.open.com.opencontacts.utils.AndroidUtils;
import opencontacts.open.com.opencontacts.utils.DomainUtils;

/**
 * Created by sultanm on 1/21/18.
 */

public class ExportMenuItemClickHandler implements MenuItem.OnMenuItemClickListener{
    private Context context;

    public ExportMenuItemClickHandler(Context context) {
        this.context = context;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        new AlertDialog.Builder(context)
                .setMessage(R.string.do_you_want_to_export)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Toast.makeText(context, R.string.exporting_contacts_started, Toast.LENGTH_SHORT).show();
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            try {
                                DomainUtils.exportAllContacts(context);
                            } catch (IOException e) {
                                e.printStackTrace();
                                return false;
                            }
                            return true;
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (Boolean.FALSE.equals(success))
                                AndroidUtils.showAlert(context, context.getString(R.string.failed), context.getString(R.string.failed_exporting_contacts));
                            else
                                Toast.makeText(context, R.string.exporting_contacts_complete, Toast.LENGTH_LONG).show();

                        }
                    }.execute();
                }).setNegativeButton(R.string.no, null).show();
        return true;
    }
}
