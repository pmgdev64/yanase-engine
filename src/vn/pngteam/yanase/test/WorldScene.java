package vn.pngteam.yanase.test;

import vn.pngteam.yanase.scene.BaseScene;
import vn.pngteam.yanase.node.*;
import vn.pngteam.yanase.base.Engine;
import vn.pngteam.yanase.util.ModelLoader; // Thêm bộ nạp model

public class WorldScene extends BaseScene {
    
    private CameraNode player;
    private GroupNode environment;
    private GroupNode entities;

    public WorldScene() {
        super("WorldScene");
    }

    @Override
    public void init() {
        // 1. Khởi tạo Camera (Người chơi)
        player = new CameraNode("FPS_Player");
        player.position.set(0, 2, 10);
        player.setupControls(Engine.getEngine().getWindowHandle());
        
        // 2. Khởi tạo các Group
        environment = new GroupNode("Environment");
        entities = new GroupNode("Entities");

        // 3. Xây dựng thế giới
        //environment.appendChild(new GridNode("GroundGrid", 500, 1.0f));
        
        // --- NẠP MODEL .GLB TẠI ĐÂY ---
        // Ví dụ: Nạp một nhân vật chính hoặc NPC
        /*Model3D mainCharacter = ModelLoader.loadGLB("mahiro_oyama_-_onii-chan_wa_oshimai.glb");
        if (mainCharacter != null) {
            mainCharacter.position.set(0, 0, 0);
            mainCharacter.scale.set(1.0f, 1.0f, 1.0f);
            // FIX: Xoay -90° quanh X để đứng thẳng (glTF Y-up)
            mainCharacter.rotation.set((float) Math.toRadians(-90), 0, 0);
            entities.appendChild(mainCharacter);
        }*/

        // Thêm một vài cây cối bằng MeshObject3D cũ
        for(int i = -5; i < 5; i++) {
            MeshObject3D tree = new MeshObject3D("Tree_" + i);
            tree.position.set(i * 4, 0, -10);
            environment.appendChild(tree);
        }

        // 4. Append vào rootNode
        rootNode.appendChild(player);
        rootNode.appendChild(environment);
        rootNode.appendChild(entities);
    }

    @Override
    public void update(long window, float deltaTime) {
        player.update(window, deltaTime);

        // Logic thực thể: Ví dụ làm cho model xoay nhẹ
        // if (entities.hasChildNodes()) {
        //     Object3D model = (Object3D) entities.getChildNodes().item(0);
        //     model.rotation.rotateY(deltaTime * 0.5f);
        // }

        rootNode.updateTransform(null);
    }

    @Override
    public void cleanup() {
        // Nhớ dọn dẹp các VBO để tránh rò rỉ bộ nhớ (Memory Leak)
        System.out.println("Cleaning up VBOs...");
        // Duyệt cây node và gọi cleanup() cho các Model3D
    }

    public CameraNode getPlayer() {
        return player;
    }
}