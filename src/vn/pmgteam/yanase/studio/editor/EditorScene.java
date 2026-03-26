package vn.pmgteam.yanase.studio.editor;

import static org.lwjgl.glfw.GLFW.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.DoubleBuffer;
import java.nio.file.*;
import java.util.*;

import org.lwjgl.system.MemoryStack;

import vn.pmgteam.yanase.base.Engine;
import vn.pmgteam.yanase.node.*;
import vn.pmgteam.yanase.node.subnodes.*;
import vn.pmgteam.yanase.scene.BaseScene;
import vn.pmgteam.yanase.studio.project.ProjectManager;
import vn.pmgteam.yanase.util.TextureManager;

/**
 * EditorScene — Scene mặc định của Studio.
 * Chứa camera, grid, sceneContent.
 * KHÔNG chứa UI (MainStudio tự lo NanoVG UI).
 */
public class EditorScene extends BaseScene {

    private CameraNode editorCamera;
    private GroupNode  gridGroup;
    private GroupNode  sceneContent;

    // Freecam
    private double  lastMouseX, lastMouseY;
    private boolean isFirstClick  = true;
    private boolean isRightMoving = false;
    private float   moveSpeed     = 0.05f;
    private float   rotationSpeed = 0.15f;

    // Scene list quét được từ project/src
    private final List<SceneEntry> detectedScenes = new ArrayList<>();

    public static class SceneEntry {
        public final String className;   // vd: mygame.GameScene
        public final String simpleName;  // vd: GameScene
        public final Path   sourceFile;  // vd: src/mygame/GameScene.java
        public SceneEntry(String className, String simpleName, Path src) {
            this.className  = className;
            this.simpleName = simpleName;
            this.sourceFile = src;
        }
    }

    public EditorScene() {
        super("Yanase_Editor_Scene");
    }

    // ----------------------------------------------------------------
    // Getters
    // ----------------------------------------------------------------

    public GroupNode   getSceneContent()   { return sceneContent; }
    public CameraNode  getEditorCamera()   { return editorCamera; }
    public List<SceneEntry> getDetectedScenes() { return Collections.unmodifiableList(detectedScenes); }

    // ----------------------------------------------------------------
    // Init
    // ----------------------------------------------------------------

    @Override
    public void init() {
        editorCamera = new CameraNode("Main_Editor_Camera");
        editorCamera.setMode(CameraMode.EDITOR);
        editorCamera.position.set(0, 5, 10);
        editorCamera.rotation.set(20, 0, 0);

        gridGroup    = new GroupNode("Editor_Helpers");
        sceneContent = new GroupNode("Scene_Root");

        createEditorGrid();

        rootNode.appendChild(editorCamera);
        rootNode.appendChild(gridGroup);
        rootNode.appendChild(sceneContent);

        // Quét scene từ project
        scanProjectScenes();
    }

    // ----------------------------------------------------------------
    // Update — chỉ freecam, không có UI logic
    // ----------------------------------------------------------------

    @Override
    public void update(long window, float deltaTime) {
        boolean rightClickHeld =
            glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        if (rightClickHeld) {
            if (!isRightMoving) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                isRightMoving = true;
                isFirstClick  = true;
            }
            handleFreecam(window, deltaTime);
        } else {
            if (isRightMoving) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                isRightMoving = false;
            }
        }

        rootNode.updateTransform(null);
    }

    // ----------------------------------------------------------------
    // Freecam
    // ----------------------------------------------------------------

    private void handleFreecam(long window, float dt) {
        double curX, curY;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1), y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            curX = x.get(0); curY = y.get(0);
        }

        if (isFirstClick) {
            lastMouseX = curX; lastMouseY = curY;
            isFirstClick = false;
            return;
        }

        float dx = (float)(curX - lastMouseX);
        float dy = (float)(curY - lastMouseY);

        editorCamera.rotation.x += dy * rotationSpeed;
        editorCamera.rotation.y += dx * rotationSpeed;
        editorCamera.rotation.x  = Math.max(-89f, Math.min(89f, editorCamera.rotation.x));

        updateMovement(window, dt);

        lastMouseX = curX; lastMouseY = curY;
    }

    private void updateMovement(long window, float dt) {
        float yaw   = (float) Math.toRadians(editorCamera.rotation.y);
        float speed = moveSpeed * dt * 100.0f;

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            editorCamera.position.x += Math.sin(yaw) * speed;
            editorCamera.position.z -= Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            editorCamera.position.x -= Math.sin(yaw) * speed;
            editorCamera.position.z += Math.cos(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            editorCamera.position.x -= Math.cos(yaw) * speed;
            editorCamera.position.z -= Math.sin(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            editorCamera.position.x += Math.cos(yaw) * speed;
            editorCamera.position.z += Math.sin(yaw) * speed;
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE)      == GLFW_PRESS) editorCamera.position.y += speed;
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) editorCamera.position.y -= speed;
    }

    // ----------------------------------------------------------------
    // Grid
    // ----------------------------------------------------------------

    private void createEditorGrid() {
        GridNode grid = new GridNode("Editor_Grid", 20, 1.0f);
        gridGroup.appendChild(grid);
    }

    // ----------------------------------------------------------------
    // Auto-scan project src để tìm class kế thừa BaseScene
    // ----------------------------------------------------------------

    /**
     * Quét toàn bộ src/ trong project đang mở.
     * Tìm file .java chứa "extends BaseScene" hoặc "extends Engine".
     * Kết quả lưu vào detectedScenes để SceneView có thể load.
     */
    public void scanProjectScenes() {
        detectedScenes.clear();
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) return;

        Path srcDir = Paths.get(pm.getProjectDir(), "src");
        if (!Files.exists(srcDir)) return;

        try {
            Files.walk(srcDir)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> {
                    try {
                        String content = Files.readString(file);
                        // Tìm class kế thừa BaseScene
                        if (content.contains("extends BaseScene")) {
                            String className  = resolveClassName(file, srcDir);
                            String simpleName = file.getFileName().toString().replace(".java", "");
                            if (className != null) {
                                detectedScenes.add(new SceneEntry(className, simpleName, file));
                                System.out.println("[EditorScene] Detected scene: " + className);
                            }
                        }
                    } catch (IOException ignored) {}
                });
        } catch (IOException e) {
            System.err.println("[EditorScene] scanProjectScenes error: " + e.getMessage());
        }

        System.out.println("[EditorScene] Found " + detectedScenes.size() + " scene(s) in project");
    }

    /**
     * Chuyển đường dẫn file → fully qualified class name.
     * vd: src/mygame/GameScene.java → mygame.GameScene
     */
    private String resolveClassName(Path javaFile, Path srcRoot) {
        try {
            Path relative = srcRoot.relativize(javaFile);
            String classPath = relative.toString()
                .replace("\\", "/")
                .replace("/", ".")
                .replace(".java", "");
            return classPath;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Load và instantiate một scene từ compiled classes.
     * Cần build project trước.
     * @param entry Scene entry từ detectedScenes
     * @return Instance BaseScene hoặc null nếu lỗi
     */
    public BaseScene instantiateScene(SceneEntry entry) {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) return null;

        Path classesDir = Paths.get(pm.getProjectDir(), "build", "classes");
        Path libDir     = Paths.get(pm.getProjectDir(), "lib");

        try {
            // Build URL array cho ClassLoader
            List<URL> urls = new ArrayList<>();
            urls.add(classesDir.toUri().toURL());

            if (Files.exists(libDir)) {
                Files.list(libDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(jar -> {
                        try { urls.add(jar.toUri().toURL()); }
                        catch (Exception ignored) {}
                    });
            }

            // Dùng URLClassLoader để load class từ build/classes
            URLClassLoader loader = new URLClassLoader(
                urls.toArray(new URL[0]),
                EditorScene.class.getClassLoader() // parent = studio classloader
            );

            Class<?> clazz = loader.loadClass(entry.className);

            // Kiểm tra có phải BaseScene không
            if (!BaseScene.class.isAssignableFrom(clazz)) {
                System.err.println("[EditorScene] " + entry.className + " is not a BaseScene");
                loader.close();
                return null;
            }

            // Instantiate (cần constructor no-arg)
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            BaseScene scene = (BaseScene) ctor.newInstance();

            System.out.println("[EditorScene] Instantiated: " + entry.className);
            return scene;

        } catch (NoSuchMethodException e) {
            System.err.println("[EditorScene] " + entry.className
                + " needs a no-arg constructor");
        } catch (Exception e) {
            System.err.println("[EditorScene] Failed to instantiate "
                + entry.className + ": " + e.getMessage());
        }
        return null;
    }

    // ----------------------------------------------------------------
    // Raycast (giữ để tương thích — SceneView cũng có raycast)
    // ----------------------------------------------------------------

    public Object3D castRayFromScreen(float sx, float sy, float vpW, float vpH) {
        float ndcX =  (2.0f * sx / vpW) - 1.0f;
        float ndcY = 1.0f - (2.0f * sy / vpH);

        org.joml.Matrix4f invProj = new org.joml.Matrix4f(editorCamera.projectionMatrix).invert();
        org.joml.Vector4f rayClip = new org.joml.Vector4f(ndcX, ndcY, -1.0f, 1.0f);
        org.joml.Vector4f rayEye  = invProj.transform(rayClip);
        rayEye.z = -1.0f; rayEye.w = 0.0f;

        org.joml.Matrix4f invView  = new org.joml.Matrix4f(editorCamera.viewMatrix).invert();
        org.joml.Vector4f rayWorld = invView.transform(rayEye);

        org.joml.Vector3f rayDir    = new org.joml.Vector3f(rayWorld.x, rayWorld.y, rayWorld.z).normalize();
        org.joml.Vector3f rayOrigin = new org.joml.Vector3f(editorCamera.position);

        return vn.pmgteam.yanase.util.Raycaster.findSelectedObject(rayOrigin, rayDir, sceneContent);
    }

    // ----------------------------------------------------------------
    // Cleanup
    // ----------------------------------------------------------------

    @Override
    public void cleanup() {
        if (rootNode instanceof BaseNode bn) bn.cleanup();
    }
}