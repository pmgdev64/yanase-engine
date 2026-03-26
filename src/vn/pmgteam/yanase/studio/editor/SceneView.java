package vn.pmgteam.yanase.studio.editor;

import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import vn.pmgteam.yanase.node.*;
import vn.pmgteam.yanase.node.subnodes.*;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.studio.editor.render.GizmoRenderer;
import vn.pmgteam.yanase.util.RenderUtils;

import static org.lwjgl.opengl.GL11.*;

/**
 * SceneView — renderer nhẹ, nhận bất kỳ BaseScene nào.
 * - Nếu scene chứa Object3D → render Perspective 3D
 * - Nếu scene chỉ có GUI/2D   → render Orthographic 2D
 *
 * SceneView không kế thừa Engine. Nó chỉ render thuần OpenGL.
 * EditorRuntime truyền camera + viewport cho nó.
 */
public class SceneView {

    // ----------------------------------------------------------------
    // View mode
    // ----------------------------------------------------------------
    public enum ViewMode { AUTO, PERSPECTIVE_3D, ORTHOGRAPHIC_2D }
    private ViewMode viewMode = ViewMode.AUTO;
    private ViewMode resolvedMode = ViewMode.PERSPECTIVE_3D;
    
    // Thêm field
    private boolean isPreviewMode = false; // true = đang preview game scene, không lock cursor

    public void setPreviewMode(boolean preview) { this.isPreviewMode = preview; }
    public boolean isPreviewMode() { return isPreviewMode; }

    // ----------------------------------------------------------------
    // State
    // ----------------------------------------------------------------
    private BaseScene activeScene;
    private final GizmoRenderer gizmo = new GizmoRenderer();
    private Object3D selectedNode;

    // Sky color
    private float skyR = 0.45f, skyG = 0.55f, skyB = 0.65f;

    // ----------------------------------------------------------------
    // Scene lifecycle
    // ----------------------------------------------------------------

    /**
     * Load một BaseScene vào SceneView.
     * Tự động cleanup scene cũ nếu có.
     */
    public void loadScene(BaseScene scene) {
        if (activeScene != null) activeScene.cleanup();
        activeScene = scene;
        activeScene.init();
        // Nếu không phải EditorScene → đây là preview, block cursor
        isPreviewMode = !(scene instanceof EditorScene);
        resolvedMode = resolveViewMode(scene);
        System.out.println("[SceneView] Loaded: " + scene.getClass().getSimpleName()
            + "  mode=" + resolvedMode
            + "  preview=" + isPreviewMode);
    }
    
    public void reloadScene() {
        if (activeScene == null) return;
        activeScene.cleanup();
        activeScene.init();
        resolvedMode = resolveViewMode(activeScene);
    }

    // ----------------------------------------------------------------
    // Update
    // ----------------------------------------------------------------

    public void update(long window, float deltaTime) {
        if (activeScene == null) return;

        if (isPreviewMode) {
            // Preview mode — KHÔNG cho scene tự lock cursor
            // Inject deltaTime nhưng skip input handling
            activeScene.update(window, deltaTime);
            // Force cursor về normal sau mỗi frame
            org.lwjgl.glfw.GLFW.glfwSetInputMode(window,
                org.lwjgl.glfw.GLFW.GLFW_CURSOR,
                org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL);
        } else {
            activeScene.update(window, deltaTime);
        }
    }

    // ----------------------------------------------------------------
    // Render
    // ----------------------------------------------------------------

    /**
     * Render scene vào viewport hiện tại.
     * Gọi sau glViewport() từ EditorRuntime.
     *
     * @param camera Camera dùng để render (editor camera hoặc game camera)
     * @param vpW    Viewport width
     * @param vpH    Viewport height
     */
    public void render(CameraNode camera, int vpW, int vpH) {
        if (activeScene == null) return;

        switch (resolvedMode) {
            case PERSPECTIVE_3D  -> render3D(camera, vpW, vpH);
            case ORTHOGRAPHIC_2D -> render2D(vpW, vpH);
            default              -> render3D(camera, vpW, vpH);
        }
    }

    // ----------------------------------------------------------------
    // 3D rendering
    // ----------------------------------------------------------------

    private void render3D(CameraNode camera, int vpW, int vpH) {
        // Sky
        glClearColor(skyR, skyG, skyB, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        if (camera == null) return;

        // Projection + View
        camera.setAspect((float) vpW / (float) vpH);
        camera.applyMatrices();

        // Render scene tree
        Object3D root = activeScene.getRootNode();
        if (root != null) {
            root.updateTransform(new Matrix4f());
            renderRecursive(root);
        }

        // Selected node highlight + gizmo
        if (selectedNode != null) {
            renderSelectionHighlight(selectedNode);
            gizmo.render(selectedNode, camera);
        }

        // Reset state
        glLineWidth(1.0f);
        glColor3f(1, 1, 1);
        glEnable(GL_DEPTH_TEST);
    }

    // ----------------------------------------------------------------
    // 2D rendering
    // ----------------------------------------------------------------

    private void render2D(int vpW, int vpH) {
        glClearColor(0.12f, 0.12f, 0.15f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);

        // Setup orthographic projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, vpW, vpH, 0, -1, 1); // Y-down để khớp với screen coords
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Render GUI nodes
        BaseNode gui = activeScene.getSceneGui();
        if (gui != null) {
            renderGuiRecursive(gui);
        }

        glEnable(GL_DEPTH_TEST);
    }

    // ----------------------------------------------------------------
    // Recursive renderers
    // ----------------------------------------------------------------

    private void renderRecursive(Object3D node) {
        if (node == null) return;

        // Skip nếu GroupNode (chỉ là container)
        if (!(node instanceof GroupNode)) {
            node.render();
        }

        for (BaseNode child : node.getChildren()) {
            if (child instanceof Object3D) {
                renderRecursive((Object3D) child);
            }
        }
    }

    private void renderGuiRecursive(BaseNode node) {
        if (node == null) return;
        // 2D nodes tự render nếu có method render()
        // (Button2D, Label2D, Panel2D đều có render() riêng)
        try {
            node.getClass().getMethod("render").invoke(node);
        } catch (Exception ignored) {
            // Node không có render() → bỏ qua
        }
        for (BaseNode child : node.getChildren()) {
            renderGuiRecursive(child);
        }
    }

    // ----------------------------------------------------------------
    // Selection highlight (wireframe vàng)
    // ----------------------------------------------------------------

    private void renderSelectionHighlight(Object3D node) {
        glPushMatrix();

        RenderUtils.matrixBuffer.clear();
        node.worldMatrix.get(RenderUtils.matrixBuffer);
        glMultMatrixf(RenderUtils.matrixBuffer);

        glDisable(GL_DEPTH_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glColor3f(1.0f, 0.8f, 0.0f);
        glLineWidth(2.0f);

        float s = 0.55f;
        glBegin(GL_QUADS);
        glVertex3f(-s,-s, s); glVertex3f( s,-s, s); glVertex3f( s, s, s); glVertex3f(-s, s, s);
        glVertex3f(-s,-s,-s); glVertex3f(-s, s,-s); glVertex3f( s, s,-s); glVertex3f( s,-s,-s);
        glVertex3f(-s, s,-s); glVertex3f(-s, s, s); glVertex3f( s, s, s); glVertex3f( s, s,-s);
        glVertex3f(-s,-s,-s); glVertex3f( s,-s,-s); glVertex3f( s,-s, s); glVertex3f(-s,-s, s);
        glVertex3f( s,-s,-s); glVertex3f( s, s,-s); glVertex3f( s, s, s); glVertex3f( s,-s, s);
        glVertex3f(-s,-s,-s); glVertex3f(-s,-s, s); glVertex3f(-s, s, s); glVertex3f(-s, s,-s);
        glEnd();

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_DEPTH_TEST);
        glLineWidth(1.0f);
        glColor3f(1, 1, 1);

        glPopMatrix();
    }

    // ----------------------------------------------------------------
    // Raycast
    // ----------------------------------------------------------------

    public Object3D castRayFromScreen(float screenX, float screenY,
                                      float vpW, float vpH,
                                      CameraNode camera) {
        if (activeScene == null || camera == null) return null;

        float ndcX =  (2.0f * screenX / vpW) - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY / vpH);

        org.joml.Matrix4f invProj = new org.joml.Matrix4f(camera.projectionMatrix).invert();
        org.joml.Vector4f rayClip = new org.joml.Vector4f(ndcX, ndcY, -1.0f, 1.0f);
        org.joml.Vector4f rayEye  = invProj.transform(rayClip);
        rayEye.z = -1.0f; rayEye.w = 0.0f;

        org.joml.Matrix4f invView = new org.joml.Matrix4f(camera.viewMatrix).invert();
        org.joml.Vector4f rayWorld = invView.transform(rayEye);

        org.joml.Vector3f rayDir    = new org.joml.Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize();
        org.joml.Vector3f rayOrigin = new org.joml.Vector3f(camera.position);

        // Chỉ raycast vào sceneContent nếu là EditorScene
        Object3D searchRoot = activeScene.getRootNode();
        if (activeScene instanceof EditorScene) {
            searchRoot = ((EditorScene) activeScene).getSceneContent();
        }

        return vn.pmgteam.yanase.util.Raycaster.findSelectedObject(
            rayOrigin, rayDir, searchRoot);
    }

    // ----------------------------------------------------------------
    // View mode detection
    // ----------------------------------------------------------------

    private ViewMode resolveViewMode(BaseScene scene) {
        if (viewMode != ViewMode.AUTO) return viewMode;
        // Nếu scene chứa Object3D có children → 3D
        // Nếu rootNode rỗng nhưng sceneGui có content → 2D
        Object3D root = scene.getRootNode();
        if (root != null && !root.getChildren().isEmpty()) {
            return ViewMode.PERSPECTIVE_3D;
        }
        BaseNode gui = scene.getSceneGui();
        if (gui != null && !gui.getChildren().isEmpty()) {
            return ViewMode.ORTHOGRAPHIC_2D;
        }
        return ViewMode.PERSPECTIVE_3D; // default
    }

    // ----------------------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------------------

    public BaseScene getActiveScene()  { return activeScene; }
    public GizmoRenderer getGizmo()    { return gizmo; }
    public ViewMode getResolvedMode()  { return resolvedMode; }

    public void setViewMode(ViewMode m) {
        this.viewMode = m;
        if (activeScene != null) resolvedMode = resolveViewMode(activeScene);
    }

    public void setSelectedNode(Object3D n) { this.selectedNode = n; }
    public Object3D getSelectedNode()        { return selectedNode; }

    public void setSkyColor(float r, float g, float b) {
        skyR = r; skyG = g; skyB = b;
    }

    public void cleanup() {
        if (activeScene != null) activeScene.cleanup();
    }
}