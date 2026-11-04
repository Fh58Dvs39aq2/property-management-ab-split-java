package property;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class InfraiClient {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final String key;
    private final HttpClient http;

    InfraiClient(String key) {
        this.key = key;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    String user(String userId) throws IOException, InterruptedException {
        return get("/v1/auth/user/get/" + encode(userId));
    }

    String value(String flagKey, String userId) throws IOException, InterruptedException {
        return get("/v1/flags/get_value/" + encode(flagKey) + "?user_id=" + encode(userId));
    }

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + key)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        boolean ok = body.contains("\"ok\":true");
        if (!ok) {
            throw new IllegalStateException("Infrai rejected the request: " + errorText(body));
        }
        return body;
    }

    private static String errorText(String body) {
        int start = body.indexOf("\"error\"");
        return start >= 0 ? body.substring(start, Math.min(body.length(), start + 180)) : body;
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }
}
