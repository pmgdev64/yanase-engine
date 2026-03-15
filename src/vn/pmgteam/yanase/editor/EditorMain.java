package vn.pmgteam.yanase.editor;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.editor.SceneEditor;
import vn.pmgteam.yanase.settings.GameSettings;

public class EditorMain extends Engine {
    private SceneEditor editor;

    @Override
    public void onInit() {
        // 1. Khởi tạo Camera riêng cho Editor
        // CameraNode sẽ giúp bạn điều hướng quanh các model như Mahiro dễ dàng hơn
        vn.pmgteam.yanase.node.CameraNode editorCamera = new vn.pmgteam.yanase.node.CameraNode("EditorCamera");
        editorCamera.position.set(10, 10, 10); 
        editorCamera.rotation.set(30, -45, 0); // Nhìn chéo từ trên xuống Grid
        
        // Gán trực tiếp vào Engine để Renderer sử dụng ma trận của Camera này
        this.mainCamera = editorCamera; 

        // 2. Khởi tạo SceneEditor
        editor = new SceneEditor();
        editor.init();
        
        // 3. Kết nối UI của Editor vào hệ thống GUI của Engine
        // Điều này giúp hàm renderRecursive2D() tự động vẽ các nút bấm của Editor
        if (editor.getEditorGui() != null) {
            this.setSceneGui(editor.getEditorGui());
        }

        System.out.println("=========================================");
        System.out.println("| Status: Yanase Scene Editor Started   |");
        System.out.println("| Mode  : modcoderpack-redevelop        |");
        System.out.println("=========================================");
    }

    @Override
    public void onLoop() {
        // 1. Cập nhật logic Input (Di chuyển Cam, Xoay chuột...)
        // Truyền windowHandle và deltaTime để tính toán moveSpeed chuẩn xác
        editor.update(getWindowHandle(), getDeltaTime());

        // 2. Render phần 3D bổ trợ của Editor (Grid, Gizmo)
        // Lưu ý: sceneRoot vẫn được Engine tự động render, editor.render() chỉ vẽ thêm Grid/Gizmo
        editor.render();
        
        // 3. Đảm bảo GUI luôn được kết nối
        if (getSceneGui() == null && editor.getEditorGui() != null) {
            setSceneGui(editor.getEditorGui());
        }
    }

    @Override
    public void onCleanup() {
        System.out.println("[Editor] Closing...");
    }

    public static void main(String[] args) {
    	EditorMain main = new EditorMain();
    	GameSettings gameSettings = new GameSettings(main);
        main.startApplication();
    }
}