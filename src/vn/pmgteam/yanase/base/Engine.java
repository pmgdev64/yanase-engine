package vn.pmgteam.yanase.base;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
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
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.node.subnodes.CameraMode;
import vn.pmgteam.yanase.node.subnodes.CameraNode;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.scene.SceneManager;
import vn.pmgteam.yanase.settings.GameSettings;
import vn.pmgteam.yanase.test.WorldScene;
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
    protected boolean running = false;
    
    private boolean isEmergencyClosing = false;
    
    private int splashTextureId = -1; // Lưu trữ ID của file jpg
    
    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    
    private float currentMouseX, currentMouseY;
    // Trong lớp Engine hoặc EditorScene
    private Object3D hoveredObject = null;
    private Object3D selectedNode = null;
    
    // Một chu kỳ ngày đêm tính bằng giây (ví dụ: 600 giây = 10 phút)
    private float dayCycleLength = 600.0f; 
    private float worldTime = 0.25f; // Bắt đầu ở 0.25 (Bình minh)
    
    public SceneManager sceneManager;
       
    // Thêm một flag để Engine tự nhận biết môi trường
    private boolean isEditorMode = false;

    public boolean isRunning() {
        return running;
    }
    
    public SceneManager getSceneManager() {
    	return sceneManager;
    }
    
 // Khai báo ở đầu class Engine
    private final org.joml.Matrix4f worldMatrix = new org.joml.Matrix4f();
    
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

    public void startApplication() {
        // --- PRE-FLIGHT RAM CHECK ---
        if (!isSystemHealthy()) {
            System.err.println("[SystemGuard] Khởi động thất bại: Không đủ tài nguyên hệ thống.");
            // Bạn có thể hiện một Message Box đơn giản ở đây trước khi exit
            return; 
        }

        System.out.println("[SystemGuard] RAM Check Passed. Khởi động Yanase Engine...");
        this.run();
    }

    private boolean isSystemHealthy() {
        // --- 1. CONFIGURATION ---
        final long CRITICAL_RAM_MB = 180; // Dưới mức này Windows 11 sẽ bắt đầu "ngáp"
        final long WARNING_RAM_MB = 350;  // Mức khuyến cáo để chạy mượt
        
        System.out.println("[SystemGuard] Đang phân tích tài nguyên hệ thống...");

        try {
            oshi.SystemInfo si = new oshi.SystemInfo();
            var memory = si.getHardware().getMemory();
            
            // --- 2. FORCED CLEANUP ---
            // Thử gọi GC để dọn dẹp các object rác từ quá trình init class trước đó
            System.gc();
            Thread.sleep(50); // Nghỉ một chút để GC kịp làm việc

            // --- 3. PHYSICAL RAM CHECK ---
            long availableBytes = memory.getAvailable();
            long totalBytes = memory.getTotal();
            long availableMB = availableBytes / (1024 * 1024);
            double percentFree = (double) availableBytes / totalBytes * 100.0;

            // --- 4. VIRTUAL MEMORY (PAGEFILE) CHECK ---
            // Nếu Pagefile đã đầy > 90%, máy sẽ cực lag dù RAM còn trống
            long swapUsed = memory.getVirtualMemory().getSwapUsed();
            long swapTotal = memory.getVirtualMemory().getSwapTotal();
            double swapUsagePercent = (swapTotal > 0) ? (double) swapUsed / swapTotal * 100.0 : 0;

            // --- 5. LOGGING DETAILED REPORT ---
            System.out.printf("[SystemGuard] RAM trống: %d MB (%.1f%%)\n", availableMB, percentFree);
            System.out.printf("[SystemGuard] Pagefile usage: %.1f%%\n", swapUsagePercent);

            // --- 6. TRIỆT ĐỂ: CÁC ĐIỀU KIỆN TỪ CHỐI ---
            
            // Điều kiện 1: RAM vật lý quá thấp
            if (availableMB < CRITICAL_RAM_MB) {
                showFatalDialog("Tài nguyên RAM không đủ (" + availableMB + "MB). Vui lòng đóng bớt các ứng dụng như Chrome hoặc Discord.");
                return false;
            }

            // Điều kiện 2: Pagefile quá tải (Dấu hiệu của việc hệ thống đang swap mạnh)
            if (swapUsagePercent > 95.0) {
                showFatalDialog("Hệ thống đang quá tải bộ nhớ ảo (Swap > 95%). Khởi động lúc này sẽ gây treo máy.");
                return false;
            }

            // Điều kiện 3: Cảnh báo nhưng vẫn cho chạy (Optimized Mode)
            if (availableMB < WARNING_RAM_MB) {
                System.out.println("[SystemGuard] CẢNH BÁO: Tài nguyên thấp. Kích hoạt chế độ Low-RAM cho Engine.");
                // Tại đây bạn có thể set một biến flag: gameSettings.setLowRamMode(true);
            }

            return true;

        } catch (Exception e) {
            // Fallback an toàn nếu OSHI gặp sự cố
            return Runtime.getRuntime().freeMemory() / (1024 * 1024) > 120;
        }
    }

    private void showFatalDialog(String message) {
        System.err.println("!!!! FATAL ERROR: " + message + " !!!!");
        // Native Dialog để người dùng không phải nhìn console
        javax.swing.JOptionPane.showMessageDialog(null, 
            message, 
            "Yanase Engine - Out of Memory", 
            javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    @Override
    public void run() {
        // 1. Thiết lập bẫy lỗi cho toàn bộ các Thread (Global Trap)
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handleFatalError(throwable, identifyErrorCode(throwable));
        });

        try {
            init();
            onInit(); // Nơi xảy ra lỗi NPE "this.world is null"
            loop();
        } catch (Throwable t) { 
            // 2. Bắt mọi lỗi (bao gồm cả Error và Exception) ở Thread chính
            // và đẩy sang handleFatalError để hiện GUI
            handleFatalError(t, identifyErrorCode(t));
        } finally {
            // 3. Chỉ dọn dẹp bình thường nếu không phải là sập khẩn cấp
            if (!isEmergencyClosing) {
                cleanup();
            }
        }
    }
    
    private String identifyErrorCode(Throwable t) {
        if (t instanceof OutOfMemoryError) {
            return "FATAL_MEMORY_EXHAUSTED_OOM"; // Lỗi ám ảnh nhất trên máy 4GB RAM
        }
        if (t instanceof NullPointerException) {
            return "NULL_POINTER_REFERENCE";
        }
        if (t instanceof StackOverflowError) {
            return "STACK_OVERFLOW_RECURSION_LIMIT";
        }
        if (t instanceof ArrayIndexOutOfBoundsException) {
            return "ARRAY_INDEX_OUT_OF_BOUNDS";
        }
        if (t.getMessage() != null && t.getMessage().contains("GLFW")) {
            return "GLFW_WINDOW_CONTEXT_FAILURE";
        }
        if (t.getMessage() != null && t.getMessage().contains("OpenGL")) {
            return "OPENGL_RENDERER_FATAL";
        }
        
        // Nếu không xác định được, lấy tên Class của Exception làm mã lỗi
        return t.getClass().getSimpleName().toUpperCase();
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
    
    private void handleFatalError(Throwable t, String errorCode) {
        if (this.isEmergencyClosing) return; // Tránh vòng lặp vô hạn nếu chính hàm này lỗi
        this.isEmergencyClosing = true;
        this.running = false;

        // QUAN TRỌNG: AutoSave ngay lập tức (Theo yêu cầu: Toggle/Crash là phải Save)
        handleAutoSave(); 

        // Dọn dẹp tài nguyên Engine để nhường RAM cho CrashReport GUI
        try {
            if (windowHandle != NULL) {
                glfwHideWindow(windowHandle);
                glfwDestroyWindow(windowHandle);
            }
            glfwTerminate();
        } catch (Exception ignored) {}

        // Gọi hàm make (đã có CountDownLatch bên trong để chặn Main Thread)
        vn.pmgteam.yanase.util.CrashReport.make(errorCode, 0.0, t);

        // Sau khi người dùng đóng CrashReport, chương trình mới thực sự kết thúc
        System.err.println("[TERMINATED] Engine closed via Crash Handler.");
        System.exit(1);
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
            throw new IllegalStateException("GLFW init failed");
        }

        System.out.println("[WINDOW] Applying Game Settings...");
        gameSettings.applyToEngine();

        // --- XỬ LÝ FULLSCREEN TẠI ĐÂY ---
        long monitor = NULL;
        int width = gameSettings.getWindowWidth();
        int height = gameSettings.getWindowHeight();

        if (gameSettings.getFullscreenMode()) {
            monitor = glfwGetPrimaryMonitor(); // Lấy màn hình chính
            GLFWVidMode vidmode = glfwGetVideoMode(monitor);
            if (vidmode != null) {
            	width = vidmode.width(); // Ép độ phân giải theo màn hình nếu muốn chuẩn Fullscreen
            	height = vidmode.height();
            }
            System.out.println("[WINDOW] Mode: Fullscreen (" + width + "x" + height + ")");
        } else {
            System.out.println("[WINDOW] Mode: Windowed (" + width + "x" + height + ")");
        }

        windowHandle = glfwCreateWindow(width, height, gameSettings.getProjectTitle(), monitor, NULL);

        if (windowHandle == NULL) {
            throw new RuntimeException("Window creation failed");
        }

        // Center window nếu không phải fullscreen
        if (monitor == NULL) {
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            glfwSetWindowPos(windowHandle, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
        }
        // --- STAGE 3: OPENGL CONTEXT & CAPABILITIES ---
        System.out.println("[RENDER] Setting up OpenGL Context...");
        glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities(); 
        
        // --- BỔ SUNG: Đặt Viewport ban đầu ---
        int[] fbW = new int[1], fbH = new int[1];
        glfwGetFramebufferSize(windowHandle, fbW, fbH);
        glViewport(0, 0, fbW[0], fbH[0]); // <--- DÒNG NÀY SẼ SỬA LỖI
        System.out.println("[RENDER] Initial Viewport: " + fbW[0] + "x" + fbH[0]);
        // -------------------------------------
        
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
        this.sceneManager = new SceneManager();

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("==================================================");
        System.out.println("[INIT] " + centerString("YANASE ENGINE READY", frameWidth - 8));
        System.out.println("[INIT] Boot time: " + loadTime + "ms");
        System.out.println("==================================================");
    }
    
    public void initForEditor(int w, int h, long existingWindowHandle) {
        this.width = w;
        this.height = h;
        this.isEditorMode = true;
        long startTime = System.currentTimeMillis();

        preFlightCheck();

        // KHÔNG tạo window mới — dùng window của MainStudio
        this.windowHandle = existingWindowHandle;

        fontRenderer = new FontRenderer("JetBrains Mono", 14);
        mainCamera = new CameraNode("MainCamera");
        mainCamera.position.set(0, 0, 5);
        sceneRoot.appendChild(mainCamera);

        this.isSplashFinished = true;
        this.sceneManager = new SceneManager();
        this.running = true;

        System.out.println("[INIT-EDITOR] Engine Metadata Ready in " + (System.currentTimeMillis() - startTime) + "ms");
    }
    
    private void preFlightCheck() {
        // --- STAGE 1: SYSTEM PRE-FLIGHT (Logic gốc của bạn) ---
        int frameWidth = 50;
        
        System.out.println("==================================================");
        System.out.println("[PRE-INIT] Starting Yanase Engine...");
        System.out.println("[PRE-INIT] Operating System: " + System.getProperty("os.name"));
        System.out.println("[PRE-INIT] Java Version: " + System.getProperty("java.version"));
        
        // Log thông số RAM để giám sát ngưỡng 92% cho máy 4GB
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / 1024 / 1024;
        long totalMem = runtime.totalMemory() / 1024 / 1024;
        long freeMem = runtime.freeMemory() / 1024 / 1024;
        
        System.out.println("[PRE-INIT] Memory: " + totalMem + "MB total, " + maxMem + "MB max");
        System.out.println("[PRE-INIT] Free Memory: " + freeMem + "MB");
        System.out.println("==================================================");
    }

    /**
     * Hàm khởi tạo tương đương init() nhưng lược bỏ các thành phần 
     * gây nặng hoặc xung đột với JavaFX/Studio.
     */
    
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
        
        glfwSetCursorPosCallback(windowHandle, (win, xpos, ypos) -> {
            // Nếu Menu đang mở, chúng ta dừng cập nhật tọa độ chuột cho logic xoay Cam
            if (gameSettings.isShowSettingsMenu()) {
                this.currentMouseX = (float) xpos;
                this.currentMouseY = (float) ypos;
                return; // THOÁT: Không cho Camera nhận dữ liệu di chuyển này
            }

            this.currentMouseX = (float) xpos;
            this.currentMouseY = (float) ypos;
        });
        
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                
            	if (gameSettings.isExitKey(key)) {
            	    gameSettings.toggleSettingsMenu();
            	    
            	    if (sceneManager != null && sceneManager.getCurrentScene() instanceof WorldScene) {
            	        WorldScene world = (WorldScene) sceneManager.getCurrentScene();
            	        CameraNode player = world.getPlayer();
            	        
            	        if (gameSettings.isShowSettingsMenu()) {
            	            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            	            // Dừng ngay lập tức để nhân vật không bị trôi khi đang mở menu
            	            if (player != null) player.getCurrentVelocity().set(0); 
            	        } else {
            	            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            	            if (player != null) {
            	                player.setMode(CameraMode.PLAYER);
            	                // QUAN TRỌNG: Reset mốc chuột để không bị văng góc nhìn
            	                player.setFirstMouse(true); 
            	            }
            	        }
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
            // --- BƯỚC 0: CẬP NHẬT SỰ KIỆN TRƯỚC (QUAN TRỌNG NHẤT) ---
            // Đưa PollEvents lên đầu để isShowSettingsMenu cập nhật ngay khi nhấn ESC
            glfwPollEvents(); 
            
            // --- CẬP NHẬT VIEWPORT & SIZE (Bổ sung tại đây) ---
            int[] fW = new int[1], fH = new int[1];
            glfwGetFramebufferSize(windowHandle, fW, fH);
            glViewport(0, 0, fW[0], fH[0]); // Ép OpenGL vẽ trên toàn bộ diện tích mới
            
            // Kiểm tra trạng thái menu ngay lập tức
            boolean isMenuOpen = gameSettings.isShowSettingsMenu();

            // Xóa màn hình
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Cập nhật thời gian
            double currentTime = glfwGetTime();
            deltaTime = (float) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            // Tính toán FPS
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
                glEnable(GL_DEPTH_TEST);
                renderSky(); 
                
                // --- BỔ SUNG: Lấy Framebuffer thật ---
                int[] w = new int[1], h = new int[1];
                glfwGetFramebufferSize(windowHandle, w, h);
                // mainCamera.setAspect((float) width / (float) height); // <-- Sửa dòng này
                mainCamera.setAspect((float) w[0] / (float) h[0]); // <-- Thành dòng này
                
                mainCamera.applyProjection();
                mainCamera.applyViewMatrix(); 
                
                sceneRoot.updateTransform(new Matrix4f());
                
                // CHỈ chạy logic thế giới (Update vị trí, Camera) khi KHÔNG mở Menu
                if (!isMenuOpen) {
                    onLoop(); 
                } else {
                    // Nếu Menu mở, ép chuột hiện ra để chắc chắn không bị node nào khóa lại
                    if (glfwGetInputMode(windowHandle, GLFW_CURSOR) != GLFW_CURSOR_NORMAL) {
                        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    }
                }
                
                renderRecursive(sceneRoot);
                
                // --- BƯỚC 2: RENDER GUI & OVERLAY (Hệ 2D) ---
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glMatrixMode(GL_PROJECTION); 
                glPushMatrix(); 
                glLoadIdentity();
                glOrtho(0, width, height, 0, -1, 1);
                
                glMatrixMode(GL_MODELVIEW); 
                glPushMatrix(); 
                glLoadIdentity();

                // Vẽ các thành phần 2D
                if (sceneGui != null) {
                    renderRecursive2D(sceneGui);
                }

                // Chỉ vẽ Crosshair khi đang chơi và không mở Menu
                if (!isMenuOpen && mainCamera.getMode() == CameraMode.PLAYER) {
                    renderCrosshair();
                }

                if (showDebug) {
                    renderDebugInfo(); 
                }

                // Vẽ Menu Settings (Phủ đen toàn màn hình)
                if (isMenuOpen) {
                    renderEngineSettingsOverlay();
                }

                glMatrixMode(GL_PROJECTION); 
                glPopMatrix();
                glMatrixMode(GL_MODELVIEW); 
                glPopMatrix();
                
                glEnable(GL_DEPTH_TEST);
                glDisable(GL_BLEND);

                // --- BƯỚC 3: LOGIC HẬU RENDER (Raycast, Collision) ---
                // Chỉ xử lý Raycast khi không mở Menu để tránh click xuyên thấu vào thế giới
                if (!isMenuOpen) {
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
            }
            
            // --- BƯỚC 4: KIỂM TRA RAM KHẨN CẤP ---
            checkRamEmergency();
            
            // Hoàn tất frame
            glfwSwapBuffers(windowHandle);
            handleAutoSave();
        }
    }
    
    public void update() {
        if (!running) return;
        
        // Cập nhật DeltaTime
        double currentTime = glfwGetTime();
        deltaTime = (float) (currentTime - lastFrameTime);
        lastFrameTime = currentTime;

        // Logic xử lý chính (onLoop được triển khai ở lớp con)
        onLoop(); 
    }

    public void renderToBuffer(java.nio.IntBuffer targetBuffer) {
        if (!running || targetBuffer == null || !targetBuffer.isDirect()) return;
        
        // Đảm bảo Viewport khớp với kích thước ảnh
        glViewport(0, 0, width, height);
        
        glClearColor(0.1f, 0.1f, 0.12f, 1.0f); 
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Render 3D
        glEnable(GL_DEPTH_TEST);
        renderSky(); 
        mainCamera.applyProjection();
        mainCamera.applyViewMatrix(); 
        
        worldMatrix.identity();
        sceneRoot.updateTransform(worldMatrix);
        renderRecursive(sceneRoot);

        // Render 2D Overlay
        glDisable(GL_DEPTH_TEST);
        if (showDebug) renderDebugInfo();
        glEnable(GL_DEPTH_TEST);

        checkRamEmergency();

        // Đồng bộ và copy
        glFlush();
        glFinish(); 
        targetBuffer.rewind(); 
        glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, targetBuffer);
    }

    /**
     * Tách logic kiểm tra RAM để loop chính gọn gàng hơn
     */
    private void checkRamEmergency() {
        long totalPhys = osBean.getTotalPhysicalMemorySize();
        long freePhys = osBean.getFreePhysicalMemorySize();
        double ramUsagePercent = ((double) (totalPhys - freePhys) / totalPhys) * 100.0;
        long usedPhys = totalPhys - freePhys;

        if (ramUsagePercent >= 95.0 && !isEmergencyClosing) {
            isEmergencyClosing = true;
            glfwHideWindow(windowHandle);
            System.err.println("[EMERGENCY] RAM LIMIT REACHED: " + ramUsagePercent + "%");

            try {
                // Logic load block hoặc phát video mp4 ở đây
            } catch (Throwable t) {
                vn.pmgteam.yanase.util.CrashReport.make(
                    vn.pmgteam.yanase.util.CrashReport.ERR_OPENGL_FATAL,
                    ramUsagePercent,
                    t // <--- Truyền t để hiện Stack Trace màu đỏ rực kiểu MC
                );
            }

            glfwSetWindowShouldClose(windowHandle, true);
            throw new vn.pmgteam.yanase.memory.WeakMemoryError("Critical RAM Usage", usedPhys, totalPhys);
        }
    }
    
    private void renderEngineSettingsOverlay() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // 1. Vẽ lớp nền tối mờ
        glColor4f(0.0f, 0.0f, 0.0f, 0.75f); 
        glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // 2. Tiêu đề
        glColor4f(0.2f, 0.6f, 1.0f, 1.0f); // Màu xanh Cyan nhẹ
        String title = "--- ENGINE SETTINGS ---";
        drawDebugLine(title, (width - fontRenderer.getStringWidth(title)) / 2, height / 4);

        // 3. Các tùy chọn có thể Toggle
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawDebugLine("[F1] VSync: " + (gameSettings.isVsyncEnabled() ? "ENABLED" : "DISABLED"), width / 3, height / 2);
        drawDebugLine("[F2] MSAA:  " + gameSettings.getMSAALevel() + "x", width / 3, height / 2 + 30);
        drawDebugLine("[F3] Debug: " + (showDebug ? "VISIBLE" : "HIDDEN"), width / 3, height / 2 + 60);
        
        glColor4f(0.7f, 0.7f, 0.7f, 1.0f);
        drawDebugLine("Press ESC to Close", width / 3, height / 2 + 120);

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
        // 1. Chỉ render nếu là Object2D
        if (node instanceof vn.pmgteam.yanase.node.Object2D) {
            ((vn.pmgteam.yanase.node.Object2D) node).render2D();
        }

        // 2. DUYỆT THEO DANH SÁCH CỦA ENGINE (Tránh dùng getChildNodes() của DOM)
        if (node instanceof BaseNode) {
            BaseNode baseNode = (BaseNode) node;
            for (BaseNode child : baseNode.getChildren()) {
                renderRecursive2D(child);
            }
        }
    }
    
    private void renderSplashScreen() {
        double elapsedTime = glfwGetTime() - splashStartTime;
        float alpha = getAlpha(elapsedTime);

        // --- 1. RESOURCE INITIALIZATION ---
        if (splashTextureId == -1) {
            splashTextureId = TextureManager.loadTexture("/assets/yanase-splash.jpg");
        }
        if (splashFont == null) {
            splashFont = new FontRenderer("Segoe UI", 18);
        }
        if (splashFont == null) return;

        // --- 2. UPDATE VIEWPORT & DYNAMIC SIZING ---
        // Lấy kích thước thật từ Framebuffer để tránh lỗi trên màn hình High-DPI hoặc khi đổi Resolution
        int[] fbW = new int[1], fbH = new int[1];
        glfwGetFramebufferSize(windowHandle, fbW, fbH);
        int ww = fbW[0];
        int wh = fbH[0];

        // Cập nhật Viewport ngay lập tức
        glViewport(0, 0, ww, wh); 
        
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Thiết lập Ortho khớp hoàn toàn với kích thước mới
        glMatrixMode(GL_PROJECTION); 
        glLoadIdentity();
        glOrtho(0, ww, wh, 0, -1, 1); 
        
        glMatrixMode(GL_MODELVIEW); 
        glLoadIdentity();

        // --- 3. RENDER MAIN LOGO (Tự động căn giữa theo ww, wh mới) ---
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        float imgW = 800.0f; 
        float imgH = 450.0f;
        
        // Đảm bảo logo không to hơn màn hình nếu user để Res thấp
        if (imgW > ww) {
            float scale = (float)ww / imgW;
            imgW *= scale;
            imgH *= scale;
        }

        float x = (ww - imgW) / 2.0f;
        float y = (wh - imgH) / 2.0f;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, splashTextureId);
        glColor4f(1.0f, 1.0f, 1.0f, alpha); 

        glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(x, y);
            glTexCoord2f(1, 0); glVertex2f(x + imgW, y);
            glTexCoord2f(1, 1); glVertex2f(x + imgW, y + imgH);
            glTexCoord2f(0, 1); glVertex2f(x, y + imgH);
        glEnd();

        // --- 4. RENDER SUBTITLE TEXT ---
        var textData = splashFont.getStringTextureData("Created with Yanase Engine", java.awt.Color.WHITE);
        if (textData != null) {
            float textX = (ww - textData.width) / 2.0f;
            float textY = y + imgH + 40; 
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            splashFont.drawCachedTexture(textData.id, textX, textY, textData.width, textData.height);
        }

        // --- 5. CLEANUP ---
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
    
    private float getAlpha(double time) {
        // Fade In: 1s | Stay: 2s | Fade Out: 1s
        if (time <= 1.0) return (float) time;
        if (time <= 3.0) return 1.0f;
        if (time <= 4.0) return (float) (1.0 - (time - 3.0));
        return 0.0f;
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
    
    public void editorCleanup() {
        this.running = false; // Dừng vòng lặp Virtual Thread ngay lập tức

        // 1. Chỉ hiển thị màn hình đóng nếu KHÔNG phải Editor Mode
        if (!isEditorMode && windowHandle != NULL) {
            renderClosingScreen();
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }

        // 2. Logic dọn dẹp của lớp con (onCleanup là abstract/protected)
        onCleanup(); 

        // 3. Dọn dẹp tài nguyên GPU (Mesh, Texture)
        if (sceneRoot != null) sceneRoot.cleanup(); 
        if (sceneGui != null) sceneGui.cleanup(); 

        // 4. Giải phóng Font
        if (debugFont != null) debugFont.cleanup(); 
        if (fontRenderer != null) fontRenderer.cleanup();

        // 5. Quản lý Window Context
        if (windowHandle != NULL) {
            glfwDestroyWindow(windowHandle);
            windowHandle = NULL;
        }

        // QUAN TRỌNG: Chỉ Terminate khi đóng toàn bộ ứng dụng (Studio)
        // Nếu gọi ở đây, bạn sẽ không thể mở lại tab Editor thứ 2
        // glfwTerminate(); 
        
        System.out.println("[CLEANUP] Yanase Engine resources released safely.");
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