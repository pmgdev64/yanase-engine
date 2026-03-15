package vn.pmgteam.yanase.node;

import org.joml.Vector2f;

public abstract class Object2D extends BaseNode {
    public Vector2f position = new Vector2f(0, 0);
    public float rotation = 0;
    public Vector2f scale = new Vector2f(1, 1);
    public int zIndex = 0;

    public Object2D(String name) { super(name); }
    
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    
    public abstract void render2D();
}