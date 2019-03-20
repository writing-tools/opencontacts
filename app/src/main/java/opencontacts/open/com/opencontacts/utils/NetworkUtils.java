package opencontacts.open.com.opencontacts.utils;

import android.support.annotation.NonNull;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import opencontacts.open.com.opencontacts.BuildConfig;

public class NetworkUtils {

    private static OkHttpClient okHttpClient;

    @NonNull
    public static OkHttpClient getHttpClientWithBasicAuth(String username, String password) {
        boolean isDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor().setLevel(isDebugBuild ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        return okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logger)
                .authenticator((route, response) -> {
                    String basicAuthentication = Credentials.basic(username, password);
                    return response.request()
                            .newBuilder()
                            .addHeader("Authorization", basicAuthentication)
                            .build();
                }).build();
    }

    public static OkHttpClient getHttpClientWithBasicAuth() {
        if(okHttpClient == null) throw new RuntimeException("Initialize okhttp client before requesting for one with creds.");
        return okHttpClient;
    }

}
