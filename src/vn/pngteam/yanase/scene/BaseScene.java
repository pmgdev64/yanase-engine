package vn.pngteam.yanase.scene;

import vn.pngteam.yanase.node.GroupNode;
import vn.pngteam.yanase.node.Object3D;

public abstract class BaseScene {
    protected String name;
    // rootNode dùng GroupNode (Object3D) để có thể updateTransform và render đệ quy
    protected Object3D rootNode;

    public BaseScene(String name) {
        this.name = name;
        this.rootNode = new GroupNode(name + "_SceneRoot");
    }

    // Các giai đoạn vòng đời của một Scene
    public abstract void init();
    public abstract void update(long window, float deltaTime);
    public abstract void cleanup();

    public Object3D getRootNode() {
        return rootNode;
    }
    
    public String getName() {
        return name;
    }
}