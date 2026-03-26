package vn.pmgteam.yanase.studio.editor.render;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import static org.lwjgl.nanovg.NanoVG.*;

import vn.pmgteam.yanase.studio.editor.node.BaseNode;
import vn.pmgteam.yanase.studio.editor.node.NodeEditor;

public class NodeEditorRenderer {

    public void render(long nvg, NodeEditor data, float offsetX, float offsetY, 
                      NodeEditor.Pin activePin, BaseNode activeNode, float mx, float my) {
        
        // 1. Vẽ các đường kết nối Bezier (Nằm dưới Node)
        for (NodeEditor.Connection conn : data.connections) {
            BaseNode from = findNode(data, conn.fromId);
            BaseNode to = findNode(data, conn.toId);
            
            if (from != null && to != null) {
                // Tọa độ Output (Bên phải Node nguồn)
                float startX = from.x + from.w + offsetX;
                float startY = from.y + offsetY + (from.pins.isEmpty() ? 20 : from.pins.get(from.pins.size()-1).relativeY);

                // Tọa độ Input (Bên trái Node đích)
                float endX = to.x + offsetX;
                float endY = to.y + offsetY + (to.pins.isEmpty() ? 20 : to.pins.get(0).relativeY);

                drawBezierCurve(nvg, startX, startY, endX, endY);
            }
        }

        // Vẽ dây tạm thời khi người dùng đang kéo chuột từ một Pin
        if (activePin != null && activeNode != null) {
            float px = activePin.isOutput ? (activeNode.x + activeNode.w + offsetX) : (activeNode.x + offsetX);
            float py = activeNode.y + activePin.relativeY + offsetY;

            if (activePin.isOutput) {
                drawBezierCurve(nvg, px, py, mx, my);
            } else {
                drawBezierCurve(nvg, mx, my, px, py);
            }
        }

        // 2. Vẽ danh sách các Node
        for (BaseNode n : data.nodes) {
            drawNode(nvg, n, offsetX, offsetY);
        }
    }

    private void drawNode(long nvg, BaseNode n, float offsetX, float offsetY) {
        float x = n.x + offsetX;
        float y = n.y + offsetY;

        // --- 1. Vẽ Shadow (Đổ bóng) ---
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + 2, y + 2, n.w, n.h, 6);
        nvgFillColor(nvg, rgba(0, 0, 0, 120));
        nvgFill(nvg);

        // --- 2. Thân Node (Background) ---
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, n.w, n.h, 6);
        nvgFillColor(nvg, rgba(35, 35, 42, 255));
        nvgFill(nvg);

        // --- 3. Header (Tiêu đề Node) ---
        nvgBeginPath(nvg);
        // Header cao 26px, bo tròn 2 góc trên
        NanoVG.nvgRoundedRectVarying(nvg, x, y, n.w, 26, 6, 6, 0, 0);
        nvgFillColor(nvg, n.color);
        nvgFill(nvg);

        // --- 4. Chữ tiêu đề ---
        nvgFontSize(nvg, 14.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        
        // Shadow cho chữ
        nvgFillColor(nvg, rgba(0, 0, 0, 150));
        nvgText(nvg, x + 11, y + 14, n.title);
        // Chữ chính
        nvgFillColor(nvg, rgba(255, 255, 255, 255));
        nvgText(nvg, x + 10, y + 13, n.title);

        // --- 5. NỘI DUNG TÙY CHỈNH (Đa hình) ---
        // Gọi hàm drawInternal để các Node con tự vẽ logic UI của chúng
        n.drawInternal(nvg, x, y, n.w, n.h);

        // --- 6. Vẽ các Pin (Sockets) ---
        for (NodeEditor.Pin pin : n.pins) {
            float pinY = y + pin.relativeY;
            float socketX = pin.isOutput ? (x + n.w) : x;
            
            // Vẽ vòng tròn Pin
            nvgBeginPath(nvg);
            nvgCircle(nvg, socketX, pinY, 5);
            nvgFillColor(nvg, rgba(160, 160, 170, 255));
            nvgFill(nvg);
            
            // Stroke cho Pin (giúp nhìn rõ hơn trên nền tối)
            nvgStrokeColor(nvg, rgba(255, 255, 255, 100));
            nvgStrokeWidth(nvg, 1.0f);
            nvgStroke(nvg);

            // Vẽ tên Pin
            nvgFontSize(nvg, 11.0f);
            nvgFillColor(nvg, rgba(190, 190, 200, 255));
            if (pin.isOutput) {
                nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
                nvgText(nvg, x + n.w - 12, pinY, pin.name);
            } else {
                nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
                nvgText(nvg, x + 12, pinY, pin.name);
            }
        }
    }
    
    private void drawBezierCurve(long nvg, float x1, float y1, float x2, float y2) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x1, y1);

        float diffX = Math.abs(x2 - x1);
        float cpOffset = Math.max(diffX * 0.5f, 35.0f);

        nvgBezierTo(nvg, x1 + cpOffset, y1, x2 - cpOffset, y2, x2, y2);

        // Màu dây nối (Xám sáng, hơi trong suốt)
        nvgStrokeColor(nvg, rgba(200, 200, 210, 180));
        nvgStrokeWidth(nvg, 2.0f); 
        nvgStroke(nvg);
    }

    private BaseNode findNode(NodeEditor data, String id) {
        return data.nodes.stream()
                .filter(n -> n.id.equals(id))
                .findFirst()
                .orElse(null);
    }

    private NVGColor rgba(int r, int g, int b, int a) {
        NVGColor color = NVGColor.create();
        color.r(r / 255.0f);
        color.g(g / 255.0f);
        color.b(b / 255.0f);
        color.a(a / 255.0f);
        return color;
    }
}