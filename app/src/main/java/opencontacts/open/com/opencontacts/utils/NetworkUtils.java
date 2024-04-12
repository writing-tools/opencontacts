package opencontacts.open.com.opencontacts.utils;

import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.debugLogsEnabledForSync;

import android.content.Context;

import androidx.annotation.NonNull;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DefaultRequestCacheKeyProvider;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

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
    public static OkHttpClient createHttpClientWithAuth(String username, String password, boolean shouldIgnoreSSL, Context context) {
        HttpLoggingInterceptor logger = new HttpLoggingInterceptor();
        logger.level(debugLogsEnabledForSync(context) ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient.Builder okHTTPClientBuilder = shouldIgnoreSSL ? getUnsafeOkHttpClientBuilder() : new OkHttpClient.Builder();
        final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();

        final Credentials credentials = new Credentials(username, password);
        final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
        final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);

        // note that all auth schemes should be registered as lowercase!
        DispatchingAuthenticator authenticator = new DispatchingAuthenticator.Builder()
            .with("digest", digestAuthenticator)
            .with("basic", basicAuthenticator)
            .build();

        return okHttpClient = okHTTPClientBuilder
            .addInterceptor(logger)
            .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
            .addInterceptor(new AuthenticationCacheInterceptor(authCache, new DefaultRequestCacheKeyProvider()))
            .build();
    }

    public static OkHttpClient getHttpClientWithAuth() {
        if (okHttpClient == null)
            throw new RuntimeException("Initialize okhttp client before requesting for one with creds.");
        return okHttpClient;
    }

}
