package vn.pmgteam.yanase.node;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.w3c.dom.Node;

import vn.pmgteam.yanase.shader.ShaderSystem;

public abstract class Object3D extends BaseNode {
    // Thuộc tính tương đương Inspector trong Godot
    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f rotation = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);
    protected boolean selected = false;
    
    public ShaderSystem shader; // Thêm biến này
    public Matrix4f worldMatrix = new Matrix4f();

    public Object3D(String name) { super(name); }
    
    public void setSelected(boolean s) { this.selected = s; }

    public void updateTransform(Matrix4f parentMatrix) {
        worldMatrix.identity()
                   .translate(position)
                   .rotateXYZ(rotation.x, rotation.y, rotation.z)
                   .scale(scale);
        
        if (parentMatrix != null) {
            parentMatrix.mul(worldMatrix, worldMatrix);
        }

        // Đệ quy cập nhật các con (W3C Node traversal)
        for (Node child : children) {
            if (child instanceof Object3D) {
                ((Object3D) child).updateTransform(this.worldMatrix);
            }
        }
    }

    public abstract void render(); // Lớp con sẽ dùng LWJGL để vẽ
}