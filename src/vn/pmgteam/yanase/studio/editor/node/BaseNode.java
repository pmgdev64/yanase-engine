package vn.pmgteam.yanase.studio.editor.node;

import org.lwjgl.nanovg.NVGColor;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseNode {
    public String id, title;
    public float x, y, w, h;
    public NVGColor color;
    public List<NodeEditor.Pin> pins = new ArrayList<>();
    
    public boolean isDraggingSlider = false;
    public String focusedField = null; // "X" hoặc "Y" hoặc null

    public BaseNode(String id, String title, float x, float y, NVGColor color) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
        this.w = 160;
        this.h = 40; 
        this.color = color;
    }
    
    public void onBackspace() {}

    public abstract void onExecute();

    // Để trống nếu không có nội dung đặc biệt, hoặc override để vẽ thêm UI
    public abstract void drawInternal(long nvg, float x, float y, float w, float h);

    public void addPin(String name, boolean isOutput) {
        float yPos = 35 + (pins.size() * 22); 
        pins.add(new NodeEditor.Pin(name, isOutput, yPos));
        this.h = Math.max(this.h, yPos + 15);
    }
}