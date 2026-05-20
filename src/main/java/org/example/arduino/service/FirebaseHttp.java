package org.example.arduino.service;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/** Общие настройки HTTP-клиента для Firebase. */
final class FirebaseHttp {

    private FirebaseHttp() {
    }

    static OkHttpClient newClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build();
    }
}
