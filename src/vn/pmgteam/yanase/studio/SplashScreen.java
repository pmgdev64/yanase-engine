package vn.pmgteam.yanase.studio;

import org.lwjgl.glfw.*;
import org.lwjgl.nanovg.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;

public class SplashScreen {
    private long window;
    private long nvg;
    private int splashImage = -1;
    private static final int W = 564, H = 314;
    private static final double DURATION = 3.5;

    private final NVGColor colorBuf = NVGColor.create();
    private final NVGPaint paintBuf = NVGPaint.create();

    public void run() {
        if (!glfwInit()) return;

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_FALSE); // Không viền
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(W, H, "Yanase Studio", NULL, NULL);
        if (window == NULL) return;

        // Căn giữa màn hình
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null)
            glfwSetWindowPos(window, (vid.width() - W) / 2, (vid.height() - H) / 2);

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        nvg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        
        // Load tài nguyên: Font và Ảnh nền
        setupResources();
        
        glfwShowWindow(window);

        String[] steps = {"Initializing...", "Loading...", "Preparing UI...", "Starting..."};
        double startTime = glfwGetTime();

        while (!glfwWindowShouldClose(window)) {
            double elapsed = glfwGetTime() - startTime;
            if (elapsed >= DURATION) break;

            float progress = Math.min((float)(elapsed / DURATION), 1.0f);
            int stepIdx = Math.min((int)(elapsed / (DURATION / steps.length)), steps.length - 1);

            render(progress, steps[stepIdx]);
            
            glfwPollEvents();
            glfwSwapBuffers(window);
        }

        // Dọn dẹp
        if (splashImage != -1) nvgDeleteImage(nvg, splashImage);
        nvgDelete(nvg);
        glfwDestroyWindow(window);
    }

    private void setupResources() {
        // Load font cho stepText
        String fp = "C:/Windows/Fonts/segoeui.ttf";
        if (!Files.exists(Paths.get(fp))) fp = "C:/Windows/Fonts/arial.ttf";
        nvgCreateFont(nvg, "sans", fp);

        // Load ảnh nền splash (Đảm bảo file nằm đúng trong assets/)
        try {
            ByteBuffer imgBuffer = loadResourceToBuffer("/assets/yanase-splash.jpg");
            // nvgCreateImageMem nạp data vào GPU
            splashImage = nvgCreateImageMem(nvg, 0, imgBuffer);
            
            // Sau khi nvgCreateImageMem xong, data đã nằm trên GPU 
            // nên ta có thể giải phóng bộ nhớ RAM ngay lập tức
            MemoryUtil.memFree(imgBuffer);
            
            if (splashImage == 0) System.err.println("Lỗi tạo texture từ memory!");
        } catch (IOException e) {
            System.err.println("Không thể đọc ảnh splash từ JAR: " + e.getMessage());
        }
    }
    
    private ByteBuffer loadResourceToBuffer(String resourcePath) throws IOException {
        try (java.io.InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Không tìm thấy tài nguyên: " + resourcePath);
            
            byte[] bytes = is.readAllBytes();
            // Sử dụng memAlloc theo đúng thư viện LWJGL bạn đang dùng
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            return buffer;
        }
    }

    public void render(float progress, String stepText) {
        glClear(GL_COLOR_BUFFER_BIT);
        nvgBeginFrame(nvg, W, H, 1.0f);

        // --- 1. Vẽ Ảnh Nền (Background) ---
        if (splashImage != -1) {
            NVGPaint imgPaint = nvgImagePattern(nvg, 0, 0, W, H, 0, splashImage, 1.0f, paintBuf);
            nvgBeginPath(nvg);
            nvgRect(nvg, 0, 0, W, H);
            nvgFillPaint(nvg, imgPaint);
            nvgFill(nvg);
        } else {
            // Nền đen dự phòng
            nvgBeginPath(nvg);
            nvgRect(nvg, 0, 0, W, H);
            nvgFillColor(nvg, rgba(30, 30, 30, 255));
            nvgFill(nvg);
        }

        // --- 2. Progress Bar (Sát đáy - Eclipse Style) ---
        float barH = 3.0f; // Thanh mảnh
        float barY = H - barH;

        // Track (Nền thanh bar - hơi trong suốt)
        nvgBeginPath(nvg);
        nvgRect(nvg, 0, barY, W, barH);
        nvgFillColor(nvg, rgba(0, 0, 0, 100));
        nvgFill(nvg);

        // Fill (Thanh tiến trình chạy)
        if (progress > 0) {
            nvgBeginPath(nvg);
            nvgRect(nvg, 0, barY, W * progress, barH);
            
            // Màu xanh đặc trưng của Eclipse (Gradient)
            nvgLinearGradient(nvg, 0, barY, W * progress, barY,
                rgba(60, 120, 220, 255), rgba(100, 180, 240, 255), paintBuf);
                
            nvgFillPaint(nvg, paintBuf);
            nvgFill(nvg);
        }

        // --- 3. Step Text (Nhỏ, nằm trên bar một chút) ---
        nvgFontSize(nvg, 10.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM);
        nvgFillColor(nvg, rgba(200, 200, 200, 180));
        nvgText(nvg, W - 10, barY - 4, stepText.toUpperCase());

        // --- 4. Viền mờ bao quanh cửa sổ ---
        nvgBeginPath(nvg);
        nvgRect(nvg, 0.5f, 0.5f, W - 1, H - 1);
        nvgStrokeColor(nvg, rgba(255, 255, 255, 40));
        nvgStrokeWidth(nvg, 1.0f);
        nvgStroke(nvg);

        nvgEndFrame(nvg);
    }

    private NVGColor rgba(int r, int g, int b, int a) {
        colorBuf.r(r / 255f); colorBuf.g(g / 255f);
        colorBuf.b(b / 255f); colorBuf.a(a / 255f);
        return colorBuf;
    }
}