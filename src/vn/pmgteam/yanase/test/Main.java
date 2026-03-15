package vn.pmgteam.yanase.test;

import vn.pmgteam.yanase.base.Engine;

public class Main extends Engine {
    
    private WorldScene world;

    @Override
    public void onInit() {
        // Khởi tạo thế giới
        world = new WorldScene();
        world.init();
        
        // Chỉ việc set scene chính, Engine sẽ tự lấy Camera và rootNode từ đây
        this.setMainScene(world); 
    }

    @Override
    public void onLoop() {
        // Main chỉ còn lo cập nhật logic tùy biến nếu cần
        world.update(windowHandle, getDeltaTime());
    }

    @Override
    public void onCleanup() {
        if (world != null) world.cleanup();
    }
    
    public static void main(String[] args) {
        Main testApp = new Main();
        testApp.getGameSettings().setWindowTitle("Yanase Engine - modcoderpack-redevelop");
        testApp.getGameSettings().setVSyncEnable(false); // Uncap FPS để test hiệu năng VBO
        testApp.startApplication();
    }
}