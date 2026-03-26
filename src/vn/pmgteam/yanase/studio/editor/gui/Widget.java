package vn.pmgteam.yanase.studio.editor.gui;

import vn.pmgteam.yanase.node.Object2D;
import org.lwjgl.glfw.GLFW;

public abstract class Widget extends Object2D {
    public float width, height;
    protected boolean isHovered = false;

    public Widget(String name, float width, float height) {
        super(name);
        this.width = width;
        this.height = height;
    }

    // Kiểm tra chuột có nằm trong Widget không (Bounding Box)
    public boolean contains(float mx, float my) {
        return mx >= position.x && mx <= position.x + width &&
               my >= position.y && my <= position.y + height;
    }

    public abstract void onUpdate(long window, float mouseX, float mouseY);
}