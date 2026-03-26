package vn.pmgteam.yanase.test;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.gui.node.Button2D;
import vn.pmgteam.yanase.node.*;
import vn.pmgteam.yanase.node.subnodes.Box3D;
import vn.pmgteam.yanase.node.subnodes.CameraMode;
import vn.pmgteam.yanase.node.subnodes.CameraNode;
import vn.pmgteam.yanase.node.subnodes.GroupNode;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.util.TextureManager;

public class WorldScene extends BaseScene {
    
    private CameraNode player;
    private GroupNode environment;
    private GroupNode entities;

    public WorldScene() {
        super("WorldScene");
    }

    @Override
    public void init() {
        // 1. Khởi tạo Camera
        player = new CameraNode("FPS_Player");
        player.position.set(0, 5, 10); 
        player.setMode(CameraMode.PLAYER);
        player.setupControls(Engine.getEngine().getWindowHandle());
        
        // 2. Khởi tạo các Group
        environment = new GroupNode("Environment");
        entities = new GroupNode("Entities");

        // 3. Xây dựng thế giới bằng các Block 1x1
        int grassTex = TextureManager.loadTexture("/assets/textures/grass.png");
        int worldSize = 20; // Tạo vùng 20x20 blocks (tổng 400 blocks)

        for (int x = -worldSize / 2; x < worldSize / 2; x++) {
            for (int z = -worldSize / 2; z < worldSize / 2; z++) {
                // Tạo block tại vị trí (x, y, z)
                Box3D grassBlock = new Box3D("Grass_" + x + "_" + z);
                grassBlock.setTexture(grassTex);
                
                // Đặt y = 0 để mặt trên của block nằm ngay mặt đất
                // Lưu ý: Nếu Box3D của bạn vẽ từ tâm, hãy set y = -0.5f
                grassBlock.position.set(x, 0, z); 
                grassBlock.scale.set(1.0f, 1.0f, 1.0f); // Kích thước chuẩn 1 đơn vị
                
                environment.appendChild(grassBlock);
            }
        }

        // 3.5 Tạo hàng cây (Đặt trên các block đã có)
        for(int i = -5; i < 5; i++) {
            Box3D tree = new Box3D("Tree_" + i);
            // Đặt cây tại y=1.5f để thân cây (cao 3.0) đứng trên mặt đất (y=0)
            tree.position.set(i * 4, 2.0f, -10); 
            tree.scale.set(0.5f, 3.0f, 0.5f);
            environment.appendChild(tree);
        }

        // 4. Thiết lập UI
        setupUI();

        // 5. Append vào rootNode
        rootNode.appendChild(player);
        rootNode.appendChild(environment);
        rootNode.appendChild(entities);
    }

    private void setupUI() {
        float buttonWidth = 200.0f;
        float buttonHeight = 50.0f;
        float margin = 20.0f;
        // Đặt nút ở vị trí dễ nhìn theo style UI hiện đại
        float safeX = 50.0f; 
        float safeY = Engine.getEngine().getWindowHeight() - buttonHeight - margin; 

        Button2D testButton = new Button2D("Btn_Test", buttonWidth, buttonHeight);
        testButton.position.set(safeX, safeY); 
        testButton.setColor(0.2f, 0.6f, 1.0f); 

        if (sceneGui != null) {
            sceneGui.appendChild(testButton);
        }
    }

    @Override
    public void update(long window, float deltaTime) {
        // Cập nhật Player (Trọng lực, Di chuyển, Nhảy)
        player.update(window, deltaTime);

        // Cập nhật ma trận toàn cầu cho tất cả các node trong cây phân cấp
        rootNode.updateTransform(null);
    }
    
    @Override
    public void cleanup() {
        if (rootNode instanceof BaseNode) {
            ((BaseNode) rootNode).cleanup();
        }
        System.out.println("Yanase Engine: WorldScene resources released.");
    }

    public CameraNode getPlayer() {
        return player;
    }
}