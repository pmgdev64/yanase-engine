package vn.pmgteam.yanase.test;

import org.w3c.dom.Node;
import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.gui.node.Button2D;
import vn.pmgteam.yanase.node.*;
import vn.pmgteam.yanase.scene.BaseScene;

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
        player.position.set(0, 2, 10);
        player.setupControls(Engine.getEngine().getWindowHandle());
        
        // 2. Khởi tạo các Group
        environment = new GroupNode("Environment");
        entities = new GroupNode("Entities");

        // 3. Xây dựng thế giới (Cây cối)
        for(int i = -5; i < 5; i++) {
            Box3D tree = new Box3D("Tree_" + i);
            tree.position.set(i * 4, 0, -10);
            environment.appendChild(tree);
        }

        // 4. Thiết lập UI (Blue Archive Style)
        setupUI();

        // 5. Append vào rootNode (Chỉ append 1 lần để tránh loạn cây phân cấp)
        rootNode.appendChild(player);
        rootNode.appendChild(environment);
        rootNode.appendChild(entities);
    }

    private void setupUI() {
        float buttonWidth = 200.0f;
        float buttonHeight = 50.0f;
        float margin = 20.0f;
        float safeX = 350.0f; 
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
        // QUAN TRỌNG: player.update phải xử lý cả vận tốc trôi (Friction) 
        // ngay cả khi không có input nào được nhấn.
        player.update(window, deltaTime);

        // Cập nhật ma trận toàn cầu cho tất cả các node
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