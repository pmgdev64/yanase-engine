package vn.pmgteam.yanase.base;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import com.sun.management.OperatingSystemMXBean;

import vn.pmgteam.yanase.base.render.RenderingHints;
import vn.pmgteam.yanase.gui.FontRenderer;
import vn.pmgteam.yanase.gui.IClickable;
import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.node.CameraMode;
import vn.pmgteam.yanase.node.CameraNode;
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.settings.GameSettings;
import vn.pmgteam.yanase.util.Raycaster;
import vn.pmgteam.yanase.util.TextureManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class Engine implements Runnable {

    protected long windowHandle = NULL;
    protected static Engine engine;
    protected static GameSettings gameSettings;
    
    private FontRenderer splashFont;
    private FontRenderer debugFont;
    private FontRenderer.TextTextureData splashData = null;
    
    private FontRenderer fontRenderer;
    
    protected vn.pmgteam.yanase.node.BaseNode sceneGui;

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
    protected CameraNode mainCamera;
    
    private BaseScene activeScene;
    
    private boolean isEmergencyClosing = false;
    
    private int splashTextureId = -1; // Lưu trữ ID của file jpg
    
    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    
    private float currentMouseX, currentMouseY;
    // Trong lớp Engine hoặc EditorScene
    private Object3D hoveredObject = null;
    private Object3D selectedNode = null;
    
 // Một chu kỳ ngày đêm tính bằng giây (ví dụ: 600 giây = 10 phút)
    private float dayCycleLength = 800.0f; 
    private float worldTime = 0.25f; // Bắt đầu ở 0.25 (Bình minh)

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
    public Object3D getSelectedNode() { return selectedNode; }
    public void setSelectedNode(Object3D node) { this.selectedNode = node; }
    
 // Thêm vào phần khai báo biến của class Engine

    public void setMainScene(vn.pmgteam.yanase.scene.BaseScene scene) {
        this.activeScene = scene;
        if (scene != null) {
            this.sceneRoot = (Object3D) scene.getRootNode();
            // Lấy sceneGui để Engine có thể render
            this.sceneGui = (vn.pmgteam.yanase.node.BaseNode) scene.getSceneGui(); 
            
            if (scene instanceof vn.pmgteam.yanase.test.WorldScene) {
                this.mainCamera = ((vn.pmgteam.yanase.test.WorldScene) scene).getPlayer();
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
    
    public CameraNode getMainCamera() {
    	return mainCamera;
    }
    
    public Object3D getSceneRoot() {
    	return sceneRoot;
    }
    
    public FontRenderer getFontRenderer() {
    	return fontRenderer;
    }

    private void init() {
        // --- STAGE 1: SYSTEM PRE-FLIGHT (Thông tin hệ thống & RAM) ---
        int frameWidth = 50;
        long startTime = System.currentTimeMillis();
        
        System.out.println("==================================================");
        System.out.println("[PRE-INIT] Starting Yanase Engine...");
        System.out.println("[PRE-INIT] Operating System: " + System.getProperty("os.name"));
        System.out.println("[PRE-INIT] Java Version: " + System.getProperty("java.version"));
        
        // Log thông số RAM để giám sát ngưỡng 92%
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        System.out.println("[PRE-INIT] Memory: " + totalMem + "MB total, " + maxMem + "MB max");
        System.out.println("==================================================");

        // --- STAGE 2: GLFW & WINDOW CREATION ---
        System.out.println("[WINDOW] Initializing GLFW...");
        if (!glfwInit()) {
            System.err.println("[FATAL] Could not initialize GLFW!");
            throw new IllegalStateException("GLFW init failed");
        }

        System.out.println("[WINDOW] Applying Game Settings...");
        gameSettings.applyToEngine();
        
        System.out.println("[WINDOW] Creating GLFW Window: " + gameSettings.getProjectTitle());
        windowHandle = glfwCreateWindow(
            gameSettings.getWindowWidth(), 
            gameSettings.getWindowHeight(), 
            gameSettings.getProjectTitle(), NULL, NULL
        );

        if (windowHandle == NULL) {
            System.err.println("[FATAL] Failed to create the GLFW window!");
            throw new RuntimeException("Window creation failed");
        }

        // --- STAGE 3: OPENGL CONTEXT & CAPABILITIES ---
        System.out.println("[RENDER] Setting up OpenGL Context...");
        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities(); 
        
        // Sau khi có Capabilities mới lấy được thông tin Card đồ họa
        String gpuName = glGetString(GL_RENDERER);
        String glVersion = glGetString(GL_VERSION);
        System.out.println("[RENDER] GPU Renderer: " + gpuName);
        System.out.println("[RENDER] OpenGL Version: " + glVersion);
        
        // --- STAGE 4: GRAPHICS STATES (Depth, Blend, MSAA) ---
        System.out.println("[RENDER] Configuring Graphics States...");
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_MULTISAMPLE);
        glfwSwapInterval(gameSettings.getVsyncInt()); 
        
        // Cài đặt Icon cửa sổ
        System.out.println("[RESOURCES] Loading Window Icons...");
        TextureManager.setWindowIcons(windowHandle, "/assets/icons/icon_16x16.png", "/assets/icons/icon_32x32.png");

        // --- STAGE 5: SUBSYSTEMS (Font, Camera, GUI) ---
        System.out.println("[CORE] Initializing Subsystems...");
        fontRenderer = new FontRenderer("Jetbrains Mono", 14);
        
        mainCamera = new CameraNode("MainCamera");
        mainCamera.position.set(0, 0, 5);
        sceneRoot.appendChild(mainCamera);
        System.out.println("[CORE] Main Camera linked to SceneRoot.");

        // --- STAGE 6: INPUT & CALLBACKS ---
        System.out.println("[INPUT] Registering Event Callbacks...");
        setupInputCallbacks(); // Hàm gom các listener chuột/phím

        // --- STAGE 7: FINALIZING ---
        int[] w = new int[1], h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        this.width = w[0]; this.height = h[0];

        splashStartTime = glfwGetTime();
        lastFpsTime = splashStartTime;
        lastFrameTime = glfwGetTime();

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("==================================================");
        System.out.println("[INIT] " + centerString("YANASE ENGINE READY", frameWidth - 8));
        System.out.println("[INIT] Boot time: " + loadTime + "ms");
        System.out.println("==================================================");
    }
    
    protected void setupInputCallbacks() {
        glfwSetFramebufferSizeCallback(windowHandle, (window, newWidth, newHeight) -> {
            this.width = newWidth;
            this.height = newHeight;
            glViewport(0, 0, newWidth, newHeight);
            gameSettings.setWindowWidth(newWidth);
            gameSettings.setWindowHeight(newHeight);
        });
        
     // Trong lớp Engine.java
        glfwSetMouseButtonCallback(windowHandle, (win, button, action, mods) -> {
            // 1. Tạo mảng tạm để hứng tọa độ từ phần cứng
            double[] x = new double[1];
            double[] y = new double[1];
            
            // 2. Lấy vị trí chuột chính xác tại thời điểm click
            glfwGetCursorPos(win, x, y);
            
            float mx = (float) x[0];
            float my = (float) y[0]; 

            // 3. Nếu bạn dùng hệ tọa độ Y-up (OpenGL chuẩn), phải đảo Y:
            // my = windowHeight - my;

            // 4. Truyền vào hệ thống xử lý UI
            dispatchClick(getSceneGui(), mx, my, button, action);
        });
        
        // Trong hàm init của Engine
        glfwSetCursorPosCallback(windowHandle, (win, xpos, ypos) -> {
            this.currentMouseX = (float) xpos;
            this.currentMouseY = (float) ypos;
            
            // (Tùy chọn) Bạn có thể gọi hàm checkHover ở đây
        });
        
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                
                // Nếu nhấn phím ESC (ExitKey theo EngineSettings)
                if (gameSettings.isExitKey(key)) {
                    // Thay vì thoát: glfwSetWindowShouldClose(window, true);
                    // Chúng ta bật/tắt menu cài đặt
                    gameSettings.toggleSettingsMenu();
                    
                    // Giải phóng/Khóa chuột khi mở Menu để người dùng thao tác
                    if (gameSettings.isShowSettingsMenu()) {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    } else {
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                    }
                }

                if (gameSettings.isDebugKey(key)) {
                    this.showDebug = !this.showDebug;
                }
            }
        });
    };

	private boolean dispatchClick(BaseNode root, float mx, float my, int button, int action) {
        List<BaseNode> nodes = root.getChildren();
        
        // 1. Duyệt ngược từ cuối danh sách (Node trên cùng nhận click trước)
        for (int i = nodes.size() - 1; i >= 0; i--) {
            BaseNode child = nodes.get(i);
            
            // 2. Đệ quy xuống các node con trước (Ưu tiên phần tử lồng bên trong)
            boolean consumed = dispatchClick(child, mx, my, button, action);
            if (consumed) return true; // Nếu con đã xử lý, dừng ngay lập tức

            // 3. Kiểm tra chính node này
            if (child instanceof IClickable) {
                IClickable clickable = (IClickable) child;
                if (clickable.isMouseOver(mx, my)) {
                    if (action == 1) { // GLFW_PRESS
                        clickable.onMousePressed(mx, my, button);
                    } else if (action == 0) { // GLFW_RELEASE
                        clickable.onMouseReleased(mx, my, button);
                    }
                    return true; // Đã xử lý click, ngắt vòng lặp (Consumed)
                }
            }
        }
        return false; // Không có node nào ở vùng này nhận click
    }
    
    private List<Object3D> collidableObjects = new ArrayList<>();

    // Hàm này sẽ duyệt cây Scene Graph và nhặt ra các Object3D
    private void updateCollidableList(BaseNode node) {
        if (node instanceof Object3D && !(node instanceof CameraNode)) {
            collidableObjects.add((Object3D) node);
        }
     
        // Duyệt tiếp các con của Node này
        for (BaseNode child : node.getChildren()) {
            updateCollidableList(child);
        }
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

    public void updateRaycast() {
        // Chỉ thực hiện khi đang ở chế độ PLAYER (có Crosshair)
        if (mainCamera.getMode() == CameraMode.PLAYER) {
            org.joml.Vector3f origin = mainCamera.getPosition();
            org.joml.Vector3f direction = mainCamera.getForwardVector();
            
            // Raycaster sẽ tự đào sâu vào sceneRoot để tìm object gần nhất
            hoveredObject = Raycaster.findSelectedObject(origin, direction, sceneRoot);
            
            if (hoveredObject != null) {
                // Log ra console để test - Sau này có thể dùng để hiện tên lên UI
                // System.out.println("[Raycast] Hovering: " + hoveredObject.getName());
            }
        }
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
                // --- BƯỚC 1: RENDER 3D (Thế giới game) ---
                glEnable(GL_DEPTH_TEST); // Luôn bật Depth cho 3D
                renderSky(); 
                mainCamera.applyProjection();
                mainCamera.applyViewMatrix(); 
                
                sceneRoot.updateTransform(new Matrix4f());
                onLoop();
                renderRecursive(sceneRoot);
                
                // --- BƯỚC 2: RENDER GUI & OVERLAY (Hệ 2D) ---
                // Tắt Depth Test một lần duy nhất ở đây cho toàn bộ UI
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                // Chuyển sang Ortho (2D)
                glMatrixMode(GL_PROJECTION); 
                glPushMatrix(); 
                glLoadIdentity();
                glOrtho(0, width, height, 0, -1, 1);
                
                glMatrixMode(GL_MODELVIEW); 
                glPushMatrix(); 
                glLoadIdentity();

                // Vẽ các thành phần 2D theo thứ tự từ dưới lên trên
                if (sceneGui != null) {
                    renderRecursive2D(sceneGui);
                }

                // Vẽ Tâm ngắm (Crosshair)
                if (mainCamera.getMode() == CameraMode.PLAYER) {
                    renderCrosshair();
                }

                // Vẽ Debug Info (Bảng thông số góc trái)
                if (showDebug) {
                    renderDebugInfo(); 
                }

                // Vẽ Menu Settings (Phủ đen toàn màn hình)
                if (gameSettings.isShowSettingsMenu()) {
                    renderEngineSettingsOverlay();
                }

                // KẾT THÚC 2D: Khôi phục ma trận về 3D
                glMatrixMode(GL_PROJECTION); 
                glPopMatrix();
                glMatrixMode(GL_MODELVIEW); 
                glPopMatrix();
                
                // Bật lại trạng thái mặc định cho frame tiếp theo
                glEnable(GL_DEPTH_TEST);
                glDisable(GL_BLEND);

                // --- BƯỚC 3: LOGIC HẬU RENDER (Raycast, Collision) ---
                collidableObjects.clear();
                updateCollidableList(sceneRoot);

                if (mainCamera.getMode() == CameraMode.PLAYER) {
                    updateRaycast();
                    if (hoveredObject != null) {
                        if (glfwGetMouseButton(windowHandle, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                            this.selectedNode = hoveredObject; 
                        }
                    }
                }
            }
            
            // Lấy dữ liệu RAM thực tế từ osBean (com.sun.maWnagement.OperatingSystemMXBean)
			long totalPhys = osBean.getTotalPhysicalMemorySize();
            long freePhys = osBean.getFreePhysicalMemorySize();
            double ramUsagePercent = ((double) (totalPhys - freePhys) / totalPhys) * 100.0;
            long usedPhys = totalPhys - freePhys; // Tính toán lượng RAM đã dùng

            if (ramUsagePercent >= 95.0 && !isEmergencyClosing) {
                isEmergencyClosing = true;

                // Ngắt xử lý logic nặng ngay lập tức
                //Engine.getEngine().getLogicThread().interrupt();

                // Ẩn cửa sổ để giảm tải GPU Shared RAM
                glfwHideWindow(windowHandle);

                // Sử dụng System.err với chuỗi thô để tránh tạo thêm Object String phức tạp
                System.err.print("[EMERGENCY] RAM LIMIT REACHED: ");
                System.err.println(ramUsagePercent);

                // Thực hiện AutoSave khẩn cấp 
                // Lưu ý: Module Manager cần kiểm tra biến isEmergencyClosing này

                vn.pmgteam.yanase.util.CrashReport.make(
                    vn.pmgteam.yanase.util.CrashReport.ERR_EMERGENCY_EXIT,
                    ramUsagePercent
                );

                // Yêu cầu đóng và thoát vòng lặp chính
                glfwSetWindowShouldClose(windowHandle, true);
                
                // Ném lỗi để ngắt stack hiện tại nếu cần
                throw new vn.pmgteam.yanase.memory.WeakMemoryError("Critical RAM Usage", usedPhys, totalPhys);
            }
            
            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
            handleAutoSave();
        }
    }
    
    private void renderEngineSettingsOverlay() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        
        // 1. Vẽ lớp nền tối mờ (Dim Background)
        glColor4f(0.0f, 0.0f, 0.0f, 0.7f); // Đen trong suốt 70%
        glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // 2. Tiêu đề Menu
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        String title = "ENGINE SETTINGS";
        float titleX = (width - fontRenderer.getStringWidth(title)) / 2;
        drawDebugLine(title, titleX, height / 4);

        // 3. Hiển thị các tùy chọn (Ví dụ: VSync, MSAA...)
        drawDebugLine("1. VSync: " + (gameSettings.getVsyncInt() == 1 ? "ON" : "OFF"), width / 3, height / 2);
        drawDebugLine("2. MSAA: " + gameSettings.getMSAALevel() + "x", width / 3, height / 2 + 30);
        drawDebugLine("Press ESC to Return", width / 3, height / 2 + 90);
        
        // Nếu bạn đã có hệ thống UI Node (BaseNode), bạn có thể render nó ở đây
        // renderRecursive2D(settingsGuiNode);

        glEnable(GL_DEPTH_TEST);
    }

    private void renderDebugInfo() {
        glMatrixMode(GL_PROJECTION); glPushMatrix(); glLoadIdentity();
        int ww = gameSettings.getWindowWidth();
        int wh = gameSettings.getWindowHeight();
        glOrtho(0, ww, wh, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glPushMatrix(); glLoadIdentity();

        if (debugFont == null) debugFont = new FontRenderer("JetBrains Mono", 14);

        // 1. Tính toán tài nguyên hệ thống
        long maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long usedHeap = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long totalPhys = osBean.getTotalPhysicalMemorySize() / 1024 / 1024;
        long freePhys = osBean.getFreePhysicalMemorySize() / 1024 / 1024;
        long usedPhys = totalPhys - freePhys;
        double ramUsagePercent = ((double) usedPhys / totalPhys) * 100.0;

        // 2. Tính toán thời gian Day-Night (Minecraft Style)
        // Chuyển worldTime (0.0 -> 1.0) sang 24h
        int totalMinutes = (int) (worldTime * 24 * 60);
        int hours = (totalMinutes / 60) % 24;
        int minutes = totalMinutes % 60;
        
        String period = "Day";
        if (worldTime < 0.22f || worldTime > 0.78f) period = "Night";
        else if (worldTime < 0.28f) period = "Sunrise";
        else if (worldTime > 0.72f) period = "Sunset";

        // 3. Chuẩn bị danh sách thông tin hiển thị
        String[] lines = {
            "Yanase Engine " + gameSettings.getEngineVersion(),
            "--------------------------------------",
            String.format("Performance : %d fps (%.1fms)", fps, deltaTime * 1000),
            String.format("World Time  : %02d:%02d (%s)", hours, minutes, period),
            String.format("Pos (X,Y,Z) : %.1f, %.1f, %.1f", mainCamera.position.x, mainCamera.position.y, mainCamera.position.z),
            "--------------------------------------",
            "GPU Renderer: " + glGetString(GL_RENDERER),
            "Vertices    : " + vn.pmgteam.yanase.mesh.Mesh.renderVerticesCount,
            "JVM Heap    : " + usedHeap + "/" + maxHeap + " MB",
            String.format("System RAM  : %.1f%% %s", ramUsagePercent, ramUsagePercent >= 85 ? "[DANGER!]" : ""),
            "Resolution  : " + ww + "x" + wh
        };

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Ngưỡng màu RAM
        boolean isRamDanger = ramUsagePercent >= 85.0;
        boolean isRamHealthy = ramUsagePercent < 40.0;

        for (int i = 0; i < lines.length; i++) {
            // Thiết lập màu sắc theo dòng
            if (i == 3) { // Dòng World Time
                glColor4f(1.0f, 0.9f, 0.4f, 1.0f); // Màu vàng nhạt
            } else if (i == 4) { // Dòng Tọa độ
                glColor4f(0.5f, 1.0f, 0.5f, 1.0f); // Màu xanh lá nhạt
            } else if (i == 9) { // Dòng System RAM
                if (isRamDanger) glColor4f(1.0f, 0.2f, 0.2f, 1.0f);
                else if (isRamHealthy) glColor4f(0.2f, 1.0f, 0.2f, 1.0f);
                else glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            }
            
            drawDebugLine(lines[i], 10, 10 + (i * 20));
        }
        
        // --- CẢNH BÁO NGUY CẤP (Blink) ---
        if (ramUsagePercent >= 90.0) {
            boolean isBlinkVisible = (System.currentTimeMillis() % 250) < 125;
            if (isBlinkVisible) {
                glColor4f(1.0f, 1.0f, 0.0f, 1.0f);
                drawDebugLine(" [!] WARNING: RAM EXHAUSTED - SAVE NOW [!] ", 10, wh - 30); 
            }
        } else if (ramUsagePercent >= 85.0) {
            float blinkAlpha = (float) (Math.sin(System.currentTimeMillis() / 150.0) * 0.5 + 0.5);
            glColor4f(1.0f, 0.8f, 0.0f, blinkAlpha);
            drawDebugLine("[!] SYSTEM RAM CRITICAL: " + String.format("%.1f%%", ramUsagePercent), 10, wh - 30); 
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
    
    // Trong class Engine.java
    public vn.pmgteam.yanase.node.BaseNode getSceneGui() {
        return this.sceneGui;
    }

    public void setSceneGui(vn.pmgteam.yanase.node.BaseNode guiNode) {
        this.sceneGui = guiNode;
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
    
    protected void renderRecursive2D(Node node) {
        if (node instanceof vn.pmgteam.yanase.node.Object2D) {
            ((vn.pmgteam.yanase.node.Object2D) node).render2D();
        }

        org.w3c.dom.NodeList childrenList = node.getChildNodes();
        int count = childrenList.getLength();
        for (int i = 0; i < count; i++) {
            renderRecursive2D(childrenList.item(i));
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
    
    public void updateTime(float deltaTime) {
        // deltaTime tính bằng giây (ví dụ: 0.016 cho 60 FPS)
        // Chia deltaTime cho tổng thời gian chu kỳ để ra tỉ lệ % trôi qua
        worldTime += deltaTime / dayCycleLength;

        // Reset khi vượt quá 1.0 (kết thúc một ngày)
        if (worldTime > 1.0f) {
            worldTime -= 1.0f;
        }
    }
    
    // --- PHẦN 1: LOGIC HỖ TRỢ MÀU SẮC (Atmospheric Scattering) ---

    private void calculateAtmosphericColors(float time, float[] z, float[] h, float[] g) {
        // Định nghĩa các bảng màu cơ bản cho các mốc thời gian
        float[] nightZenith = {0.02f, 0.02f, 0.08f};
        float[] nightHorizon = {0.05f, 0.05f, 0.12f};
        
        float[] dayZenith = {0.12f, 0.35f, 0.80f};
        float[] dayHorizon = {0.55f, 0.78f, 0.95f};
        
        float[] sunsetHorizon = {1.0f, 0.45f, 0.15f}; // Màu cam cháy
        float[] sunsetZenith = {0.2f, 0.25f, 0.45f};  // Tím xanh

        float factor;
        if (time >= 0.2f && time <= 0.8f) { // BAN NGÀY (Bao gồm Bình minh & Hoàng hôn)
            // Tính factor: Trưa (0.5) = 1.0 | Sáng/Chiều (0.2 hoặc 0.8) = 0.0
            factor = 1.0f - (Math.abs(time - 0.5f) / 0.3f); 
            factor = Math.max(0, Math.min(1, factor));

            if (factor < 0.4f) { // Chuyển tiếp giữa Sunset và Day
                float blend = factor / 0.4f;
                lerpColor(sunsetZenith, dayZenith, blend, z);
                lerpColor(sunsetHorizon, dayHorizon, blend, h);
            } else {
                System.arraycopy(dayZenith, 0, z, 0, 3);
                System.arraycopy(dayHorizon, 0, h, 0, 3);
            }
        } else { // BAN ĐÊM
            // Tính độ sáng ban đêm (càng gần 0.0 hoặc 1.0 càng tối)
            float nFactor = (time < 0.2f) ? (time / 0.2f) : ((1.0f - time) / 0.2f);
            lerpColor(nightZenith, sunsetZenith, nFactor, z);
            lerpColor(nightHorizon, sunsetHorizon, nFactor, h);
        }
        
        // Màu đất (Ground) luôn tối hơn chân trời 60%
        for(int i=0; i<3; i++) g[i] = h[i] * 0.4f;
    }

    private void lerpColor(float[] from, float[] to, float f, float[] res) {
        for (int i = 0; i < 3; i++) res[i] = from[i] + (to[i] - from[i]) * f;
    }

    // --- PHẦN 2: HÀM RENDER SKY HOÀN CHỈNH ---

    protected void renderSky() {
        // Cập nhật thời gian trước khi render
        updateTime(deltaTime);

        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glPushMatrix();

        glLoadIdentity();
        glRotatef(mainCamera.rotation.x, 1, 0, 0);
        glRotatef(mainCamera.rotation.y, 0, 1, 0);
        glRotatef(mainCamera.rotation.z, 0, 0, 1);

        // Lấy màu sắc dựa trên worldTime hiện tại
        float[] currentZenith = new float[3];
        float[] currentHorizon = new float[3];
        float[] currentGround = new float[3];
        calculateAtmosphericColors(worldTime, currentZenith, currentHorizon, currentGround);

        float radius = 50.0f; // Bán kính đủ lớn để không bị cắt bởi Far Plane
        int sectors = 48;     // Giảm nhẹ để tiết kiệm RAM (từ 64 xuống 48)
        int stacks = 24;      

        for (int i = 0; i < stacks; i++) {
            float rho0 = (float) (Math.PI * (float) i / stacks);
            float rho1 = (float) (Math.PI * (float) (i + 1) / stacks);

            glBegin(GL_TRIANGLE_STRIP);
            for (int j = 0; j <= sectors; j++) {
                float phi = (float) (2.0 * Math.PI * (float) j / sectors);
                
                float x, y, z;

                // Điểm 1
                x = (float) (Math.sin(rho0) * Math.cos(phi));
                y = (float) Math.cos(rho0);
                z = (float) (Math.sin(rho0) * Math.sin(phi));
                setSkyColor(y, currentZenith, currentHorizon, currentGround);
                glVertex3f(x * radius, y * radius, z * radius);

                // Điểm 2
                x = (float) (Math.sin(rho1) * Math.cos(phi));
                y = (float) Math.cos(rho1);
                z = (float) (Math.sin(rho1) * Math.sin(phi));
                setSkyColor(y, currentZenith, currentHorizon, currentGround);
                glVertex3f(x * radius, y * radius, z * radius);
            }
            glEnd();
        }

        glPopMatrix();
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glColor4f(1, 1, 1, 1);
    }
    private void setSkyColor(float y, float[] zenith, float[] horizon, float[] ground) {
        // y chạy từ 1 (đỉnh) xuống -1 (đáy)
        if (y >= 0) {
            // Pha trộn từ Horizon lên Zenith
            float t = (float) Math.pow(y, 0.7); // Hàm mũ để dải màu mềm hơn
            glColor3f(
                horizon[0] * (1-t) + zenith[0] * t,
                horizon[1] * (1-t) + zenith[1] * t,
                horizon[2] * (1-t) + zenith[2] * t
            );
        } else {
            // Pha trộn từ Horizon xuống Ground
            float t = (float) Math.pow(-y, 0.5);
            glColor3f(
                horizon[0] * (1-t) + ground[0] * t,
                horizon[1] * (1-t) + ground[1] * t,
                horizon[2] * (1-t) + ground[2] * t
            );
        }
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
    private void cleanup() {
        // 1. Hiển thị thông báo đang đóng
        if (windowHandle != NULL) {
            renderClosingScreen();
            // Đợi một chút để người dùng kịp thấy dòng chữ (khoảng 0.5s)
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }

        // 2. Thực hiện các logic dọn dẹp của lớp con
        onCleanup(); 

        // 3. Dọn dẹp Scene Graph (Đệ quy dọn dẹp tài nguyên GPU)
        if (sceneRoot != null) sceneRoot.cleanup(); 
        if (sceneGui != null) sceneGui.cleanup(); 

        // 4. Giải phóng Font
        if (debugFont != null) {
            // Lưu ý: Cần có hàm cleanup trong FontRenderer để delete texture
            debugFont.cleanup(); 
        }

        // 5. Phá hủy cửa sổ và kết thúc GLFW
        if (windowHandle != NULL) {
            glfwDestroyWindow(windowHandle);
        }
        glfwTerminate();
        
        System.out.println("=========================================");
        System.out.println("| Status: Yanase Engine Terminated.     |");
        System.out.println("=========================================");
    }

    private void renderClosingScreen() {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glMatrixMode(GL_PROJECTION); glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW); glLoadIdentity();

        String closingText = "Closing Game......";
        if (debugFont == null) debugFont = new FontRenderer("JetBrains Mono", 14);
        
        FontRenderer.TextTextureData data = debugFont.getStringTextureData(closingText, Color.WHITE);
        
        // Căn giữa dòng chữ
        float x = (width - data.width) / 2.0f;
        float y = (height - data.height) / 2.0f;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        debugFont.drawCachedTexture(data.id, x, y, data.width, data.height);
        glDeleteTextures(data.id);
        
        glDisable(GL_BLEND);
        glfwSwapBuffers(windowHandle);
    }
    public abstract void onCleanup();
}