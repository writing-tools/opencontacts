package opencontacts.open.com.opencontacts.utils;

import androidx.annotation.NonNull;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import opencontacts.open.com.opencontacts.BuildConfig;

public class NetworkUtils {

    private static OkHttpClient okHttpClient;

    private static OkHttpClient.Builder getUnsafeOkHttpClientBuilder() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static OkHttpClient getHttpClientWithBasicAuth(String username, String password, boolean shouldIgnoreSSL) {
        boolean isDebugBuild = BuildConfig.BUILD_TYPE.equals("debug");
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.level(isDebugBuild ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient.Builder okHTTPClientBuilder = shouldIgnoreSSL ? getUnsafeOkHttpClientBuilder() : new OkHttpClient.Builder();
        return okHttpClient = okHTTPClientBuilder
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
        if (okHttpClient == null)
            throw new RuntimeException("Initialize okhttp client before requesting for one with creds.");
        return okHttpClient;
    }

}
