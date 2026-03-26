package vn.pmgteam.yanase.scene;

import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.node.subnodes.GroupNode;

public abstract class BaseScene {
    protected String name;
    // Layer 3D: Dùng Object3D để xử lý ma trận không gian
    protected Object3D rootNode;
    // Layer 2D: Dùng BaseNode để quản lý Button2D, Label2D, v.v.
    protected BaseNode sceneGui;

    public BaseScene(String name) {
        this.name = name;
        this.rootNode = new GroupNode(name + "_SceneRoot");
        // Khởi tạo một GroupNode làm gốc cho toàn bộ UI của Scene
        this.sceneGui = new GroupNode(name + "_SceneGUI");
    }

    // Các giai đoạn vòng đời của một Scene
    public abstract void init();
    public abstract void update(long window, float deltaTime);
    public abstract void cleanup();

    public Object3D getRootNode() {
        return rootNode;
    }

    /**
     * Getter cho Engine có thể truy cập và render GUI
     */
    public BaseNode getSceneGui() {
        return sceneGui;
    }
    
    public String getName() {
        return name;
    }
}