package vn.pngteam.yanase.base;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import com.sun.management.OperatingSystemMXBean;

import vn.pngteam.yanase.base.render.RenderingHints;
import vn.pngteam.yanase.gui.FontRenderer;
import vn.pngteam.yanase.node.CameraNode;
import vn.pngteam.yanase.node.Object3D;
import vn.pngteam.yanase.scene.BaseScene;
import vn.pngteam.yanase.settings.GameSettings;
import vn.pngteam.yanase.util.TextureManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Color;
import java.lang.management.ManagementFactory;

public abstract class Engine implements Runnable {

    protected long windowHandle = NULL;
    protected static Engine engine;
    protected static GameSettings gameSettings;
    
    private FontRenderer splashFont;
    private FontRenderer debugFont;
    private FontRenderer.TextTextureData splashData = null;

    protected boolean isSplashFinished = false;
    private double splashStartTime;
    private static final double SPLASH_DURATION = 6.0;
    
    private double lastFrameTime;
    private float deltaTime;
    
    private int fps;
    private int currentFrames;
    private double lastFpsTime;
    private boolean showDebug = true;
    
    protected int width, height;
    private CameraNode mainCamera;
    
    private BaseScene activeScene;
    
    private int splashTextureId = -1; // Lưu trữ ID của file jpg
    
    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    public Object3D sceneRoot = new Object3D("Scene Root") {
        @Override public void render() {}
        @Override public void setNodeValue(String v) throws DOMException {}
        @Override public Node getPreviousSibling() { return null; }
        @Override public Node insertBefore(Node n, Node r) throws DOMException { return null; }
        @Override public Node replaceChild(Node n, Node o) throws DOMException { return null; }
        @Override public Node removeChild(Node o) throws DOMException { return null; }
        @Override public Node cloneNode(boolean d) { return null; }
        @Override public void normalize() {}
        @Override public boolean isSupported(String f, String v) { return false; }
        @Override public String getNamespaceURI() { return null; }
        @Override public String getPrefix() { return null; }
        @Override public void setPrefix(String p) throws DOMException {}
        @Override public String getLocalName() { return null; }
        @Override public boolean hasAttributes() { return false; }
        @Override public String getBaseURI() { return null; }
        @Override public short compareDocumentPosition(Node o) throws DOMException { return 0; }
        @Override public String getTextContent() throws DOMException { return null; }
        @Override public void setTextContent(String t) throws DOMException {}
        @Override public boolean isSameNode(Node o) { return false; }
        @Override public String lookupPrefix(String n) { return null; }
        @Override public boolean isDefaultNamespace(String n) { return false; }
        @Override public String lookupNamespaceURI(String p) { return null; }
        @Override public boolean isEqualNode(Node a) { return false; }
        @Override public Object getFeature(String f, String v) { return null; }
        @Override public Object setUserData(String k, Object d, UserDataHandler h) { return null; }
        @Override public Object getUserData(String k) { return null; }
    };

    public Engine() {
        engine = this;
        gameSettings = new GameSettings(this);
    }
    
    public long getWindowHandle() {
    	return windowHandle;
    }
    
    public static Engine getEngine() { return engine; }
    public static GameSettings getGameSettings() { return gameSettings; }
    public float getDeltaTime() { return deltaTime; }
    public boolean isSplashFinished() { return isSplashFinished; }
    public int getWindowWidth() { return width; }
    public int getWindowHeight() { return height; }
    
 // Thêm vào phần khai báo biến của class Engine

    public void setMainScene(vn.pngteam.yanase.scene.BaseScene scene) {
        this.activeScene = scene;
        if (scene != null) {
            // Tự động liên kết rootNode và Camera từ Scene vào Engine
            this.sceneRoot = (Object3D) scene.getRootNode();
            // Giả sử WorldScene có phương thức getPlayer() trả về CameraNode
            if (scene instanceof vn.pngteam.yanase.test.WorldScene) {
                this.mainCamera = ((vn.pngteam.yanase.test.WorldScene) scene).getPlayer();
            }
        }
    }

    public void startApplication() { this.run(); }

    @Override
    public void run() {
        try {
            init();
            onInit(); 
            loop();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    private void init() {
        int frameWidth = 41; 
        System.out.println("=========================================");
        System.out.println("|" + centerString("YANASE ENGINE", frameWidth - 2) + "|");
        System.out.println("|" + centerString("Version: " + gameSettings.getEngineVersion(), frameWidth - 2) + "|");
        System.out.println("=========================================");
        
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");

        gameSettings.applyToEngine();
        windowHandle = glfwCreateWindow(gameSettings.getWindowWidth(), gameSettings.getWindowHeight(), gameSettings.getProjectTitle(), NULL, NULL);
        if (windowHandle == NULL) throw new RuntimeException("Window creation failed");
        
        glfwWindowHint(GLFW_SAMPLES, RenderingHints.AA_MSAA_8X);

        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities(); 
     // Ngay sau khi GL.createCapabilities()
        glEnable(GL_MULTISAMPLE);

        mainCamera = new CameraNode("MainCamera");
        mainCamera.position.set(0, 0, 5);
        sceneRoot.appendChild(mainCamera);

        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        this.width = w[0]; this.height = h[0];

        glfwSetFramebufferSizeCallback(windowHandle, (window, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
            glViewport(0, 0, newWidth, newHeight);
            gameSettings.setWindowWidth(newWidth);
            gameSettings.setWindowHeight(newHeight);
        });

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glfwSwapInterval(gameSettings.getVsyncInt()); 
        
        System.out.println("| GPU: " + truncate(glGetString(GL_RENDERER), frameWidth - 8) + "        |");
        System.out.println("=========================================");
        
        splashStartTime = glfwGetTime();
        lastFpsTime = splashStartTime;
        lastFrameTime = glfwGetTime();

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F3 && action == GLFW_PRESS) showDebug = !showDebug;
        });
    }
    
 // Thêm vào class Engine
    public void initForEditor(long swtHandle, int width, int height) {
        this.width = width;
        this.height = height;
        this.windowHandle = swtHandle; // Gán handle từ Eclipse Canvas

        // KHÔNG gọi glfwCreateWindow
        // Chúng ta cần một thư viện cầu nối như LWJGL-SWT để bind context
        // Hoặc sử dụng kỹ thuật "Off-screen rendering" nếu không có thư viện cầu nối
        
        GL.createCapabilities(); 
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, width, height);
        
        // Bỏ qua splash để test nhanh
        this.isSplashFinished = true;
        
        mainCamera = new CameraNode("EditorCamera");
        sceneRoot.appendChild(mainCamera);
        
        onInit();
    }
    
    private void loop() {
        while (!glfwWindowShouldClose(windowHandle)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            double currentTime = glfwGetTime();
            deltaTime = (float) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            currentFrames++;
            if (currentTime - lastFpsTime >= 1.0) {
                fps = currentFrames;
                currentFrames = 0;
                lastFpsTime = currentTime;
            }

            if (!isSplashFinished) {
                if (currentTime - splashStartTime < SPLASH_DURATION) {
                    renderSplashScreen();
                } else {
                    isSplashFinished = true;
                    onSplashFinished();
                }
            } else {
                renderSky(); 

                mainCamera.applyProjection();
                mainCamera.applyViewMatrix(); 
                
                sceneRoot.updateTransform(new Matrix4f());
                
                onLoop();
                renderRecursive(sceneRoot);

                if (showDebug) renderDebugInfo();
                renderCrosshair();
            }
            
            // Lấy dữ liệu RAM thực tế từ osBean (com.sun.management.OperatingSystemMXBean)
            long totalPhys = osBean.getTotalPhysicalMemorySize();
            long freePhys = osBean.getFreePhysicalMemorySize();
            double ramUsagePercent = ((double) (totalPhys - freePhys) / totalPhys) * 100.0;

            // Ngưỡng 97% - Ranh giới cuối cùng giữa sự sống và BSOD
            if (ramUsagePercent >= 97.0) {
                System.err.println("[EMERGENCY] RAM USAGE: " + String.format("%.1f%%", ramUsagePercent));
                
                // 1. Tạo "di chúc" JVM Style trước khi bay màu
                // Sử dụng mã lỗi ERR_EMERGENCY_EXIT mà bạn đã định nghĩa
                vn.pngteam.yanase.util.CrashReport.make(
                    vn.pngteam.yanase.util.CrashReport.ERR_EMERGENCY_EXIT, 
                    ramUsagePercent
                );

                // 2. Thông báo khẩn cấp ra Console để Lead PmgTeam theo dõi
                System.err.println("[EMERGENCY] Closing Engine to prevent System Crash/BSOD...");
                
                // 3. Kích hoạt đóng cửa sổ êm đẹp (Graceful Shutdown)
                // Giúp giải phóng các Buffer trên GPU UHD 610 ngay lập tức
                glfwSetWindowShouldClose(windowHandle, true);
            }
            
            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
            handleAutoSave();
        }
    }

    private void renderDebugInfo() {
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        int ww = gameSettings.getWindowWidth();
        int wh = gameSettings.getWindowHeight();
        glOrtho(0, ww, wh, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();

        if (debugFont == null) debugFont = new FontRenderer("JetBrains Mono", 14);

        // 1. Heap Memory (Bộ nhớ Java nội bộ)
        long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long usedHeap = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

        // 2. Physical RAM (Bộ nhớ thực tế hệ thống)
        long totalPhys = osBean.getTotalPhysicalMemorySize() / 1024 / 1024;
        long freePhys = osBean.getFreePhysicalMemorySize() / 1024 / 1024;
        long usedPhys = totalPhys - freePhys;
        
        // Tính toán % Usage
        double ramUsagePercent = ((double) usedPhys / totalPhys) * 100.0;

        // Ngưỡng cảnh báo: Khi lên 32GB, dùng > 85% mới thực sự cần hiện Đỏ
        boolean isRamDanger = ramUsagePercent >= 85.0; 
        // Thêm màu Xanh cho RAM nếu dưới 40% (Cảm giác cực kỳ an toàn trên 32GB)
        boolean isRamHealthy = ramUsagePercent < 40.0;

        String osName = osBean.getName();
        String osArch = osBean.getArch();
        double processCpuLoad = osBean.getProcessCpuLoad() * 100;
        if (processCpuLoad < 0) processCpuLoad = 0;

        String[] lines = {
            "Yanase Engine " + gameSettings.getEngineVersion(),
            "OS          : " + osName + " (" + osArch + ")",
            "--------------------------------------",
            String.format("Performance : %d fps (%.1fms)", fps, deltaTime * 1000),
            String.format("CPU Usage   : %.1f%%", processCpuLoad),
            "GPU Renderer: " + glGetString(GL_RENDERER),
            "Vertices    : " + vn.pngteam.yanase.mesh.Mesh.renderVerticesCount,
            "Triangles   : " + vn.pngteam.yanase.mesh.Mesh.renderTrianglesCount,
            "JVM Heap    : " + usedHeap + "/" + maxHeap + " MB",
            String.format("System RAM  : %.1f%% %s", ramUsagePercent, isRamDanger ? "[DANGER!]" : ""),
            "Resolution  : " + ww + "x" + wh
        };

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        for (int i = 0; i < lines.length; i++) {
            // Logic đổi màu cho dòng System RAM (dòng index 9)
            if (i == 9) {
                if (isRamDanger) {
                    glColor4f(1.0f, 0.2f, 0.2f, 1.0f); // Màu đỏ nếu sắp sập
                } else if (isRamHealthy) {
                    glColor4f(0.2f, 1.0f, 0.2f, 1.0f); // Màu xanh lá nếu RAM dư dả (đặc quyền 32GB)
                } else {
                    glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Trắng trung tính
                }
            } else {
                glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Mặc định trắng
            }
            
            drawDebugLine(lines[i], 10, 10 + (i * 20));
        }
        
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_BLEND);
        glMatrixMode(GL_PROJECTION); glPopMatrix();
        glMatrixMode(GL_MODELVIEW); glPopMatrix();
    }
    
    private void drawDebugLine(String text, float x, float y) {
        FontRenderer.TextTextureData data = debugFont.getStringTextureData(text, Color.WHITE);
        debugFont.drawCachedTexture(data.id, x, y, data.width, data.height);
        glDeleteTextures(data.id);
    }

    protected void renderRecursive(Node node) {
        // 1. Vẽ chính nó nếu là Object3D
        if (node instanceof Object3D) {
            ((Object3D) node).render();
        }

        // 2. Lấy danh sách con
        // Lưu ý: Nếu node là BaseNode, bạn có thể truy cập trực tiếp list 'children' 
        // để tránh overhead của NodeList
        org.w3c.dom.NodeList childrenList = node.getChildNodes();
        int count = childrenList.getLength();
        
        for (int i = 0; i < count; i++) {
            renderRecursive(childrenList.item(i));
        }
    }


    private void renderSplashScreen() {
        double elapsedTime = glfwGetTime() - splashStartTime;
        float alpha = 1.0f;

        // Logic Fade Out sau 3 giây
        if (elapsedTime <= 3.0) {
            alpha = 1.0f;
        } else if (elapsedTime <= 4.0) {
            alpha = (float)(1.0 - (elapsedTime - 3.0));
        } else {
            alpha = 0.0f;
        }

        // 1. Nạp tài nguyên nếu chưa có
        if (splashTextureId == -1) {
            splashTextureId = TextureManager.loadTexture("/assets/yanase-splash.jpg");
        }
        if (splashFont == null) {
            // Sử dụng font hệ thống thanh mảnh, size nhỏ vừa phải (khoảng 18-20)
            splashFont = new FontRenderer("Segoe UI", 18);
        }

        int ww = gameSettings.getWindowWidth();   
        int wh = gameSettings.getWindowHeight();  

        glViewport(0, 0, ww, wh); 
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glMatrixMode(GL_PROJECTION); glLoadIdentity();
        glOrtho(0, (double)ww, (double)wh, 0, -1, 1); 
        glMatrixMode(GL_MODELVIEW); glLoadIdentity();

        // 2. Vẽ Image Splash (Giữ nguyên logic Fit của bạn)
        float imgW = 1280.0f; 
        float imgH = 720.0f;
        float imgAspect = imgW / imgH;
        float winAspect = (float)ww / (float)wh;
        float drawW, drawH;

        if (winAspect > imgAspect) {
            drawH = (float)wh; 
            drawW = drawH * imgAspect;
        } else {
            drawW = (float)ww; 
            drawH = drawW / imgAspect;
        }

        float x = ((float)ww - drawW) / 2.0f;
        float y = ((float)wh - drawH) / 2.0f;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, splashTextureId);
        glColor4f(1.0f, 1.0f, 1.0f, alpha); 

        glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(x, y);
            glTexCoord2f(1, 0); glVertex2f(x + drawW, y);
            glTexCoord2f(1, 1); glVertex2f(x + drawW, y + drawH);
            glTexCoord2f(0, 1); glVertex2f(x, y + drawH);
        glEnd();

        // 3. Vẽ Text "Created with Yanase Engine"
        // Lấy dữ liệu texture của text (đảm bảo hàm này có cache để tránh tràn VRAM)
        var textData = splashFont.getStringTextureData("Created with Yanase Engine", java.awt.Color.BLACK);

        // Vị trí: Góc dưới bên phải, cách lề 30px
        float textX = (float)ww - textData.width - 30;
        float textY = (float)wh - textData.height - 30;

        // Quan trọng: Phải Bind lại texture của Font trước khi vẽ text
        // Vì lệnh vẽ Quads ở trên đang Bind splashTextureId
        glColor4f(1.0f, 1.0f, 1.0f, alpha); // Đồng bộ độ mờ với logo
        splashFont.drawCachedTexture(textData.id, textX, textY, textData.width, textData.height);

        // 4. Reset trạng thái
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }
    
    protected void renderCrosshair() {
        glPushMatrix(); glLoadIdentity();
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        glOrtho(0, this.width, this.height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        
        glDisable(GL_DEPTH_TEST);
        float centerX = this.width / 2.0f, centerY = this.height / 2.0f, size = 10.0f;
        glColor4f(1.0f, 1.0f, 1.0f, 0.8f); glLineWidth(2.0f);
        glBegin(GL_LINES);
            glVertex2f(centerX - size, centerY); glVertex2f(centerX + size, centerY);
            glVertex2f(centerX, centerY - size); glVertex2f(centerX, centerY + size);
        glEnd();
        glEnable(GL_DEPTH_TEST);
        
        glMatrixMode(GL_PROJECTION); glPopMatrix();
        glMatrixMode(GL_MODELVIEW); glPopMatrix();
    }
    
    protected void renderSky() {
        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        glOrtho(0, 1, 0, 1, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();

        float offset = Math.max(-1.0f, Math.min(1.0f, mainCamera.rotation.x / 90.0f)); 

        glBegin(GL_QUADS);
            float b = 0.5f - (offset * 0.3f);
            glColor3f(b, b + 0.05f, b + 0.1f); 
            glVertex2f(0, 0); glVertex2f(1, 0);
            
            float t = 0.3f - (offset * 0.2f);
            glColor3f(t, t + 0.2f, t + 0.5f); 
            glVertex2f(1, 1); glVertex2f(0, 1);
        glEnd();

        glMatrixMode(GL_PROJECTION); glPopMatrix();
        glMatrixMode(GL_MODELVIEW); glPopMatrix();
        glEnable(GL_DEPTH_TEST);
        glColor4f(1, 1, 1, 1);
    }
    
    private String centerString(String s, int width) {
        int pad = width - s.length();
        return " ".repeat(Math.max(0, pad/2)) + s + " ".repeat(Math.max(0, pad - pad/2));
    }
    
    private String truncate(String s, int width) { 
        return s.length() > width ? s.substring(0, width - 3) + "..." : s; 
    }
    
    public abstract void onInit();
    public abstract void onLoop();
    
    protected void onSplashFinished() { 
        if (splashData != null) glDeleteTextures(splashData.id);
        System.out.println("| Status: Splash Finished. Game Start.  |");
        System.out.println("=========================================");
    }
    
    private void handleAutoSave() {}
    private void cleanup() { onCleanup(); if (windowHandle != NULL) glfwDestroyWindow(windowHandle); glfwTerminate(); }
    public abstract void onCleanup();
}