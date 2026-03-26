package vn.pmgteam.yanase.node.subnodes;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.node.Object3D;

public class CameraNode extends Object3D {

    private float fov = 45.0f;
    private float zNear = 0.1f;
    private float zFar = 1000.0f;
    private float sensitivity = 0.2f;
    private float walkSpeed = 5.0f;
    
    // Biến aspect mới để phục vụ Editor Viewport
    private float aspect = 1.0f; 

    // --- PHYSICS SYSTEM ---
    private Vector3f currentVelocity = new Vector3f(0, 0, 0);
    private float acceleration = 35.0f; 
    private float friction = 8.5f;     

    private float gravity = -18.0f;    
    private float jumpForce = 7.0f;    
    private float eyeHeight = 3.0f;    
    private boolean isGrounded = false; 

    private double lastX, lastY;
    private boolean firstMouse = true;

    public Matrix4f projectionMatrix = new Matrix4f();
    public Matrix4f viewMatrix = new Matrix4f();

    public CameraNode(String name) {
        super(name);
    }

    private CameraMode mode = CameraMode.EDITOR;

    public CameraMode getMode() { return mode; }
    public void setMode(CameraMode mode) { 
        this.mode = mode; 
        if (mode == CameraMode.EDITOR) {
            firstMouse = true;
        }
    }

    // --- SETTER CHO ASPECT (Sửa lỗi undefined) ---
    public void setAspect(float aspect) {
        this.aspect = aspect;
    }

    // --- CÁC PHƯƠNG THỨC LẤY VECTOR HƯỚNG ---
    public Vector3f getForwardVector() {
        Vector3f forward = new Vector3f(0, 0, -1);
        forward.rotateX((float) Math.toRadians(-rotation.x));
        forward.rotateY((float) Math.toRadians(-rotation.y));
        return forward;
    }

    public Vector3f getRightVector() {
        Vector3f right = new Vector3f(1, 0, 0);
        right.rotateX((float) Math.toRadians(-rotation.x));
        right.rotateY((float) Math.toRadians(-rotation.y));
        return right;
    }

    public Vector3f getUpVector() {
        Vector3f up = new Vector3f(0, 1, 0);
        up.rotateX((float) Math.toRadians(-rotation.x));
        up.rotateY((float) Math.toRadians(-rotation.y));
        return up;
    }

    public Vector3f getPosition() {
        return new Vector3f(position.x, position.y, position.z);
    }

    // --- LOGIC CẬP NHẬT ---
    public void update(long window, float deltaTime) {
        if (Engine.getEngine().getGameSettings().isShowSettingsMenu()) {
            currentVelocity.set(0, 0, 0); 
            return; 
        }
        
        if (mode == CameraMode.EDITOR) {
            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                applyInputAcceleration(window, deltaTime);
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                firstMouse = true;
                applyFriction(deltaTime); 
            }
        } else if (mode == CameraMode.PLAYER) {
            if (!Engine.getEngine().getGameSettings().isShowSettingsMenu()) {
                if (glfwGetInputMode(window, GLFW_CURSOR) != GLFW_CURSOR_DISABLED) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                }
                applyInputAcceleration(window, deltaTime);
            } else {
                if (glfwGetInputMode(window, GLFW_CURSOR) != GLFW_CURSOR_NORMAL) {
                    glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
                currentVelocity.set(0, 0, 0);
            }
        }

        if (mode == CameraMode.PLAYER && !isGrounded) {
            currentVelocity.y += gravity * deltaTime;
        }

        position.x += currentVelocity.x * deltaTime;
        position.y += currentVelocity.y * deltaTime;
        position.z += currentVelocity.z * deltaTime;

        if (mode == CameraMode.PLAYER) {
            if (position.y <= eyeHeight) {
                position.y = eyeHeight;
                currentVelocity.y = 0;
                isGrounded = true;
            } else {
                isGrounded = false;
            }
        }

        if (currentVelocity.length() < 0.005f) currentVelocity.set(0);
    }

    private void applyInputAcceleration(long window, float deltaTime) {
        float yawRad = (float) Math.toRadians(rotation.y);
        Vector3f wishDir = new Vector3f(0, 0, 0);
        boolean isMoving = false;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) { wishDir.x += Math.sin(yawRad); wishDir.z -= Math.cos(yawRad); isMoving = true; }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) { wishDir.x -= Math.sin(yawRad); wishDir.z += Math.cos(yawRad); isMoving = true; }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) { wishDir.x -= Math.cos(yawRad); wishDir.z -= Math.sin(yawRad); isMoving = true; }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) { wishDir.x += Math.cos(yawRad); wishDir.z += Math.sin(yawRad); isMoving = true; }

        if (wishDir.length() > 0) wishDir.normalize();

        float targetVX = wishDir.x * walkSpeed;
        float targetVZ = wishDir.z * walkSpeed;

        currentVelocity.x += (targetVX - currentVelocity.x) * acceleration * deltaTime;
        currentVelocity.z += (targetVZ - currentVelocity.z) * acceleration * deltaTime;

        if (mode == CameraMode.EDITOR) {
            float targetVY = 0;
            if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) { targetVY = walkSpeed; isMoving = true; }
            else if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) { targetVY = -walkSpeed; isMoving = true; }
            currentVelocity.y += (targetVY - currentVelocity.y) * acceleration * deltaTime;
        } else {
            if (isGrounded && glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
                currentVelocity.y = jumpForce;
                isGrounded = false;
                isMoving = true;
            }
        }

        if (!isMoving) applyFriction(deltaTime);
    }

    private void applyFriction(float deltaTime) {
        currentVelocity.x -= currentVelocity.x * friction * deltaTime;
        currentVelocity.z -= currentVelocity.z * friction * deltaTime;
        if (mode == CameraMode.EDITOR || isGrounded) {
            currentVelocity.y -= currentVelocity.y * friction * deltaTime;
        }
    }

    public void setupControls(long windowHandle) {
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (Engine.getEngine().getGameSettings().isShowSettingsMenu()) {
                lastX = xpos; lastY = ypos;
                return; 
            }
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

    public void setFirstMouse(boolean firstMouse) { this.firstMouse = firstMouse; }
    public Vector3f getCurrentVelocity() { return currentVelocity; }

    public void updateRotation(float dx, float dy) {
        this.rotation.y += dx * sensitivity;
        this.rotation.x += dy * sensitivity; 
        if (this.rotation.x > 89.0f)  this.rotation.x = 89.0f;
        if (this.rotation.x < -89.0f) this.rotation.x = -89.0f;
    }

    public void applyProjection() {
        float currentAspect = this.aspect;
        if (currentAspect <= 0) {
            int w = Engine.getEngine().getWindowWidth();
            int h = Engine.getEngine().getWindowHeight();
            currentAspect = (h == 0) ? 1.0f : (float) w / (float) h;
        }

        // JOML matrix cho raycast
        projectionMatrix.identity().perspective(
            (float) Math.toRadians(fov), currentAspect, zNear, zFar);

        // OpenGL fixed pipeline — FIX: fov/2 trước rồi mới toRadians
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float ymax = zNear * (float) Math.tan(Math.toRadians(fov / 2.0f));
        float xmax = ymax * currentAspect;
        // System.out.println("[CAM] fov=" + fov + " ymax=" + ymax + " xmax=" + xmax + " aspect=" + currentAspect);
        glFrustum(-xmax, xmax, -ymax, ymax, zNear, zFar);
        glMatrixMode(GL_MODELVIEW);
    }

    public void applyViewMatrix() {
        viewMatrix.identity()
                  .rotateX((float) Math.toRadians(rotation.x))
                  .rotateY((float) Math.toRadians(rotation.y))
                  .rotateZ((float) Math.toRadians(rotation.z))
                  .translate(-position.x, -position.y, -position.z);
        glLoadIdentity();
        glRotatef(rotation.x, 1, 0, 0); 
        glRotatef(rotation.y, 0, 1, 0); 
        glRotatef(rotation.z, 0, 0, 1); 
        glTranslatef(-position.x, -position.y, -position.z);
    }

    public void applyMatrices() { applyProjection(); applyViewMatrix(); }
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