package vn.pngteam.yanase.gui;

import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

public class ImGuiLayer {
    private final long windowHandle;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public ImGuiLayer(long windowHandle) {
        this.windowHandle = windowHandle;
    }
    
 // Thêm phương thức này vào
    public ImGuiImplGlfw getImGuiGlfw() {
        return imGuiGlfw;
    }

    public void init() {
        ImGui.createContext();
        // Giữ nguyên true để nó init mặc định, nhưng chúng ta sẽ forward thêm event
        imGuiGlfw.init(windowHandle, true); 
        imGuiGl3.init("#version 130");
    }

    public void startFrame() {
    	// Rất quan trọng: Phải gọi Backend NewFrame TRƯỚC ImGui NewFrame
        imGuiGlfw.newFrame();
        imGuiGl3.newFrame(); // Thêm dòng này nếu chưa có
        ImGui.newFrame();    }

    public void endFrame() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    // Trong ImGuiLayer.java (phần cleanup/destroy)
    public void destroy() {
        imGuiGl3.shutdown(); // Thay vì dispose()
        imGuiGlfw.shutdown(); // Thay vì dispose()
        ImGui.destroyContext();
    }
}