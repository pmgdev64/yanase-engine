package vn.pmgteam.yanase.studio.editor.node;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

public class NodeEditor {
    // Định nghĩa cấu trúc 1 Node đơn giản
	// Trong file NodeEditorTest.java
	public static class Pin {
	    public String name;
	    public boolean isOutput;
	    public float relativeY; // Vị trí Y tương đối so với đỉnh Node

	    public Pin(String name, boolean isOutput, float relativeY) {
	        this.name = name;
	        this.isOutput = isOutput;
	        this.relativeY = relativeY;
	    }
	}

	public static class Node {
	    public String id, title;
	    public float x, y, w, h;
	    public int color;
	    public List<Pin> pins = new ArrayList<>();

	    public Node(String id, String title, float x, float y, int color) {
	        this.id = id; 
	        this.title = title;
	        this.x = x; 
	        this.y = y;
	        this.w = 160; 
	        this.h = 60; // Gán tạm 60px để có vùng click (Hitbox)
	        this.color = color;
	    }
	    
	    public void addPin(String name, boolean isOutput) {
	        float yPos = 35 + (pins.size() * 20); 
	        pins.add(new Pin(name, isOutput, yPos));
	        
	        // Cập nhật chiều cao dãn theo Pin
	        // Phải đảm bảo h luôn lớn hơn yPos để chuột có thể click trúng vùng chứa Pin
	        this.h = Math.max(this.h, yPos + 20); 
	    }
	}

    // Định nghĩa đường nối giữa các Node
    public static class Connection {
        public String fromId, toId;
        public Connection(String f, String t) { this.fromId = f; this.toId = t; }
    }

    public List<BaseNode> nodes = new ArrayList<>();
    public List<Connection> connections = new ArrayList<>();

    public NodeEditor() {
        // Khởi tạo các Node chuyên dụng
        nodes.add(new EventTickNode("1", 100, 100));
        nodes.add(new SetPositionNode("2", 400, 150));
        nodes.add(new GetVelocityNode("3", 100, 250));
        
        // Kết nối vẫn dựa trên ID như cũ
        connections.add(new Connection("1", "2"));
        connections.add(new Connection("3", "2"));
    }
    
    
    public static NVGColor rgba(int r, int g, int b, int a) {
        NVGColor color = NVGColor.create();
        color.r(r / 255f); color.g(g / 255f);
        color.b(b / 255f); color.a(a / 255f);
        return color;
    }
    
    // Node sự kiện: Màu đỏ
    public static class EventTickNode extends BaseNode {
        public EventTickNode(String id, float x, float y) {
            super(id, "Event Tick", x, y, NodeEditor.rgba(204, 85, 85, 255));
            addPin("Out", true);
        }
        @Override public void onExecute() { /* Logic tick */ }
		@Override
		public void drawInternal(long nvg, float x, float y, float w, float h) {
			// TODO Auto-generated method stub
			
		}
    }

    public static class SetPositionNode extends BaseNode {
    	public String valX = "0", valY = "0";

        public SetPositionNode(String id, float x, float y) {
            super(id, "Set Position", x, y, NodeEditor.rgba(85, 170, 85, 255));
            addPin("In", false);
            this.h = 100;
        }

        @Override
        public void onBackspace() {
            if ("X".equals(focusedField) && valX.length() > 0) {
                valX = valX.substring(0, valX.length() - 1);
            } else if ("Y".equals(focusedField) && valY.length() > 0) {
                valY = valY.substring(0, valY.length() - 1);
            }
        }

        @Override
        public void drawInternal(long nvg, float x, float y, float w, float h) {
            // Highlight ô đang focus bằng cách kiểm tra focusedField
            drawInputBox(nvg, "X:", valX, x + 10, y + 40, w - 20, "X".equals(focusedField));
            drawInputBox(nvg, "Y:", valY, x + 10, y + 65, w - 20, "Y".equals(focusedField));
        }

        private void drawInputBox(long nvg, String label, String value, float ix, float iy, float iw, boolean isFocused) {
            // Label
            NanoVG.nvgFontSize(nvg, 12.0f);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(200, 200, 200, 255));
            NanoVG.nvgText(nvg, ix, iy + 13, label);

            // Box Background
            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgRoundedRect(nvg, ix + 20, iy, iw - 20, 18, 3);
            // Nếu đang focus thì vẽ nền sáng hơn hoặc có viền
            NanoVG.nvgFillColor(nvg, isFocused ? NodeEditor.rgba(40, 45, 50, 255) : NodeEditor.rgba(20, 20, 25, 255));
            NanoVG.nvgFill(nvg);
            
            if (isFocused) {
                NanoVG.nvgStrokeColor(nvg, NodeEditor.rgba(100, 150, 255, 255));
                NanoVG.nvgStrokeWidth(nvg, 1.0f);
                NanoVG.nvgStroke(nvg);
            }

            // Text value
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(255, 255, 255, 255));
            NanoVG.nvgText(nvg, ix + 25, iy + 13, value + (isFocused ? "|" : "")); // Thêm con trỏ giả
        }

        @Override public void onExecute() { 
            // Khi thực thi thì mới ép kiểu String -> float
            try { 
                float x = Float.parseFloat(valX); 
                float y = Float.parseFloat(valY);
                // logic set position...
            } catch (Exception e) {}
        }
    }
    public static class MathNode extends BaseNode {
        public MathNode(String id, String title, float x, float y, NVGColor color) {
			super(id, title, x, y, color);
			// TODO Auto-generated constructor stub
		}

		public float sliderValue = 0.5f; // 0.0f -> 1.0f

        @Override
        public void drawInternal(long nvg, float x, float y, float w, float h) {
            float bx = x + 10, by = y + 45, bw = w - 20, bh = 8;
            
            // Vẽ rãnh slider
            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgRoundedRect(nvg, bx, by, bw, bh, 4);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(30, 30, 35, 255));
            NanoVG.nvgFill(nvg);

            // Vẽ phần đã kéo (Fill)
            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgRoundedRect(nvg, bx, by, bw * sliderValue, bh, 4);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(85, 170, 85, 255));
            NanoVG.nvgFill(nvg);

            // Vẽ nút tròn (Knob)
            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgCircle(nvg, bx + (bw * sliderValue), by + bh/2, 6);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(200, 200, 200, 255));
            NanoVG.nvgFill(nvg);
        }

		@Override
		public void onExecute() {
			// TODO Auto-generated method stub
			
		}
    }
    
    public static class GetVelocityNode extends BaseNode {
        private float currentVelocity = 120.5f;

        public GetVelocityNode(String id, float x, float y) {
            super(id, "Get Velocity", x, y, NodeEditor.rgba(85, 85, 204, 255));
            addPin("Value", true);
            this.h = 70;
        }

        @Override public void onExecute() { /* Cập nhật currentVelocity */ }

        @Override
        public void drawInternal(long nvg, float x, float y, float w, float h) {
            // Vẽ một dải màu hiển thị giá trị thực tế
            float barWidth = w - 20;
            float valueWidth = (currentVelocity / 200.0f) * barWidth; // Giả sử max là 200

            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgRoundedRect(nvg, x + 10, y + 45, barWidth, 10, 5);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(50, 50, 60, 255));
            NanoVG.nvgFill(nvg);

            NanoVG.nvgBeginPath(nvg);
            NanoVG.nvgRoundedRect(nvg, x + 10, y + 45, Math.min(valueWidth, barWidth), 10, 5);
            NanoVG.nvgFillColor(nvg, NodeEditor.rgba(100, 200, 255, 255));
            NanoVG.nvgFill(nvg);
            
            // Hiển thị số
            NanoVG.nvgFontSize(nvg, 10.0f);
            NanoVG.nvgText(nvg, x + 10, y + 40, "Current: " + currentVelocity + " m/s");
        }
    }
}