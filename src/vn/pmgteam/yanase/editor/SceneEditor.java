package vn.pmgteam.yanase.editor;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;
import org.joml.Vector3f;
import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.node.subnodes.CameraMode;
import vn.pmgteam.yanase.node.subnodes.GridNode;
import vn.pmgteam.yanase.node.subnodes.GroupNode;
import vn.pmgteam.yanase.render.RenderSystem;
import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.gui.node.Checkbox2D;
import vn.pmgteam.yanase.gui.node.Label2D;
import vn.pmgteam.yanase.gui.node.Panel2D;
import vn.pmgteam.yanase.util.Raycaster; // Giả định bạn đã tạo class Raycaster như đã thảo luận

public class SceneEditor {
    private boolean enabled = true;
    private Object3D selectedNode = null;
    private Object3D hoveredNode = null; 
    private BaseNode editorGui;
    private GridNode grid; 
    
    private double lastMouseX, lastMouseY;
    private float rotationSpeed = 0.15f;
    private float moveSpeed = 0.05f;
    private boolean isFirstClick = true;

    public void init() {
        editorGui = new GroupNode("Editor_UI");
        
        Panel2D sideBar = new Panel2D("SidePanel", 0, 0, 150, Engine.getEngine().getWindowHeight());
        editorGui.appendChild(sideBar);

        // --- Checkbox cũ ---
        Checkbox2D gridCbx = new Checkbox2D("Cbx_Grid", "Show Grid", 15, 20, 20);
        gridCbx.setChecked(true);
        gridCbx.setOnToggle(() -> this.toggle());
        sideBar.appendChild(gridCbx);

        // --- TEST LABEL2D TRONG PANEL ---
        // Đặt Label nằm dưới Checkbox (tọa độ y khoảng 50-60)
        Label2D debugLabel = new Label2D("Lbl_DebugInfo", "System: Stable");
        debugLabel.position.set(15, 60); // Lấy từ Object2D.position (JOML)
        sideBar.appendChild(debugLabel);

        // Logic test: Cập nhật nội dung Label dựa trên RAM
        if (getRAMUsage() > 0.85f) {
            debugLabel.setTextContent("RAM CRITICAL!");
            // Bạn có thể thêm logic đổi màu color ở đây nếu Label2D hỗ trợ
        }

        // --- Khởi tạo Grid ---
        int safeSize = (getRAMUsage() > 0.85f) ? 10 : 20;
        grid = new GridNode("EditorGrid", safeSize, 1.0f);
        Engine.getEngine().getSceneRoot().appendChild(grid);
        Engine.getEngine().getMainCamera().setMode(CameraMode.PLAYER);
    }

    private float getRAMUsage() {
        Runtime runtime = Runtime.getRuntime();
        return (float) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
    }

    public void update(long window, float deltaTime) {
        if (!enabled) return;

        handleInput(window);

        // --- HỆ THỐNG RAYCASTING ---
        var cam = Engine.getEngine().getMainCamera();
        if (cam != null) {
            Vector3f origin = cam.getPosition();
            Vector3f direction = cam.getForwardVector();
            
            // Duyệt đệ quy trực tiếp từ sceneRoot để tìm object bị nhắm vào
            hoveredNode = Raycaster.findSelectedObject(origin, direction, Engine.getEngine().getSceneRoot());

            // Click chuột trái để chọn vật thể (Select)
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            	if (hoveredNode != null) {
            	    // Thay vì this.selectedNode = hoveredNode;
            	    Engine.getEngine().setSelectedNode(hoveredNode); 
            	}
            }
        }
    }

    private void handleInput(long window) {
        float dt = Engine.getEngine().getDeltaTime();
        float currentMoveSpeed = moveSpeed * dt * 100.0f;

        // Giữ chuột phải để xoay Camera và di chuyển (Freecam Mode)
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            if (isFirstClick) {
                double[] curX = new double[1], curY = new double[1];
                glfwGetCursorPos(window, curX, curY);
                lastMouseX = curX[0];
                lastMouseY = curY[0];
                isFirstClick = false;
            }

            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            double[] curX = new double[1], curY = new double[1];
            glfwGetCursorPos(window, curX, curY);

            float dx = (float) (curX[0] - lastMouseX);
            float dy = (float) (curY[0] - lastMouseY);

            var cam = Engine.getEngine().getMainCamera();
            if (cam != null) {
                cam.rotation.x += dy * rotationSpeed;
                cam.rotation.y += dx * rotationSpeed;
                cam.rotation.x = Math.max(-89.0f, Math.min(89.0f, cam.rotation.x));

                float yaw = (float) Math.toRadians(cam.rotation.y);
                if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
                    cam.position.x += Math.sin(yaw) * currentMoveSpeed;
                    cam.position.z -= Math.cos(yaw) * currentMoveSpeed;
                }
                if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
                    cam.position.x -= Math.sin(yaw) * currentMoveSpeed;
                    cam.position.z += Math.cos(yaw) * currentMoveSpeed;
                }
                if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
                    cam.position.x -= Math.cos(yaw) * currentMoveSpeed;
                    cam.position.z -= Math.sin(yaw) * currentMoveSpeed;
                }
                if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
                    cam.position.x += Math.cos(yaw) * currentMoveSpeed;
                    cam.position.z += Math.sin(yaw) * currentMoveSpeed;
                }
                if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) cam.position.y += currentMoveSpeed;
                if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) cam.position.y -= currentMoveSpeed;
            }

            lastMouseX = curX[0];
            lastMouseY = curY[0];
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            isFirstClick = true;
        }
    }

    public void render() {
        if (!enabled) return;

        // 1. Kích hoạt ánh sáng mặt trời cho môi trường 3D
        RenderSystem.setupSunLighting();

        // Lấy node đang chọn từ Engine để vẽ Gizmo
        Object3D selected = (Object3D) Engine.getEngine().getSelectedNode();

        // 2. Vẽ Gizmo (Tắt Lighting để Gizmo giữ màu thuần khiết, không bị bóng tối làm mờ)
        if (selected != null) {
            RenderSystem.disableLighting(); 
            renderGizmo(selected);
            RenderSystem.enableLighting();
        }
        
        // 3. Vẽ Hover Point
        if (hoveredNode != null && hoveredNode != selected) {
            RenderSystem.disableLighting();
            renderHoverPoint(hoveredNode);
            RenderSystem.enableLighting();
        }

        // 4. Quan trọng: Tắt Lighting sau khi vẽ xong 3D để không làm hỏng màu sắc của UI 2D
        RenderSystem.disableLighting();
    }

    private void renderGizmo(Object3D node) {
        float x = node.position.x, y = node.position.y, z = node.position.z;
        float size = 1.5f;

        RenderSystem.disableDepthTest(); // Đảm bảo Gizmo luôn hiện lên trên vật thể
        glLineWidth(3.0f);
        glBegin(GL_LINES);
            RenderSystem.color4f(1, 0, 0, 1); glVertex3f(x, y, z); glVertex3f(x + size, y, z);
            RenderSystem.color4f(0, 1, 0, 1); glVertex3f(x, y, z); glVertex3f(x, y + size, z);
            RenderSystem.color4f(0, 0, 1, 1); glVertex3f(x, y, z); glVertex3f(x, y, z + size);
        glEnd();
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1, 1, 1, 1);
    }

    private void renderHoverPoint(Object3D node) {
        glPointSize(8.0f);
        glBegin(GL_POINTS);
            RenderSystem.color4f(1, 1, 0, 1); // Màu vàng cho hover
            glVertex3f(node.position.x, node.position.y, node.position.z);
        glEnd();
        RenderSystem.color4f(1, 1, 1, 1);
    }

    public void toggle() { 
        this.enabled = !this.enabled; 
        if (grid != null) grid.setVisible(enabled);
    }

    public BaseNode getEditorGui() { return editorGui; }
    public Object3D getSelectedNode() { return selectedNode; }
}