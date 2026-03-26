package vn.pmgteam.yanase.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public abstract class AISystem {
    
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10)) // Chống treo máy khi mạng yếu
            .build();

    // Để mặc định là trống, sẽ nạp qua setter hoặc file config
    protected String apiKey = "0"; 
    protected String apiUrl = "https://api.openai.com/v1/chat/completions";
    protected String model = "gpt-3.5-turbo";

    public void setApiKey(String key) {
        this.apiKey = key.trim();
    }

    public CompletableFuture<String> askGlobalAI(String prompt) {
        // Kiểm tra an toàn trước khi gửi
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("ko có key")) {
            return CompletableFuture.completedFuture("[System] API Key chưa được thiết lập!");
        }

        // Thoát các ký tự đặc biệt trong prompt để tránh lỗi JSON (Rất quan trọng!)
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

        String jsonBody = """
            {
                "model": "%s",
                "messages": [{"role": "user", "content": "%s"}]
            }
            """.formatted(model, safePrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    // Kiểm tra mã lỗi HTTP trước khi parse JSON
                    if (response.statusCode() != 200) {
                        return "[Error " + response.statusCode() + "] " + response.body();
                    }
                    return parseAiResponse(response.body());
                })
                .exceptionally(ex -> "[Network Error] " + ex.getMessage());
    }

    protected abstract String parseAiResponse(String jsonRaw);
}