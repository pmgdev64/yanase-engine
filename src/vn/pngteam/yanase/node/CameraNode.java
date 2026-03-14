package vn.pngteam.yanase.node;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import org.joml.Matrix4f; // Thêm thư viện JOML
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import vn.pngteam.yanase.base.Engine;

public class CameraNode extends Object3D {

    private float fov = 45.0f;
    private float zNear = 0.1f;
    private float zFar = 1000.0f;
    private float sensitivity = 0.2f;
    private float walkSpeed = 5.0f;

    private double lastX, lastY;
    private boolean firstMouse = true;

    // --- CÁC BIẾN MA TRẬN CHO SHADER ---
    public Matrix4f projectionMatrix = new Matrix4f();
    public Matrix4f viewMatrix = new Matrix4f();

    public CameraNode(String name) {
        super(name);
    }

    public void setupControls(long windowHandle) {
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (!Engine.getEngine().isSplashFinished()) return;
            if (firstMouse) {
                lastX = xpos; lastY = ypos;
                firstMouse = false; return;
            }
            if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
                updateRotation((float) (xpos - lastX), (float) (ypos - lastY));
            }
            lastX = xpos; lastY = ypos;
        });
    }

    public void update(long window, float deltaTime) {
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            firstMouse = true;
        }
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS 
            && Engine.getEngine().isSplashFinished()) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
        if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            handleFullInput(window, deltaTime);
        }
    }

    public void handleFullInput(long window, float deltaTime) {
        float speed = walkSpeed * deltaTime;
        float yawRad = (float) Math.toRadians(rotation.y);
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            position.x += Math.sin(yawRad) * speed;
            position.z -= Math.cos(yawRad) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            position.x -= Math.sin(yawRad) * speed;
            position.z += Math.cos(yawRad) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            position.x -= (float)Math.cos(yawRad) * speed;
            position.z -= (float)Math.sin(yawRad) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            position.x += (float)Math.cos(yawRad) * speed;
            position.z += (float)Math.sin(yawRad) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) position.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) position.y -= speed;
    }

    public void updateRotation(float dx, float dy) {
        this.rotation.y += dx * sensitivity;
        this.rotation.x += dy * sensitivity; 
        if (this.rotation.x > 89.0f)  this.rotation.x = 89.0f;
        if (this.rotation.x < -89.0f) this.rotation.x = -89.0f;
    }

    // --- CẬP NHẬT MA TRẬN CHO CẢ LEGACY VÀ MODERN (Shader) ---

    public void applyProjection() {
        int w = Engine.getEngine().getWindowWidth();
        int h = Engine.getEngine().getWindowHeight();
        if (h == 0) h = 1; // Tránh lỗi chia cho 0
        float aspect = (float) w / (float) h;

        // 1. Cập nhật cho Shader - PHẢI GỌI identity() trước
        projectionMatrix.identity().perspective((float) Math.toRadians(fov), aspect, zNear, zFar);

        // 2. Legacy support (glFrustum) - Giữ nguyên như cũ của bạn
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float ymax = (float) (zNear * Math.tan(fov * Math.PI / 360.0));
        float xmax = ymax * aspect;
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
        glMatrixMode(GL_MODELVIEW);
    }

    public void applyViewMatrix() {
        // 1. Cập nhật cho Shader
        // QUAN TRỌNG: Thứ tự nhân trong JOML: Rotate X -> Rotate Y -> Translate
        viewMatrix.identity()
                  .rotateX((float) Math.toRadians(rotation.x))
                  .rotateY((float) Math.toRadians(rotation.y))
                  .rotateZ((float) Math.toRadians(rotation.z))
                  .translate(-position.x, -position.y, -position.z);

        // 2. Legacy support (glRotatef/glTranslatef)
        glLoadIdentity();
        glRotatef(rotation.x, 1, 0, 0); 
        glRotatef(rotation.y, 0, 1, 0); 
        glRotatef(rotation.z, 0, 0, 1); 
        glTranslatef(-position.x, -position.y, -position.z);
    }

    public void applyMatrices() {
        applyProjection();
        applyViewMatrix();
    }

    @Override public void render() {}

    // --- DOM STUBS ---
    @Override public void setNodeValue(String v) throws DOMException {}
    @Override public Node getPreviousSibling() { return null; }
    @Override public Node insertBefore(Node n, Node r) throws DOMException { return null; }
    @Override public Node replaceChild(Node n, Node o) throws DOMException { return null; }
    @Override public Node removeChild(Node o) throws DOMException { return null; }
    @Override public Node cloneNode(boolean d) { return null; }
    @Override public void normalize() {}
    @Override public boolean isSupported(String f, String v) { return false; }
    @Override public String getNamespaceURI() { return null; }
    @Override public String getPrefix() { return null; }
    @Override public void setPrefix(String p) throws DOMException {}
    @Override public String getLocalName() { return null; }
    @Override public boolean hasAttributes() { return false; }
    @Override public String getBaseURI() { return null; }
    @Override public short compareDocumentPosition(Node o) throws DOMException { return 0; }
    @Override public String getTextContent() throws DOMException { return null; }
    @Override public void setTextContent(String t) throws DOMException {}
    @Override public boolean isSameNode(Node o) { return false; }
    @Override public String lookupPrefix(String n) { return null; }
    @Override public boolean isDefaultNamespace(String n) { return false; }
    @Override public String lookupNamespaceURI(String p) { return null; }
    @Override public boolean isEqualNode(Node a) { return false; }
    @Override public Object getFeature(String f, String v) { return null; }
    @Override public Object setUserData(String k, Object d, UserDataHandler h) { return null; }
    @Override public Object getUserData(String k) { return null; }
}