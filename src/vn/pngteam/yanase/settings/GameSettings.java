package vn.pngteam.yanase.settings;

import vn.pngteam.yanase.base.Engine;
import vn.pngteam.yanase.base.render.RenderingHints;

import static org.lwjgl.glfw.GLFW.*;

public class GameSettings {
    
    private Engine engine;
    
    // Antialiasing sử dụng Interface RenderingHints bạn đã tạo
    private int msaaLevel = RenderingHints.AA_MSAA_4X;
    
    private boolean useModernTitleWindow = false;
    
    private boolean enableVsync = true;
    
    private boolean dirty = false;
    
    // Độ phân giải mặc định
    private int windowWidth = 1280;
    private int windowHeight = 720;
    
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;
    
    private String engineVersion = "0.0.1";
    
    public int getWindowWidth() {
    	return windowWidth;
    }
    
    public int getWindowHeight() {
    	return windowHeight;
    }
    
    public void setWindowResolution(int width, int height) {
    	this.windowWidth = width;
    	this.windowHeight = height;
    }
    
    public boolean isModernTitleWindow() {
        return useModernTitleWindow;
    }
    
    // Đặt mức độ khử răng cưa tùy game
    public void setMSAALevel(int level) {
    	this.msaaLevel = level;
    }
    
    // Có thể bật tùy chỉnh titleWindow thông qua hàm này
    public void setModernTitleWindow(boolean isEnable) {
    	this.useModernTitleWindow = isEnable;
    }
    
    public void setWindowTitle(String title) {
    	this.projectTitle = title;
    }

    public void setVSyncEnable(boolean isEnable) {
        if (this.enableVsync != isEnable) {
            this.enableVsync = isEnable;
            this.dirty = true;
        }
    }

    public boolean isDirty() { return dirty; }
    public void clearDirty() { this.dirty = false; }
    
    // Tên dự án (Hiển thị trên tiêu đề Eclipse/Engine)
    private String projectTitle = "Yanase Project";
    
    public String getProjectTitle() {
    	return projectTitle;
    }
    
    public int getVsyncInt() {
        return enableVsync ? 1 : 0;
    }

    public GameSettings(Engine engine) {
        this.engine = engine;
    }

    /**
     * Phương thức này sẽ được gọi trước khi engine.init() 
     * để thiết lập các glfwWindowHint tương ứng.
     */
    public void applyToEngine() {
        // Ví dụ:
        glfwWindowHint(GLFW_SAMPLES, msaaLevel);
        if(useModernTitleWindow) glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
    }

	public String getEngineVersion() {
		// TODO Auto-generated method stub
		return engineVersion;
	}

	public void setWindowWidth(int newWidth) {
		// TODO Auto-generated method stub
		this.windowWidth = newWidth;
	}
	

	public void setWindowHeight(int newHeight) {
		// TODO Auto-generated method stub
		this.windowHeight = newHeight;
	}
}