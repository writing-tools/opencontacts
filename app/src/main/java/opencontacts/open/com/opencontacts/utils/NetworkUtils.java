package opencontacts.open.com.opencontacts.utils;

import android.support.annotation.NonNull;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;

public class NetworkUtils {

    private static OkHttpClient okHttpClient;

    @NonNull
    public static OkHttpClient getHttpClientWithBasicAuth(String username, String password) {
        if(okHttpClient != null)
            return okHttpClient;
        return okHttpClient = new OkHttpClient.Builder()
                .followRedirects(true)
                .authenticator((route, response) -> {
                    String basicAuthentication = Credentials.basic(username, password);
                    return response.request()
                            .newBuilder()
                            .addHeader("Authorization", basicAuthentication)
                            .build();
                }).build();
    }

}
