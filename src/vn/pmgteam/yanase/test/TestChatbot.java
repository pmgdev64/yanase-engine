package vn.pmgteam.yanase.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import vn.pmgteam.yanase.ai.AISystem;

public class TestChatbot extends AISystem {

    public TestChatbot() {
        super();
        // Bạn có thể thiết lập URL cho Groq để dùng free cho bản 0.0.1
        this.apiUrl = "https://api.groq.com/openai/v1/chat/completions";
    }

    @Override
    protected String parseAiResponse(String jsonRaw) {
        try {
            // IN RA ĐỂ KIỂM TRA (Debug Console)
            System.out.println("DEBUG RAW JSON: " + jsonRaw);

            com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
            com.google.gson.JsonObject json = parser.parse(jsonRaw).getAsJsonObject();
            
            // Kiểm tra xem có phải Server báo lỗi không
            if (json.has("error")) {
                return "[Server Error] " + json.getAsJsonObject("error").get("message").getAsString();
            }

            // Kiểm tra an toàn trước khi lấy dữ liệu
            if (json.has("choices")) {
                return json.getAsJsonArray("choices")
                           .get(0).getAsJsonObject()
                           .getAsJsonObject("message")
                           .get("content").getAsString();
            }

            return "Server trả về cấu trúc lạ: " + jsonRaw;
        } catch (Exception e) {
            return "[Yanase AI Error] Parsing failed: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        TestChatbot bot = new TestChatbot();
        
        // Giả lập việc chat từ Console của Yanase Studio
        System.out.println("Connecting to Global AI Server...");
        
        bot.askGlobalAI("Hello! I am building Yanase Engine on a 4GB RAM PC. Any advice?")
           .thenAccept(response -> {
               System.out.println("\n--- AI Response ---");
               System.out.println(response);
               System.out.println("-------------------\n");
           })
           .join(); // Đợi kết quả (chỉ dùng trong hàm main để test)
    }
}