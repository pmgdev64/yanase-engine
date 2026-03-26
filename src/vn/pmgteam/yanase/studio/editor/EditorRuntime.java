package vn.pmgteam.yanase.studio.editor;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.node.*;
import vn.pmgteam.yanase.node.subnodes.CameraNode;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.studio.editor.render.GizmoRenderer;

import org.joml.Matrix4f;

public class EditorRuntime extends Engine {

    private EditorScene editorScene;
    private final SceneView sceneView = new SceneView(); // ← field mới
    private final GizmoRenderer gizmo = new GizmoRenderer();

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public GizmoRenderer getGizmo()        { return gizmo; }
    public EditorScene   getEditorScene()  { return editorScene; }
    public SceneView     getSceneView()    { return sceneView; }

    public CameraNode getEditorCamera() {
        return editorScene != null ? editorScene.getEditorCamera() : null;
    }

    public Object3D getSceneContent() {
        if (sceneView.getActiveScene() instanceof EditorScene ed)
            return ed.getSceneContent();
        BaseScene active = sceneView.getActiveScene();
        return active != null ? active.getRootNode() : null;
    }

    // ----------------------------------------------------------------
    // Engine lifecycle
    // ----------------------------------------------------------------

    @Override
    public void onInit() {
        editorScene = new EditorScene();
        sceneView.loadScene(editorScene);

        this.setMainScene(editorScene);
        this.sceneRoot  = editorScene.getRootNode();
        this.mainCamera = editorScene.getEditorCamera();

        System.out.println("[Editor] Yanase Editor Scene Loaded.");
    }

    @Override
    public void onLoop() {
        sceneView.update(getWindowHandle(), getDeltaTime());
    }

    @Override
    public void onCleanup() {
        sceneView.cleanup();
        System.out.println("[Editor] Resources Released.");
    }

    // ----------------------------------------------------------------
    // Scene switching
    // ----------------------------------------------------------------

    public void loadScene(BaseScene scene) {
        sceneView.loadScene(scene);
        this.sceneRoot = scene.getRootNode();

        if (scene instanceof EditorScene ed) {
            this.mainCamera = ed.getEditorCamera();
        } else {
            CameraNode cam = findCameraInScene(scene.getRootNode());
            this.mainCamera = cam != null ? cam : editorScene.getEditorCamera();
        }
    }

    public void loadEditorScene() {
        loadScene(editorScene);
    }

    // ----------------------------------------------------------------
    // Render
    // ----------------------------------------------------------------

    public void renderToStudio(int w, int h) {
        Object3D selected = Engine.getEngine().getSelectedNode();
        sceneView.setSelectedNode(selected);

        CameraNode cam = (sceneView.getActiveScene() instanceof EditorScene ed)
            ? ed.getEditorCamera() : mainCamera;

        if (cam != null) {
            cam.setAspect((float) w / (float) h);
            cam.applyMatrices();
            this.mainCamera = cam;
        }

        sceneView.render(cam, w, h);

        // Gizmo chỉ render khi đang ở EditorScene
        if (sceneView.getActiveScene() instanceof EditorScene && selected != null) {
            gizmo.render(selected, cam);
        }
    }

    // ----------------------------------------------------------------
    // Node management
    // ----------------------------------------------------------------

    public void addNodeToScene(Object3D node) {
        if (sceneView.getActiveScene() instanceof EditorScene ed)
            ed.getSceneContent().appendChild(node);
        else if (sceneView.getActiveScene() != null)
            sceneView.getActiveScene().getRootNode().appendChild(node);
    }

    public void removeNodeFromScene(Object3D node) {
        if (node != null && node.getParent() != null)
            node.getParent().getChildren().remove(node);
    }

    // ----------------------------------------------------------------
    // Raycast
    // ----------------------------------------------------------------

    public Object3D castRayFromScreen(float screenX, float screenY,
                                      float vpW, float vpH) {
        CameraNode cam = (sceneView.getActiveScene() instanceof EditorScene ed)
            ? ed.getEditorCamera() : mainCamera;
        return sceneView.castRayFromScreen(screenX, screenY, vpW, vpH, cam);
    }

    // ----------------------------------------------------------------
    // Camera focus
    // ----------------------------------------------------------------

    public void focusCameraOnNode(Object3D node) {
        if (node == null) return;
        CameraNode cam = getEditorCamera();
        if (cam == null) return;
        cam.position.set(node.position.x, node.position.y + 2, node.position.z + 5);
        cam.rotation.set(15, 180, 0);
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private CameraNode findCameraInScene(Object3D node) {
        if (node instanceof CameraNode cam) return cam;
        for (BaseNode child : node.getChildren()) {
            if (child instanceof Object3D obj) {
                CameraNode found = findCameraInScene(obj);
                if (found != null) return found;
            }
        }
        return null;
    }
}