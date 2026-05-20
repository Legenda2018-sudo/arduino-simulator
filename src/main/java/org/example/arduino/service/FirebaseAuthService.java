package org.example.arduino.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;

/** Авторизация Firebase (email/пароль) через REST API. */
public class FirebaseAuthService {

    private static final String FIREBASE_AUTH_BASE =
        "https://identitytoolkit.googleapis.com/v1";

    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;

    public static class AuthResult {
        private final String userId;   // localId в Firebase
        private final String idToken;  // токен для доступа к БД
        private final String email;

        public AuthResult(String userId, String idToken, String email) {
            this.userId = userId;
            this.idToken = idToken;
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public String getIdToken() {
            return idToken;
        }

        public String getEmail() {
            return email;
        }
    }

    public FirebaseAuthService() {
        this.client = FirebaseHttp.newClient();
        this.gson = new Gson();

        String key = "YOUR_FIREBASE_WEB_API_KEY";
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                key = props.getProperty("firebase.apiKey", key);
            }
        } catch (IOException ignored) {
        }

        this.apiKey = key;
    }

    public boolean isConfigured() {
        return apiKey != null
            && !apiKey.isBlank()
            && !"YOUR_FIREBASE_WEB_API_KEY".equals(apiKey);
    }

    public AuthResult register(String email, String password) throws IOException {
        return authenticate(email, password, true);
    }

    public AuthResult login(String email, String password) throws IOException {
        return authenticate(email, password, false);
    }

    private AuthResult authenticate(String email, String password, boolean register) throws IOException {
        if (!isConfigured()) {
            throw new IOException(
                "Firebase не настроен. Укажите firebase.apiKey в config.properties (ключ Web API из Firebase Console).");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
        }

        String method = register ? "accounts:signUp" : "accounts:signInWithPassword";
        String url = FIREBASE_AUTH_BASE + "/" + method + "?key=" + apiKey;

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("email", email);
        bodyJson.addProperty("password", password);
        bodyJson.addProperty("returnSecureToken", true);

        RequestBody body = RequestBody.create(
            bodyJson.toString(),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();

        try {
            return executeAuthRequest(request);
        } catch (IOException e) {
            String networkMessage = describeNetworkFailure(e);
            if (networkMessage != null) {
                throw new IOException(networkMessage, e);
            }
            throw e;
        }
    }

    private static String describeNetworkFailure(IOException e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        if (root instanceof UnknownHostException) {
            return "Не удалось найти сервер Firebase. Проверьте интернет и DNS (identitytoolkit.googleapis.com).";
        }
        if (root instanceof SocketTimeoutException) {
            return "Сервер Firebase не ответил вовремя. Интернет может быть медленным или доступ к Google заблокирован.";
        }
        if (root instanceof ConnectException) {
            return "Не удалось подключиться к Firebase. Проверьте интернет, VPN, прокси или блокировку Google-сервисов.";
        }
        if (root instanceof SSLException) {
            return "Ошибка защищённого соединения с Firebase. Проверьте дату и время на компьютере.";
        }

        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("Unknown host") || msg.contains("хост неизвестен") || msg.contains("Unable to resolve host")) {
            return "Не удалось найти сервер Firebase. Проверьте интернет и DNS.";
        }
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("Таймаут")) {
            return "Сервер Firebase не ответил вовремя. Повторите попытку позже.";
        }
        return null;
    }

    private AuthResult executeAuthRequest(Request request) throws IOException {
        try (Response response = client.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // Пробуем аккуратно распарсить JSON-ошибку Firebase и выдать короткое сообщение
                String friendly = "Не удалось выполнить запрос к Firebase.";
                try {
                    JsonObject root = gson.fromJson(respBody, JsonObject.class);
                    if (root != null && root.has("error")) {
                        JsonObject err = root.getAsJsonObject("error");
                        String msg = err.has("message") ? err.get("message").getAsString() : "";

                        // Наиболее частые коды/сообщения Firebase
                        if (msg.contains("API key not valid") || msg.contains("API_KEY_INVALID")) {
                            friendly = "Неверный ключ API Firebase. Проверьте значение firebase.apiKey в config.properties.";
                        } else if (msg.contains("EMAIL_NOT_FOUND")) {
                            friendly = "Аккаунт с таким email не найден.";
                        } else if (msg.contains("INVALID_PASSWORD")) {
                            friendly = "Неверный пароль.";
                        } else if (msg.contains("USER_DISABLED")) {
                            friendly = "Аккаунт отключён.";
                        } else if (msg.contains("EMAIL_EXISTS")) {
                            friendly = "Такой email уже зарегистрирован.";
                        } else if (msg.contains("INVALID_EMAIL")) {
                            friendly = "Неверно введен email.";
                        } else if (msg.contains("INVALID_LOGIN_CREDENTIALS")) {
                            friendly = "Логин или пароль указан не верно.";
                        } else if (msg.contains("CONFIGURATION_NOT_FOUND")) {
                            friendly = "В Firebase Console не настроена аутентификация. Включите метод «Email/пароль»: Authentication → Sign-in method → Email/Password → Включить.";
                        } else {
                            // Если получили осмысленное текстовое сообщение от Firebase — показываем его
                            if (!msg.isBlank()) {
                                friendly = msg;
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Если парсинг не удался, оставляем общее сообщение
                }

                throw new IOException(friendly);
            }

            JsonObject obj = gson.fromJson(respBody, JsonObject.class);
            if (obj == null
                || !obj.has("localId")
                || !obj.has("idToken")
                || !obj.has("email")) {
                throw new IOException("Неожиданный ответ Firebase Auth");
            }

            String userId = obj.get("localId").getAsString();
            String idToken = obj.get("idToken").getAsString();
            String respEmail = obj.get("email").getAsString();

            return new AuthResult(userId, idToken, respEmail);
        }
    }
}
