package org.example.arduino.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.example.arduino.model.Circuit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class FirebaseService {
    private final OkHttpClient client;
    private final Gson gson;
    private final String databaseUrl;

    public FirebaseService() {
        this.client = FirebaseHttp.newClient();
        this.gson = new Gson();

        String url = "https://your-project-id-default-rtdb.firebaseio.com";

        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                url = props.getProperty("firebase.url", url);
            }
        } catch (IOException ignored) {}

        this.databaseUrl = url;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    private String buildUserPath(String userId, String path, String idToken) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(databaseUrl);
        if (!databaseUrl.endsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append("users/").append(userId).append("/");
        urlBuilder.append(path);
        if (idToken != null && !idToken.isEmpty()) {
            urlBuilder.append("?auth=").append(idToken);
        }
        return urlBuilder.toString();
    }

    /** Сохраняет схему в том же JSON-формате, что и локальный файл (name, components[], wires[]). */
    public void saveCircuit(JsonObject circuitJson, String userId, String idToken) throws IOException {
        if (circuitJson == null || !circuitJson.has("name") || circuitJson.get("name").isJsonNull()) {
            throw new IllegalArgumentException("Имя схемы не может быть пустым");
        }
        String name = circuitJson.get("name").getAsString();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Имя схемы не может быть пустым");
        }
        if (userId == null || userId.isEmpty() || idToken == null || idToken.isEmpty()) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);

        String url = buildUserPath(userId, "circuits/" + encodedName + ".json", idToken);

        RequestBody body = RequestBody.create(
            circuitJson.toString(),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Нет деталей";
                throw new IOException("Ошибка сохранения в Firebase (код " + response.code() + "): " + errorBody);
            }
        }
    }

    public List<Circuit> loadCircuits(String userId, String idToken) throws IOException {
        if (userId == null || userId.isEmpty() || idToken == null || idToken.isEmpty()) {
            throw new IllegalStateException("Пользователь не авторизован");
        }

        String url = buildUserPath(userId, "circuits.json", idToken);

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        List<Circuit> circuits = new ArrayList<>();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return circuits;
            }

            String jsonResponse = response.body().string();
            if (jsonResponse == null || jsonResponse.isEmpty() || "null".equals(jsonResponse.trim())) {
                return circuits;
            }

            JsonElement rootElement = gson.fromJson(jsonResponse, JsonElement.class);
            if (!rootElement.isJsonObject()) {
                return circuits;
            }

            JsonObject root = rootElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject obj = entry.getValue().getAsJsonObject();
                if (obj.has("name") && !obj.get("name").isJsonNull()) {
                    String name = obj.get("name").getAsString();
                    circuits.add(new Circuit(name));
                }
            }
        }

        return circuits;
    }

    /** Удаляет схему из облака по имени. */
    public void deleteCircuit(String circuitName, String userId, String idToken) throws IOException {
        if (circuitName == null || circuitName.isEmpty()) {
            throw new IllegalArgumentException("Имя схемы не может быть пустым");
        }
        if (userId == null || userId.isEmpty() || idToken == null || idToken.isEmpty()) {
            throw new IllegalStateException("Пользователь не авторизован");
        }
        String encodedName = URLEncoder.encode(circuitName, StandardCharsets.UTF_8);
        String url = buildUserPath(userId, "circuits/" + encodedName + ".json", idToken);
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Нет деталей";
                throw new IOException("Ошибка удаления из Firebase (код " + response.code() + "): " + errorBody);
            }
        }
    }
}
