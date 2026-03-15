package vn.pmgteam.yanase.editor;

import static org.lwjgl.opengl.GL11.*;
import org.joml.Matrix4f;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.node.CameraNode;
import vn.pmgteam.yanase.node.Object3D;

public class EditorView extends Engine {

    public EditorView() {
        super();
        this.isSplashFinished = true; // Bỏ qua splash để test nhanh
    }
    
    /**
     * Phương thức hỗ trợ để thêm nhanh node từ UI Editor vào Engine
     */
    public void addNodeToScene(Object3D newNode) {
        if (sceneRoot != null) {
            // appendChild là phương thức của org.w3c.dom.Node mà Object3D kế thừa
            sceneRoot.appendChild(newNode); 
            System.out.println("Yanase Editor: Đã thêm thành công node: " + newNode.getNodeName());
        } else {
            System.err.println("Yanase Editor: sceneRoot chưa được khởi tạo!");
        }
    }

    // Phương thức này giúp Editor "lái" Engine mà không cần luồng run() riêng
    public void renderFrame() {
        // 1. Xóa buffer khung hình hiện tại
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 2. Vẽ bầu trời và môi trường
        renderSky(); 

        // 3. Cập nhật ma trận camera và transform
        // Lưu ý: Đảm bảo mainCamera không null
        if (getEngine().sceneRoot != null) {
            sceneRoot.updateTransform(new Matrix4f());
            
            // 4. Chạy logic loop (ví dụ: xoay node test)
            onLoop();
            
            // 5. Vẽ toàn bộ cây Node
            renderRecursive(sceneRoot);
        }

        // 6. Vẽ UI hỗ trợ nếu cần
        renderCrosshair();
    }

    @Override
    public void onInit() {
        // Giữ nguyên logic khởi tạo testCube của bạn
        System.out.println("Yanase Editor: Đã sẵn sàng nạp Node.");
    }

    @Override
    public void onLoop() {
        // Test nhanh: Xoay toàn bộ Scene Root để kiểm tra loop
        // sceneRoot.rotation.y += 0.5f;
    }

    @Override
    public void onCleanup() {}
}