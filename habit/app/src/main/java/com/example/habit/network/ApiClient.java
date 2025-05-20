package com.example.habit.network;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    public static Retrofit getRetrofit(Context ctx) {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            SharedPreferences prefs = ctx.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();
                            Request.Builder builder = originalRequest.newBuilder();

                            String path = originalRequest.url().encodedPath();
                            if (path != null && !path.startsWith(AUTH_PATH_PREFIX)) {
                                String token = prefs.getString("jwt", null);
                                if (token != null && !token.isEmpty()) {
                                    builder.addHeader("Authorization", "Bearer " + token);
                                }
                            }

                            Request newRequest = builder.build();
                            return chain.proceed(newRequest);
                        }
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
