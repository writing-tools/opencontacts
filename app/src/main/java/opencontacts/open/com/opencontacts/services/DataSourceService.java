package opencontacts.open.com.opencontacts.services;

import static open.com.opencontactsdatasourcecontract.Contract.formErrorResponseV1;
import static opencontacts.open.com.opencontacts.services.ContractMethodImpls.validateAndCall;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.isValidAuthCode;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.opencsv.CSVReader;

import java.io.StringReader;

import open.com.opencontactsdatasourcecontract.ContactsDataStoreService;

public class DataSourceService extends Service {
    ContactsDataStoreService.Stub service = new ContactsDataStoreService.Stub() {

        @Override
        public String call(String authCode, String args) {
            try {
                PackageManager packageManager = getPackageManager();
                String callingPackage = packageManager.getNameForUid(Binder.getCallingUid());

                if (!isValidAuthCode(DataSourceService.this, callingPackage, authCode)) {
                    System.out.println("Invalid auth yolo");
                    return formErrorResponseV1("Invalid auth");
                }

                System.out.println("called service yolo");
                String[] arguments = new CSVReader(new StringReader(args)).readNext();
                return validateAndCall(callingPackage, arguments, DataSourceService.this);
            } catch (Exception e) {
                e.printStackTrace();
                return formErrorResponseV1("Unexpected error occurred");
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("on bind called yolo");
        return service;
    }
}
