package vn.pmgteam.yanase.settings;

import static org.lwjgl.glfw.GLFW.*;
import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.base.render.RenderingHints;

public class GameSettings {
    
    private Engine engine;
    private String engineVersion = "0.0.2";
    private String projectTitle = "Yanase Project";
    
    // --- CẤU HÌNH ĐỒ HỌA ---
    private int msaaLevel = RenderingHints.AA_MSAA_4X;
    private boolean useModernTitleWindow = false;
    private boolean enableVsync = true;
    private int windowWidth = 1280;
    private int windowHeight = 720;
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    
    private boolean dirty = false;
    
    public boolean isVsyncEnabled() {
        return enableVsync;
    }
    
    // Trong GameSettings.java
    private boolean showSettingsMenu = false;
    
    private boolean fullscreenMode = false;

    // --- CẤU HÌNH PHÍM BẤM (KEYBINDINGS) ---
    // Chúng ta đặt mặc định các phím phổ biến
    private int keyToggleDebug = GLFW_KEY_F3;
    private int keyEmergencyExit = GLFW_KEY_ESCAPE;
    private int keyScreenshot = GLFW_KEY_F2;

    public GameSettings(Engine engine) {
        this.engine = engine;
    }

    // --- GETTERS & SETTERS CHO PHÍM BẤM ---
    

    public boolean isShowSettingsMenu() { return showSettingsMenu; }
    public void setShowSettingsMenu(boolean show) { this.showSettingsMenu = show; }
    public void toggleSettingsMenu() { this.showSettingsMenu = !this.showSettingsMenu; }
    
    public int getKeyToggleDebug() { return keyToggleDebug; }
    public void setKeyToggleDebug(int key) { this.keyToggleDebug = key; }
    
    public boolean getFullscreenMode() {
    	 return fullscreenMode;
    }
    
    public void setFullscreenMode(boolean isFullscreen) {
    	this.fullscreenMode = isFullscreen;
    }

    public int getKeyEmergencyExit() { return keyEmergencyExit; }
    public void setKeyEmergencyExit(int key) { this.keyEmergencyExit = key; }

    // Hàm kiểm tra nhanh phím nhấn có trùng với cấu hình không
    public boolean isDebugKey(int key) { return key == keyToggleDebug; }
    public boolean isExitKey(int key) { return key == keyEmergencyExit; }

    // --- CÁC PHƯƠNG THỨC HIỆN CÓ ---

    public void applyToEngine() {
        glfwWindowHint(GLFW_SAMPLES, msaaLevel);
        if(useModernTitleWindow) glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
    }

    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
    public void setWindowWidth(int newWidth) { this.windowWidth = newWidth; }
    public void setWindowHeight(int newHeight) { this.windowHeight = newHeight; }
    
    public void setWindowResolution(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
    }

    public boolean isModernTitleWindow() { return useModernTitleWindow; }
    public void setModernTitleWindow(boolean isEnable) { this.useModernTitleWindow = isEnable; }

    public void setMSAALevel(int level) { this.msaaLevel = level; }
    public int getMSAALevel() { return msaaLevel; }

    public void setWindowTitle(String title) { this.projectTitle = title; }
    public String getProjectTitle() { return projectTitle; }

    public void setVSyncEnable(boolean isEnable) {
        if (this.enableVsync != isEnable) {
            this.enableVsync = isEnable;
            this.dirty = true;
        }
    }
    public int getVsyncInt() { return enableVsync ? 1 : 0; }

    public boolean isDirty() { return dirty; }
    public void clearDirty() { this.dirty = false; }
    public String getEngineVersion() { return engineVersion; }
}