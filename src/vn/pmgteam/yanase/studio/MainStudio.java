package vn.pmgteam.yanase.studio;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.nanovg.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.studio.build.BuildSystem;
import vn.pmgteam.yanase.studio.build.LibraryManager;
import vn.pmgteam.yanase.studio.editor.EditorRuntime;
import vn.pmgteam.yanase.studio.editor.EditorScene;
import vn.pmgteam.yanase.studio.editor.node.BaseNode;
import vn.pmgteam.yanase.studio.editor.node.NodeEditor;
import vn.pmgteam.yanase.studio.editor.node.NodeEditor.EventTickNode;
import vn.pmgteam.yanase.studio.editor.node.NodeEditor.SetPositionNode;
import vn.pmgteam.yanase.studio.editor.render.GizmoRenderer;
import vn.pmgteam.yanase.studio.project.ProjectManager;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import java.io.IOException;
import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class MainStudio {
    private long window;
    private long nvg;
    private int width, height;
    
    private EditorRuntime editorCore;
    
    private boolean isVisible = false;
    private int frameCount = 0;
    private final int WARMUP_FRAMES = 5;
    private double lastTitleUpdate = 0;
    private int fpsCounter = 0;
    
    // --- INSPECTOR RESIZE ---
    private float inspectorWidth = 380f;
    private final float INSPECTOR_MIN_WIDTH = 200f;
    private final float INSPECTOR_MAX_WIDTH = 600f;
    private boolean isDraggingInspector = false;
    
    private String projectName = null; 
    private String statusMessage = "";
    private double statusTimer = 0;

    private double mouseX, mouseY;
    private final NVGColor colorBuf = NVGColor.create();
    private FontMetrics menuMetrics;
    private final String[] menuItems = {"File", "Edit", "Project", "Build", "Help"};
    private final float MENU_HEIGHT = 30.0f;
    
    // --- CURSOR SYSTEM ---
    private int cursorLine = 0;
    private int cursorCol = 0;
    private double lastBlinkTime = 0;
    private boolean showCursor = true;
    private boolean mousePressedLastFrame = false;

    // --- TAB & CODE EDITOR SYSTEM ---
    private EditorTab currentTab = EditorTab.SCENE_3D;
    private final float TAB_HEIGHT = 35.0f;
    
    // --- IDE SYSTEM ---
    private enum EditorTab { SCENE_3D, CODE_EDITOR, CONSOLE, NODE_EDITOR }
    private final List<String> consoleLines = new ArrayList<>();
    private float consoleScrollY = 0;
    private boolean consoleAutoScroll = true;
    private Process runningProcess = null;
    
    // Đổi enum

    // Node Editor state
    private final vn.pmgteam.yanase.studio.editor.node.NodeEditor nodeEditorData
        = new vn.pmgteam.yanase.studio.editor.node.NodeEditor();
    private final vn.pmgteam.yanase.studio.editor.render.NodeEditorRenderer nodeEditorRenderer
        = new vn.pmgteam.yanase.studio.editor.render.NodeEditorRenderer();

    private float   neOffsetX = 0, neOffsetY = 0;
    private boolean neIsPanning = false;
    private float   nePanStartX, nePanStartY;

    // Thêm 2 dòng này để sửa lỗi của bạn:
    private float neDragStartX = 0; 
    private float neDragStartY = 0;

    // File Explorer
    private float fileExplorerScrollY = 0;
    private List<Path> projectFiles = new ArrayList<>();
    private Path selectedFile = null;
    
    // --- UNDO SYSTEM ---
    private final java.util.ArrayDeque<String> undoStack = new java.util.ArrayDeque<>();
    private static final int MAX_UNDO = 100;

    // --- SELECTION SYSTEM ---
    private int selectStartLine = -1, selectStartCol = -1;
    private int selectEndLine = -1, selectEndCol = -1;
    private boolean hasSelection = false;
    private boolean shiftHeld = false;
    
    private float lastViewportX, lastViewportY, lastViewportW, lastViewportH;
    
    private boolean crosshairSelectRequested = false;
    private double lastMouseXForDrag = 0;

    // --- SCROLL ---
    private long scrollCallback;
    
   // Thêm vào đầu class
    private final Set<Path> expandedDirs = new HashSet<>();
    private List<FileTreeEntry> fileTree = new ArrayList<>();

    private static class FileTreeEntry {
        Path path;
        int depth;
        boolean isDir;
        FileTreeEntry(Path path, int depth, boolean isDir) {
            this.path = path; this.depth = depth; this.isDir = isDir;
        }
    }
    
 // Thêm vào đầu class
    private int editingVec3Field = -1;  // -1 = không edit, 0-8 = field index (3 node * 3 axis)
    private int editingNodeIndex = -1;
    private String editingBuffer = "";
    private String editingLabel = ""; // "pos", "rot", "scale"
    
 // --- MENU STATE ---
    private int activeMenuIndex = -1; // -1 = không mở menu nào
    private static final String[][] MENU_ITEMS_SUB = {
        // File
        {"New Project", "Open Project...", "Save Project", "Save As...", "─────────", "Settings", "─────────", "Exit"},
        // Edit
        {"Undo", "Redo", "─────────", "Cut", "Copy", "Paste", "─────────", "Select All"},
        // Project
        {"Project Settings", "Scan Scenes", "─────────", "Open Project Folder", "─────────", "Reload"},
        // Build
        {"Build", "Run", "Build & Run", "─────────", "Pack Game", "─────────", "Clean Build"},
        // Help
        {"Documentation", "About Yanase Studio", "─────────", "Report Bug"}
    };

    private StringBuilder codeContent = new StringBuilder(
    	    "package vn.pmgteam.yanase.script;\n" +
    	    "\n" +
    	    "import vn.pmgteam.yanase.base.Script;\n" +
    	    "\n" +
    	    "public class GameScript extends Script {\n" +
    	    "\n" +
    	    "    @Override\n" +
    	    "    public void onStart() {\n" +
    	    "        // Chạy 1 lần khi scene khởi động\n" +
    	    "    }\n" +
    	    "\n" +
    	    "    @Override\n" +
    	    "    public void onUpdate(float deltaTime) {\n" +
    	    "        // Chạy mỗi frame\n" +
    	    "    }\n" +
    	    "\n" +
    	    "    @Override\n" +
    	    "    public void onDestroy() {\n" +
    	    "        // Chạy khi object bị xóa\n" +
    	    "    }\n" +
    	    "}"
    	);
    
    private float codeScrollY = 0; 
    
    // --- SCENE TREE ---
    private final java.util.List<SceneNodeEntry> sceneNodes = new java.util.ArrayList<>();
    private int selectedNodeIndex = -1;
    private boolean showAddNodeMenu = false;
    private float addMenuX, addMenuY;
    private float treeScrollY = 0;
    private boolean showContextMenu = false;
    private float ctxMenuX, ctxMenuY;
    private int ctxTargetIndex = -1;
    private String projectPath;
    
    private boolean showAbout = false;
    private boolean showSettings = false;
    private boolean showGameSettings = false;
    
    private final NVGColor color = NVGColor.create();

    // Tọa độ và kích thước cửa sổ
    private float winX = 100, winY = 100, winW = 450, winH = 350;
    
    private float neScale = 1.0f;
    // Thêm vào đầu class
    private boolean inspectorResetPressed = false;
    private boolean inspectorFocusPressed = false;
    private boolean inspectorDuplicatePressed = false;
    private boolean inspectorDeletePressed = false;
    private boolean inspectorVisiblePressed = false;
    private int draggingVec3Node = -1;   // selectedNodeIndex khi đang drag
    private int draggingVec3Axis = -1;   // 0=x, 1=y, 2=z
    private String draggingVec3Label = ""; // "pos","rot","scale"
    private int sideTab = 0;
    
    private BaseNode neDraggingNode = null;
    private BaseNode neActivePinNode = null;
    private NodeEditor.Pin neActivePin = null;
    
    private int windowWidth = 1280, windowHeight = 720;
    
    private String focusField = ""; // "width" hoặc "height"
    private String resWidth = "1280";
    private String resHeight = "720";
    
    private GizmoRenderer.GizmoMode gizmoMode = GizmoRenderer.GizmoMode.TRANSLATE;

    private static class SceneNodeEntry {
        String name, type;
        int depth;
        Object3D node;
        boolean visible = true; // ← THÊM
        SceneNodeEntry(String n, String t, int d, Object3D node) {
            this.name = n; this.type = t; this.depth = d; this.node = node;
        }
    }

    // Constructor nhận path từ ProjectHub
    public MainStudio(String projectPath) {
        this.projectPath = projectPath;
    }

    // Constructor mặc định (giữ để tương thích)
    public MainStudio() {
        this(null);
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!glfwInit()) throw new IllegalStateException("Không thể khởi tạo GLFW");

        glfwDefaultWindowHints(); // ← THÊM: reset tất cả hints về mặc định
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE); // ← THÊM
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        glfwWindowHint(GLFW_SAMPLES, 8); // ← THÊM: 4x MSAA

        window = glfwCreateWindow(windowWidth, windowHeight, "Yanase Studio v0.0.1", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Lỗi tạo cửa sổ");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        // Trong ProjectHub.run(), sau GL.createCapabilities():
        vn.pmgteam.yanase.util.TextureManager.setWindowIcons(window,
            "/assets/icons/icon_16x16.png",
            "/assets/icons/icon_32x32.png");

        glEnable(GL_MULTISAMPLE); // ← THÊM: Kích hoạt MSAA

        editorCore = new EditorRuntime();
        editorCore.initForEditor(1280, 720, window);
        editorCore.onInit();
        
        // Trong init(), sau editorCore.onInit():
        if (ProjectManager.getInstance().isProjectLoaded()) {
            String content = ProjectManager.getInstance().getActiveSourceContent();
            if (content != null && !content.isEmpty()) {
                codeContent = new StringBuilder(content);
                cursorLine = 0; cursorCol = 0; // ← THÊM
            }
            projectName = ProjectManager.getInstance().getProjectName();
        }
        
        syncSceneTree();
        
        // Load project từ path nếu có
        if (projectPath != null) {
            if (!ProjectManager.getInstance().isProjectLoaded()) {
                ProjectManager.getInstance().loadProject(projectPath);
            }
            String content = ProjectManager.getInstance().getActiveSourceContent();
            if (content != null && !content.isEmpty()) {
                codeContent = new StringBuilder(content);
                cursorLine = 0; cursorCol = 0; // ← THÊM
            }
            projectName = ProjectManager.getInstance().getProjectName();
        }

        nvg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        setupTextFont();
        setupKeyCallbacks();

        Canvas c = new Canvas();
        menuMetrics = c.getFontMetrics(new Font("Segoe UI", Font.PLAIN, 14));

        glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        glfwSwapInterval(1); 
    }

    private void setupKeyCallbacks() {
    	glfwSetCharCallback(window, (win, codepoint) -> {
    		// --- TRƯỜNG HỢP 1: NODE EDITOR ---
    	    if (currentTab == EditorTab.NODE_EDITOR) {
    	        String inputChar = new String(Character.toChars(codepoint));
    	        
    	        // Tìm xem có node nào đang được focus không
    	        for (BaseNode n : nodeEditorData.nodes) {
    	            if (n.focusedField != null) {
    	                // Xử lý riêng cho SetPositionNode hoặc các node có TextField
    	                if (n instanceof SetPositionNode) {
    	                    SetPositionNode sn = (SetPositionNode) n;
    	                    
    	                    // Chỉ cho phép nhập số, dấu chấm, dấu trừ
    	                    if (inputChar.matches("[0-9.\\-]")) {
    	                        if ("X".equals(sn.focusedField)) {
    	                            sn.valX += inputChar;
    	                        } else if ("Y".equals(sn.focusedField)) {
    	                            sn.valY += inputChar;
    	                        }
    	                        
    	                        // Tự động lưu mỗi khi thay đổi giá trị (theo yêu cầu của bạn)
    	                        // autoSave(); 
    	                    }
    	                }
    	                // Nếu đã tìm thấy node focus và xử lý xong, thoát callback
    	                return; 
    	            }
    	        }
    	    }
    	    if (currentTab == EditorTab.CODE_EDITOR) {
    	        pushUndo();

    	        if (hasSelection) deleteSelection();
    	        
    	        // Nhập số vào Vec3 field
    	        if (editingVec3Field >= 0) {
    	            String ch = new String(Character.toChars(codepoint));
    	            // Chỉ cho phép số, dấu trừ, dấu chấm
    	            if (ch.matches("[0-9.\\-]")) {
    	                editingBuffer += ch;
    	            }
    	            return;
    	        }

    	        String[] lines = codeContent.toString().split("\n", -1);
    	        // ← THÊM: bounds check
    	        if (cursorLine < 0) cursorLine = 0;
    	        if (cursorLine >= lines.length) cursorLine = lines.length - 1;
    	        if (cursorCol < 0) cursorCol = 0;
    	        if (cursorCol > lines[cursorLine].length()) cursorCol = lines[cursorLine].length();
    	        String currentLine = lines[cursorLine];
    	        String inserted = new String(Character.toChars(codepoint));

    	        // ← THÊM: Skip closing char nếu cursor đang đứng trước đúng ký tự đó
    	        if (inserted.length() == 1 && isClosingChar(inserted.charAt(0))
    	                && cursorCol < currentLine.length()
    	                && currentLine.charAt(cursorCol) == inserted.charAt(0)) {
    	            cursorCol++;
    	            resetCursorBlink();
    	            return;
    	        }

    	        // Auto-close ngoặc
    	        String closing = getAutoClose(inserted);

    	        // Auto-dedent khi gõ '}'
    	        if (inserted.equals("}")) {
    	            String trimmed = currentLine.stripTrailing();
    	            if (trimmed.isEmpty() && currentLine.length() >= 4) {
    	                lines[cursorLine] = currentLine.substring(0,
    	                    Math.max(0, currentLine.length() - 4)) + "}";
    	                cursorCol = lines[cursorLine].length();
    	                updateCodeContent(lines);
    	                resetCursorBlink();
    	                clearSelection();
    	                return;
    	            }
    	        }

    	        lines[cursorLine] = currentLine.substring(0, cursorCol)
    	                          + inserted
    	                          + (closing != null ? closing : "")
    	                          + currentLine.substring(cursorCol);
    	        cursorCol += inserted.length();
    	        updateCodeContent(lines);
    	        resetCursorBlink();
    	        clearSelection();
    	    }
    	});

    	glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
    	    if (currentTab != EditorTab.CODE_EDITOR) return;
    	    // Vec3 field editing — hoạt động ở mọi tab
    	 // Gizmo mode switch — W/E/R
    	    if (action == GLFW_PRESS && currentTab == EditorTab.SCENE_3D) {
    	        if (key == GLFW_KEY_W) {
    	            gizmoMode = GizmoRenderer.GizmoMode.TRANSLATE;
    	            editorCore.getGizmo().setMode(gizmoMode);
    	        } else if (key == GLFW_KEY_E) {
    	            gizmoMode = GizmoRenderer.GizmoMode.ROTATE;
    	            editorCore.getGizmo().setMode(gizmoMode);
    	        } else if (key == GLFW_KEY_R) {
    	            gizmoMode = GizmoRenderer.GizmoMode.SCALE;
    	            editorCore.getGizmo().setMode(gizmoMode);
    	        }
    	    }
    	    if (editingVec3Field >= 0) {
    	        if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
    	            // Commit — lấy node hiện tại để apply
    	            if (selectedNodeIndex >= 0 && selectedNodeIndex < sceneNodes.size()) {
    	                SceneNodeEntry e = sceneNodes.get(selectedNodeIndex);
    	                if (e.node != null) {
    	                    String lbl = editingLabel;
    	                    int axis = editingVec3Field;
    	                    try {
    	                        float val = Float.parseFloat(editingBuffer);
    	                        if (lbl.startsWith("Position")) {
    	                            float[] v = {e.node.position.x, e.node.position.y, e.node.position.z};
    	                            v[axis] = val;
    	                            e.node.position.set(v[0], v[1], v[2]);
    	                        } else if (lbl.startsWith("Rotation")) {
    	                            float[] v = {e.node.rotation.x, e.node.rotation.y, e.node.rotation.z};
    	                            v[axis] = val;
    	                            e.node.rotation.set(v[0], v[1], v[2]);
    	                        } else if (lbl.startsWith("Scale")) {
    	                            float[] v = {e.node.scale.x, e.node.scale.y, e.node.scale.z};
    	                            v[axis] = val;
    	                            e.node.scale.set(v[0], v[1], v[2]);
    	                        }
    	                    } catch (NumberFormatException ignored) {}
    	                }
    	            }
    	            editingVec3Field = -1; editingLabel = ""; editingBuffer = "";
    	            return;
    	        }
    	        if (key == GLFW_KEY_ESCAPE) {
    	            editingVec3Field = -1; editingLabel = ""; editingBuffer = "";
    	            return;
    	        }
    	        if (key == GLFW_KEY_BACKSPACE && action != GLFW_RELEASE) {
    	            if (!editingBuffer.isEmpty())
    	                editingBuffer = editingBuffer.substring(0, editingBuffer.length() - 1);
    	            return;
    	        }
    	    }
    	    if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
    	    if (key == GLFW_KEY_F && action == GLFW_PRESS) {
    	        crosshairSelectRequested = true;
    	        System.out.println("[KEY] F pressed, flag set"); // debug
    	        //return; // ← QUAN TRỌNG: return trước khi check CODE_EDITOR
    	    }


    	    shiftHeld = (mods & GLFW_MOD_SHIFT) != 0;
    	    boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;
    	    String[] lines = codeContent.toString().split("\n", -1);

    	    // --- CTRL SHORTCUTS ---
    	    if (ctrl) {
    	        if (key == GLFW_KEY_Z) { popUndo(); return; }
    	        if (key == GLFW_KEY_C) { copySelection(); return; }
    	        if (key == GLFW_KEY_X) { pushUndo(); cutSelection(); return; }
    	        if (key == GLFW_KEY_V) { pushUndo(); pasteClipboard(); return; }
    	        if (key == GLFW_KEY_A) { selectAll(lines); return; }
    	        if (key == GLFW_KEY_S) { saveCodeToFile("sources/modcoderpack-redevelop.java"); return; }
    	        if (key == GLFW_KEY_SLASH) { pushUndo(); toggleLineComment(lines); return; }
    	    }
    	    
    	    // --- BACKSPACE ---
    	    if (key == GLFW_KEY_BACKSPACE) {
    	        pushUndo();
    	        if (hasSelection) {
    	            deleteSelection();
    	        } else if (cursorCol > 0) {
    	            lines = codeContent.toString().split("\n", -1);
    	            String line = lines[cursorLine];
    	            String textBefore = line.substring(0, cursorCol);
    	            if (textBefore.endsWith("    ") && textBefore.stripLeading().isEmpty()) {
    	                lines[cursorLine] = line.substring(0, cursorCol - 4) + line.substring(cursorCol);
    	                cursorCol -= 4;
    	            } else {
    	                // Xóa cả cặp nếu cursor ở giữa auto-close (ví dụ: "(|)")
    	                char prev = line.charAt(cursorCol - 1);
    	                if (cursorCol < line.length() && isAutoClosePair(prev, line.charAt(cursorCol))) {
    	                    lines[cursorLine] = line.substring(0, cursorCol - 1) + line.substring(cursorCol + 1);
    	                } else {
    	                    lines[cursorLine] = line.substring(0, cursorCol - 1) + line.substring(cursorCol);
    	                }
    	                cursorCol--;
    	            }
    	            updateCodeContent(lines);
    	        } else if (cursorLine > 0) {
    	            lines = codeContent.toString().split("\n", -1);
    	            cursorCol = lines[cursorLine - 1].length();
    	            lines[cursorLine - 1] += lines[cursorLine];
    	            removeLine(lines, cursorLine);
    	            cursorLine--;
    	        }
    	        clearSelection();
    	        resetCursorBlink();
    	        return;
    	    }

    	    // --- DELETE ---
    	    if (key == GLFW_KEY_DELETE) {
    	        pushUndo();
    	        if (hasSelection) {
    	            deleteSelection();
    	        } else {
    	            lines = codeContent.toString().split("\n", -1);
    	            String line = lines[cursorLine];
    	            if (cursorCol < line.length()) {
    	                lines[cursorLine] = line.substring(0, cursorCol) + line.substring(cursorCol + 1);
    	                updateCodeContent(lines);
    	            } else if (cursorLine < lines.length - 1) {
    	                lines[cursorLine] += lines[cursorLine + 1];
    	                removeLine(lines, cursorLine + 1);
    	            }
    	        }
    	        clearSelection();
    	        resetCursorBlink();
    	        return;
    	    }

    	    // --- ENTER ---
    	    if (key == GLFW_KEY_ENTER) {
    	        pushUndo();
    	        if (hasSelection) deleteSelection();
    	        lines = codeContent.toString().split("\n", -1);
    	        String line = lines[cursorLine];
    	        String left = line.substring(0, cursorCol);
    	        String right = line.substring(cursorCol);

    	        int indentCount = 0;
    	        for (char c : left.toCharArray()) {
    	            if (c == ' ') indentCount++; else break;
    	        }
    	        String indent = " ".repeat(indentCount);
    	        boolean openBrace = left.stripTrailing().endsWith("{");
    	        boolean closeBrace = right.stripLeading().startsWith("}");
    	        lines[cursorLine] = left;

    	        if (openBrace && closeBrace) {
    	            String innerIndent = indent + "    ";
    	            String afterClose = right.stripLeading().substring(1);
    	            insertLine(lines, cursorLine + 1, innerIndent);
    	            insertLine(lines, cursorLine + 2, indent + "}" + afterClose);
    	            cursorLine++; cursorCol = innerIndent.length();
    	        } else if (openBrace) {
    	            String newIndent = indent + "    ";
    	            insertLine(lines, cursorLine + 1, newIndent + right);
    	            cursorLine++; cursorCol = newIndent.length();
    	        } else if (closeBrace) {
    	            String dedent = indentCount >= 4 ? " ".repeat(indentCount - 4) : "";
    	            insertLine(lines, cursorLine + 1, dedent + right.stripLeading());
    	            cursorLine++; cursorCol = dedent.length();
    	        } else {
    	            insertLine(lines, cursorLine + 1, indent + right);
    	            cursorLine++; cursorCol = indent.length();
    	        }
    	        clearSelection();
    	        resetCursorBlink();
    	        return;
    	    }

    	    // --- TAB ---
    	    if (key == GLFW_KEY_TAB) {
    	        lines = codeContent.toString().split("\n", -1);
    	        String line = lines[cursorLine];

    	        // Jump out khỏi closing char (giống VSCode)
    	        if (!shiftHeld && cursorCol < line.length()
    	                && isClosingChar(line.charAt(cursorCol))) {
    	            cursorCol++;
    	            resetCursorBlink();
    	            return; // KHÔNG pushUndo, KHÔNG indent
    	        }

    	        pushUndo();
    	        if (shiftHeld) {
    	            // Shift+Tab: dedent
    	            if (line.startsWith("    ")) {
    	                lines[cursorLine] = line.substring(4);
    	                cursorCol = Math.max(0, cursorCol - 4);
    	                updateCodeContent(lines);
    	            }
    	        } else {
    	            lines[cursorLine] = line.substring(0, cursorCol) + "    " + line.substring(cursorCol);
    	            cursorCol += 4;
    	            updateCodeContent(lines);
    	        }
    	        resetCursorBlink();
    	        return;
    	    }

    	    // --- NAVIGATION (với Shift để select) ---
    	    int prevLine = cursorLine, prevCol = cursorCol;

    	    if (key == GLFW_KEY_UP && cursorLine > 0) {
    	        cursorLine--; cursorCol = Math.min(cursorCol, lines[cursorLine].length());
    	    } else if (key == GLFW_KEY_DOWN && cursorLine < lines.length - 1) {
    	        cursorLine++; cursorCol = Math.min(cursorCol, lines[cursorLine].length());
    	    } else if (key == GLFW_KEY_LEFT) {
    	        if (hasSelection && !shiftHeld) {
    	            // Nhảy về đầu selection
    	            normalizeSelection();
    	            cursorLine = selectStartLine; cursorCol = selectStartCol;
    	            clearSelection(); resetCursorBlink(); return;
    	        }
    	        if (cursorCol > 0) cursorCol--;
    	        else if (cursorLine > 0) { cursorLine--; cursorCol = lines[cursorLine].length(); }
    	    } else if (key == GLFW_KEY_RIGHT) {
    	        if (hasSelection && !shiftHeld) {
    	            normalizeSelection();
    	            cursorLine = selectEndLine; cursorCol = selectEndCol;
    	            clearSelection(); resetCursorBlink(); return;
    	        }
    	        if (cursorCol < lines[cursorLine].length()) cursorCol++;
    	        else if (cursorLine < lines.length - 1) { cursorLine++; cursorCol = 0; }
    	    } else if (key == GLFW_KEY_HOME) {
    	        String line = lines[cursorLine];
    	        int first = 0;
    	        while (first < line.length() && line.charAt(first) == ' ') first++;
    	        cursorCol = (cursorCol == first) ? 0 : first;
    	    } else if (key == GLFW_KEY_END) {
    	        cursorCol = lines[cursorLine].length();
    	    } else if (key == GLFW_KEY_PAGE_UP) {
    	        cursorLine = Math.max(0, cursorLine - 20);
    	        cursorCol = Math.min(cursorCol, lines[cursorLine].length());
    	        codeScrollY = Math.max(0, codeScrollY - 20 * 22f);
    	    } else if (key == GLFW_KEY_PAGE_DOWN) {
    	        cursorLine = Math.min(lines.length - 1, cursorLine + 20);
    	        cursorCol = Math.min(cursorCol, lines[cursorLine].length());
    	        codeScrollY += 20 * 22f;
    	    }

    	    // Cập nhật selection khi Shift giữ
    	    if (shiftHeld) {
    	        if (!hasSelection) {
    	            selectStartLine = prevLine; selectStartCol = prevCol;
    	        }
    	        selectEndLine = cursorLine; selectEndCol = cursorCol;
    	        hasSelection = true;
    	    } else {
    	        clearSelection();
    	    }

    	    // Auto-scroll theo cursor
    	    float cursorScreenY = 20 + cursorLine * 22f - codeScrollY;
    	    float editorH = height - MENU_HEIGHT - TAB_HEIGHT;
    	    if (cursorScreenY < 20) codeScrollY = Math.max(0, cursorLine * 22f - 20);
    	    if (cursorScreenY > editorH - 40) codeScrollY = cursorLine * 22f - editorH + 60;

    	    resetCursorBlink();
    	});
        
    	// Console scroll
    	glfwSetScrollCallback(window, (win, xoff, yoff) -> {
    	    if (currentTab == EditorTab.CODE_EDITOR) {
    	        codeScrollY -= (float)(yoff * 40);
    	        codeScrollY = Math.max(0, codeScrollY);
    	    } else if (currentTab == EditorTab.CONSOLE) {
    	        consoleAutoScroll = false;
    	        consoleScrollY -= (float)(yoff * 35);
    	        consoleScrollY = Math.max(0, consoleScrollY);
    	    } else if (mouseX < inspectorWidth) {
    	        treeScrollY -= (float)(yoff * 30);
    	        treeScrollY = Math.max(0, treeScrollY);
    	    } else if (currentTab == EditorTab.NODE_EDITOR) {
    	        // --- ZOOM TO MOUSE LOGIC ---
    	        float neX = inspectorWidth + 20;
    	        float neY = MENU_HEIGHT + TAB_HEIGHT;

    	        // Vị trí chuột cục bộ
    	        float localMx = (float)mouseX - neX;
    	        float localMy = (float)mouseY - neY;

    	        // Tọa độ thế giới trước khi zoom
    	        float worldX = (localMx - neOffsetX) / neScale;
    	        float worldY = (localMy - neOffsetY) / neScale;

    	        // Tính toán scale mới
    	        float zoomFactor = 1.1f;
    	        if (yoff > 0) neScale *= zoomFactor;
    	        else if (yoff < 0) neScale /= zoomFactor;

    	        // Giới hạn độ thu phóng để tránh lỗi hiển thị
    	        neScale = Math.max(0.15f, Math.min(neScale, 3.0f));

    	        // Cập nhật lại Offset để tâm zoom nằm tại con trỏ chuột
    	        neOffsetX = localMx - (worldX * neScale);
    	        neOffsetY = localMy - (worldY * neScale);
    	    }
    	});
    }
    
    private void drawSceneTree(float x, float y, float w, float h) {
        // Nền
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, rgba(22, 22, 26, 255));
        nvgFill(nvg);

        // Header
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, 30);
        nvgFillColor(nvg, rgba(30, 30, 36, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 11.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(160, 160, 170, 255));
		nvgText(nvg, x + 10, y + 15, sideTab == 0 ? "SCENE TREE"
                                    : sideTab == 1 ? "FILE EXPLORER"
                                                  : "LIBRARIES");

        // Nút "+" chỉ hiện ở tab Scene
        if (sideTab == 0) {
            float btnSize = 20;
            float btnX = x + w - btnSize - 6, btnY = y + 5;
            boolean btnHover = isMouseOver(btnX, btnY, btnSize, btnSize);
            nvgBeginPath(nvg);
            nvgRoundedRect(nvg, btnX, btnY, btnSize, btnSize, 4);
            nvgFillColor(nvg, btnHover ? rgba(70, 140, 80, 255) : rgba(45, 100, 55, 255));
            nvgFill(nvg);
            nvgFontSize(nvg, 16.0f);
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(255, 255, 255, 255));
            nvgText(nvg, btnX + btnSize / 2, btnY + btnSize / 2, "+");
            if (btnHover && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                showAddNodeMenu = !showAddNodeMenu;
                addMenuX = btnX - 100; addMenuY = btnY + btnSize + 2;
                showContextMenu = false;
            }
        }

        // --- MINI TABS ---
        float mtY = y + 30, mtH = 24, mtW = w / 3f;
        String[] mtLabels = {"Scene", "Files", "Libs"};
        for (int i = 0; i < 3; i++) {
            boolean active = sideTab == i;
            boolean hov = isMouseOver(x + i * mtW, mtY, mtW, mtH);
            nvgBeginPath(nvg);
            nvgRect(nvg, x + i * mtW, mtY, mtW, mtH);
            nvgFillColor(nvg, active ? rgba(28, 28, 36, 255) : rgba(20, 20, 26, 255));
            nvgFill(nvg);
            // Active indicator
            if (active) {
                nvgBeginPath(nvg);
                nvgRect(nvg, x + i * mtW, mtY + mtH - 2, mtW, 2);
                nvgFillColor(nvg, rgba(65, 115, 215, 255));
                nvgFill(nvg);
            }
            nvgFontSize(nvg, 10.5f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, active ? rgba(200, 200, 215, 255) : rgba(80, 80, 100, 255));
            nvgText(nvg, x + i * mtW + mtW / 2, mtY + mtH / 2, mtLabels[i]);
        }

        // Divider
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x, mtY + mtH); nvgLineTo(nvg, x + w, mtY + mtH);
        nvgStrokeColor(nvg, rgba(18, 18, 24, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        // Content area bắt đầu từ y + 30 + 24 = y + 54
        float contentY = y + 54, contentH = h - 54;
        nvgSave(nvg);
        nvgScissor(nvg, x, contentY, w, contentH);

        switch (sideTab) {
            case 0 -> drawSceneList(x, contentY, w, contentH);
            case 1 -> drawFileExplorer(x, contentY, w, contentH);
            case 2 -> drawLibManager(x, contentY, w, contentH);
        }

        nvgRestore(nvg);
    }

    // --- Scene list (tách từ code cũ) ---
    private void drawSceneList(float x, float y, float w, float h) {
        float itemH = 26;
        for (int idx = 0; idx < sceneNodes.size(); idx++) {
            SceneNodeEntry e = sceneNodes.get(idx);
            float iy = y + 5 + idx * itemH - treeScrollY;
            if (iy + itemH < y || iy > y + h) continue;

            boolean isSelected = idx == selectedNodeIndex;
            boolean isHover = isMouseOver(x, iy, w, itemH);

            if (isSelected) {
                nvgBeginPath(nvg); nvgRect(nvg, x, iy, w, itemH);
                nvgFillColor(nvg, rgba(38, 79, 140, 220)); nvgFill(nvg);
            } else if (isHover) {
                nvgBeginPath(nvg); nvgRect(nvg, x, iy, w, itemH);
                nvgFillColor(nvg, rgba(40, 40, 50, 150)); nvgFill(nvg);
            }

            float indentX = x + 8 + e.depth * 16;
            String icon = switch (e.type) {
                case "Box3D" -> "▣"; case "Sphere3D" -> "●";
                case "Plane3D" -> "▬"; case "CameraNode" -> "◎";
                case "GroupNode" -> "◈"; default -> "•";
            };
            nvgFontSize(nvg, 12.0f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(100, 170, 255, 255));
            nvgText(nvg, indentX, iy + itemH / 2, icon);
            nvgFontSize(nvg, 13.0f);
            nvgFillColor(nvg, isSelected ? rgba(255,255,255,255) : rgba(195,195,205,255));
            nvgText(nvg, indentX + 16, iy + itemH / 2, e.name);
            nvgFontSize(nvg, 10.0f);
            nvgFillColor(nvg, rgba(90, 90, 110, 255));
            nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
            nvgText(nvg, x + w - 6, iy + itemH / 2, e.type);

            if (isHover && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                selectedNodeIndex = idx;
                showAddNodeMenu = false; showContextMenu = false;
            }
            if (isHover && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                selectedNodeIndex = idx; ctxTargetIndex = idx;
                ctxMenuX = (float)mouseX; ctxMenuY = (float)mouseY;
                showContextMenu = true; showAddNodeMenu = false;
            }
        }
    }

    // --- File Explorer ---
    private void drawFileExplorer(float x, float y, float w, float h) {
        if (fileTree.isEmpty()) {
            nvgFontSize(nvg, 11.0f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(55, 55, 70, 255));
            nvgText(nvg, x + w / 2, y + 40, "No project loaded");
            return;
        }

        float itemH = 22;
        for (int i = 0; i < fileTree.size(); i++) {
            FileTreeEntry entry = fileTree.get(i);
            float iy = y + 4 + i * itemH - fileExplorerScrollY;
            if (iy + itemH < y || iy > y + h) continue;

            boolean isSelected = entry.path.equals(selectedFile);
            boolean hov = isMouseOver(x, iy, w, itemH);

            // Background
            if (isSelected) {
                nvgBeginPath(nvg); nvgRect(nvg, x, iy, w, itemH);
                nvgFillColor(nvg, rgba(35, 70, 130, 200)); nvgFill(nvg);
            } else if (hov) {
                nvgBeginPath(nvg); nvgRect(nvg, x, iy, w, itemH);
                nvgFillColor(nvg, rgba(35, 35, 45, 150)); nvgFill(nvg);
            }

            float indentX = x + 6 + entry.depth * 16;

            // Arrow cho folder
            if (entry.isDir) {
                boolean expanded = expandedDirs.contains(entry.path);
                nvgFontSize(nvg, 9.0f);
                nvgFontFace(nvg, "sans");
                nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
                nvgFillColor(nvg, rgba(120, 120, 140, 255));
                nvgText(nvg, indentX + 5, iy + itemH / 2, expanded ? "▼" : "▶");
            }

            // Icon
            String name = entry.path.getFileName().toString();
            String icon;
            NVGColor iconColor;
            if (entry.isDir) {
                icon = "📁";
                iconColor = rgba(200, 160, 60, 255);
            } else if (name.endsWith(".java")) {
                icon = "J";
                iconColor = rgba(80, 180, 255, 255);
            } else if (name.endsWith(".ygp")) {
                icon = "Y";
                iconColor = rgba(120, 220, 120, 255);
            } else if (name.endsWith(".jar")) {
                icon = "▣";
                iconColor = rgba(200, 140, 60, 255);
            } else if (name.endsWith(".png") || name.endsWith(".jpg")) {
                icon = "🖼";
                iconColor = rgba(180, 120, 200, 255);
            } else {
                icon = "•";
                iconColor = rgba(100, 100, 120, 255);
            }

            // Icon box cho file .java
            if (name.endsWith(".java")) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, indentX + 12, iy + 4, 14, 14, 2);
                nvgFillColor(nvg, rgba(20, 50, 100, 255));
                nvgFill(nvg);
            }

            nvgFontSize(nvg, 11.5f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, iconColor);
            nvgText(nvg, indentX + 19, iy + itemH / 2, icon);

            // Tên file
            nvgFontSize(nvg, 12.0f);
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, entry.isDir
                ? rgba(210, 210, 225, 255)
                : isSelected ? rgba(255, 255, 255, 255) : rgba(175, 175, 195, 255));
            nvgText(nvg, indentX + 30, iy + itemH / 2, name);

            // Click
            /*if (hov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                if (entry.isDir) {
                    // Toggle expand
                    if (expandedDirs.contains(entry.path))
                        expandedDirs.remove(entry.path);
                    else
                        expandedDirs.add(entry.path);
                    refreshProjectFiles(); // rebuild tree
                } else {
                    selectedFile = entry.path;
                    // Mở .java trong code editor
                    if (name.endsWith(".java")) {
                        try {
                            String content = Files.readString(entry.path);
                            codeContent = new StringBuilder(content);
                            cursorLine = 0; cursorCol = 0;
                            currentTab = EditorTab.CODE_EDITOR;
                        } catch (Exception ignored) {}
                    }
                }
            }*/
        }
    }

    // --- Library Manager ---
    private void drawLibManager(float x, float y, float w, float h) {
        // Add lib button
        float btnY = y + 4;
        boolean addHov = isMouseOver(x + 8, btnY, w - 16, 26);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + 8, btnY, w - 16, 26, 5);
        nvgFillColor(nvg, addHov ? rgba(35, 80, 150, 255) : rgba(25, 55, 110, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 11.0f); nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(180, 210, 255, 255));
        nvgText(nvg, x + w / 2, btnY + 13, "+ Add .jar Library");
        if (addHov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !mousePressedLastFrame) {
            // AWT file dialog
            new Thread(() -> {
                java.awt.FileDialog fd = new java.awt.FileDialog(
                    (java.awt.Frame) null, "Select JAR", java.awt.FileDialog.LOAD);
                fd.setFilenameFilter((dir, name) -> name.endsWith(".jar"));
                fd.setVisible(true);
                if (fd.getFile() != null) {
                    LibraryManager.getInstance().addLibrary(fd.getDirectory() + fd.getFile());
                    refreshLibraries();
                }
            }).start();
        }

        // Lib list
        List<LibraryManager.LibEntry> libs = LibraryManager.getInstance().getLibraries();
        float itemH = 28;
        float listY = y + 36;
        for (int i = 0; i < libs.size(); i++) {
            LibraryManager.LibEntry lib = libs.get(i);
            float iy = listY + i * itemH;
            if (iy + itemH < y || iy > y + h) continue;

            boolean hov = isMouseOver(x, iy, w, itemH);
            if (hov) {
                nvgBeginPath(nvg); nvgRect(nvg, x, iy, w, itemH);
                nvgFillColor(nvg, rgba(30, 30, 40, 180)); nvgFill(nvg);
            }

            // Jar icon
            nvgFontSize(nvg, 11.0f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(200, 160, 80, 255));
            nvgText(nvg, x + 8, iy + 14, "▣");

            // Name
            nvgFillColor(nvg, rgba(185, 185, 200, 255));
            String displayName = lib.name.length() > 22
                ? lib.name.substring(0, 19) + "..." : lib.name;
            nvgText(nvg, x + 24, iy + 14, displayName);

            // Size
            nvgFontSize(nvg, 9.5f);
            nvgFillColor(nvg, rgba(70, 70, 90, 255));
            nvgText(nvg, x + 24, iy + 22, lib.getSizeStr());

            // Delete button
            float delX = x + w - 22, delY = iy + 6;
            boolean delHov = isMouseOver(delX, delY, 16, 16);
            nvgFontSize(nvg, 12.0f);
            nvgFillColor(nvg, delHov ? rgba(220, 60, 60, 255) : rgba(80, 40, 40, 255));
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgText(nvg, delX + 8, delY + 8, "×");
            if (delHov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                LibraryManager.getInstance().removeLibrary(lib.name);
                refreshLibraries();
            }

            // Divider
            nvgBeginPath(nvg);
            nvgMoveTo(nvg, x + 6, iy + itemH - 1);
            nvgLineTo(nvg, x + w - 6, iy + itemH - 1);
            nvgStrokeColor(nvg, rgba(28, 28, 36, 255));
            nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);
        }

        if (libs.isEmpty()) {
            nvgFontSize(nvg, 11.0f);
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(50, 50, 65, 255));
            nvgText(nvg, x + w / 2, y + 70, "No libraries added");
        }
    }

    // --- Helpers ---
    private void refreshProjectFiles() {
        fileTree.clear();
        if (!ProjectManager.getInstance().isProjectLoaded()) return;
        Path root = Paths.get(ProjectManager.getInstance().getProjectDir());
        buildFileTree(root, 0);
    }

    private void buildFileTree(Path dir, int depth) {
        try {
            List<Path> children = Files.list(dir)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return !name.equals("build") && !name.equals("dist")
                        && !name.equals(".git") && !name.startsWith(".");
                })
                .sorted(Comparator
                    .comparing((Path p) -> !Files.isDirectory(p)) // dirs first
                    .thenComparing(p -> p.getFileName().toString().toLowerCase()))
                .toList();

            for (Path child : children) {
                boolean isDir = Files.isDirectory(child);
                fileTree.add(new FileTreeEntry(child, depth, isDir));
                // Nếu là dir và đang expanded → đệ quy
                if (isDir && expandedDirs.contains(child)) {
                    buildFileTree(child, depth + 1);
                }
            }
        } catch (IOException ignored) {}
    }

    private List<LibraryManager.LibEntry> cachedLibs = new ArrayList<>();
    private void refreshLibraries() {
        cachedLibs = LibraryManager.getInstance().getLibraries();
    }
    
    private void drawAddNodeMenu(float x, float y) {
        String[] types = {"Box3D", "Sphere3D", "Plane3D", "GroupNode", "CameraNode"};
        float mw = 130, mh = types.length * 28 + 8;

        // Clamp để không ra ngoài màn hình
        if (x + mw > width) x = width - mw - 4;
        if (y + mh > height) y = height - mh - 4;

        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, mw, mh, 6);
        nvgFillColor(nvg, rgba(32, 32, 40, 255));
        nvgFill(nvg);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, mw, mh, 6);
        nvgStrokeColor(nvg, rgba(55, 55, 75, 255));
        nvgStrokeWidth(nvg, 1.0f);
        nvgStroke(nvg);

        for (int i = 0; i < types.length; i++) {
            float iy = y + 4 + i * 28;
            boolean hov = isMouseOver(x + 2, iy, mw - 4, 26);
            if (hov) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, x + 2, iy, mw - 4, 26, 4);
                nvgFillColor(nvg, rgba(45, 95, 155, 200));
                nvgFill(nvg);
            }
            nvgFontSize(nvg, 13.0f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(200, 200, 210, 255));
            nvgText(nvg, x + 12, iy + 13, types[i]);

            if (hov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                addSceneNode(types[i]);
                showAddNodeMenu = false;
            }
        }

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !isMouseOver(x, y, mw, mh) && !mousePressedLastFrame) {
            showAddNodeMenu = false;
        }
    }
    
    private void drawContextMenu(float x, float y) {
        String[] opts = {"Duplicate", "─────────", "Delete"};
        float mw = 120, mh = opts.length * 28 + 8;

        if (x + mw > width) x = width - mw - 4;
        if (y + mh > height) y = height - mh - 4;

        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, mw, mh, 6);
        nvgFillColor(nvg, rgba(32, 32, 40, 255));
        nvgFill(nvg);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, mw, mh, 6);
        nvgStrokeColor(nvg, rgba(55, 55, 75, 255));
        nvgStrokeWidth(nvg, 1.0f);
        nvgStroke(nvg);

        for (int i = 0; i < opts.length; i++) {
            float iy = y + 4 + i * 28;
            if (opts[i].startsWith("─")) {
                nvgBeginPath(nvg);
                nvgMoveTo(nvg, x + 8, iy + 14);
                nvgLineTo(nvg, x + mw - 8, iy + 14);
                nvgStrokeColor(nvg, rgba(55, 55, 75, 255));
                nvgStrokeWidth(nvg, 1.0f);
                nvgStroke(nvg);
                continue;
            }
            boolean isDel = opts[i].equals("Delete");
            boolean hov = isMouseOver(x + 2, iy, mw - 4, 26);
            if (hov) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, x + 2, iy, mw - 4, 26, 4);
                nvgFillColor(nvg, isDel ? rgba(130, 35, 35, 200) : rgba(45, 95, 155, 200));
                nvgFill(nvg);
            }
            nvgFontSize(nvg, 13.0f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, isDel ? rgba(255, 90, 90, 255) : rgba(200, 200, 210, 255));
            nvgText(nvg, x + 12, iy + 13, opts[i]);

            if (hov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                    && !mousePressedLastFrame) {
                handleContextAction(opts[i], ctxTargetIndex);
                showContextMenu = false;
            }
        }

        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !isMouseOver(x, y, mw, mh) && !mousePressedLastFrame) {
            showContextMenu = false;
        }
    }
    
    private void addSceneNode(String type) {
        Object3D node = switch (type) {
            case "Box3D"      -> new vn.pmgteam.yanase.node.subnodes.Box3D("Box_" + sceneNodes.size());
            case "Sphere3D"   -> new vn.pmgteam.yanase.node.subnodes.Sphere3D("Sphere_" + sceneNodes.size());
            case "Plane3D"    -> new vn.pmgteam.yanase.node.subnodes.Plane3D("Plane_" + sceneNodes.size());
            case "GroupNode"  -> new vn.pmgteam.yanase.node.subnodes.GroupNode("Group_" + sceneNodes.size());
            case "CameraNode" -> new vn.pmgteam.yanase.node.subnodes.CameraNode("Camera_" + sceneNodes.size());
            default -> null;
        };
        if (node == null) return;
        editorCore.addNodeToScene(node);
        syncSceneTree();
        selectedNodeIndex = sceneNodes.size() - 1;
    }

    private void handleContextAction(String action, int idx) {
        if (idx < 0 || idx >= sceneNodes.size()) return;
        SceneNodeEntry e = sceneNodes.get(idx);
        switch (action) {
            case "Delete" -> {
                editorCore.removeNodeFromScene(e.node);
                syncSceneTree();
                selectedNodeIndex = Math.min(selectedNodeIndex, sceneNodes.size() - 1);
            }
            case "Duplicate" -> {
                addSceneNode(e.type);
                // Copy transform
                if (!sceneNodes.isEmpty()) {
                    Object3D last = sceneNodes.get(sceneNodes.size() - 1).node;
                    last.position.set(e.node.position.x + 1, e.node.position.y, e.node.position.z);
                    last.rotation.set(e.node.rotation);
                    last.scale.set(e.node.scale);
                }
                syncSceneTree();
            }
        }
    }
    
    @FunctionalInterface
    interface Vec3Setter { void set(float x, float y, float z); }

    private float drawVec3Row(float x, float y, float w, String label,
            float vx, float vy, float vz, Vec3Setter setter) {
        // Label
        nvgFontSize(nvg, 11.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(140, 140, 150, 255));
        nvgText(nvg, x, y + 10, label);

        float fw = (w - 6) / 3f;
        float[] values = {vx, vy, vz};
        String[] axes = {"X", "Y", "Z"};
        int[] colors = {0xCC3333, 0x33AA44, 0x3366CC};

        // ID duy nhất cho row này dựa trên label + vị trí y
        String rowId = label + selectedNodeIndex;

        for (int i = 0; i < 3; i++) {
            float fx = x + i * (fw + 3);
            float fy = y + 18;
            String fieldId = rowId + i;
            boolean isEditing = fieldId.equals(editingVec3Field + editingLabel);
            boolean hov = isMouseOver(fx, fy, fw, 20);

            // Nền input — highlight khi hover hoặc editing
            nvgBeginPath(nvg);
            nvgRoundedRect(nvg, fx, fy, fw, 20, 3);
            nvgFillColor(nvg, isEditing ? rgba(25, 35, 55, 255)
                           : hov       ? rgba(28, 28, 36, 255)
                                       : rgba(15, 15, 18, 255));
            nvgFill(nvg);

            // Border khi editing
            if (isEditing) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, fx, fy, fw, 20, 3);
                nvgStrokeColor(nvg, rgba(60, 120, 220, 255));
                nvgStrokeWidth(nvg, 1.0f);
                nvgStroke(nvg);
            }

            // Axis color prefix
            nvgBeginPath(nvg);
            nvgRoundedRect(nvg, fx, fy, 14, 20, 3);
            int c = colors[i];
            nvgFillColor(nvg, rgba((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, isEditing ? 255 : 200));
            nvgFill(nvg);

            // Label X/Y/Z
            nvgFontSize(nvg, 10.0f);
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(255, 255, 255, 255));
            nvgText(nvg, fx + 7, fy + 10, axes[i]);

            // Giá trị hoặc buffer đang nhập
            String displayVal = isEditing ? editingBuffer + "|" : String.format("%.3f", values[i]);
            nvgFontSize(nvg, 11.0f);
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, isEditing ? rgba(100, 180, 255, 255) : rgba(200, 200, 210, 255));
            nvgText(nvg, fx + 17, fy + 10, displayVal);

            // Cursor resize khi hover (drag mode)
            if (hov && !isEditing) {
                glfwSetCursor(window, glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR));
            }

            // Click đúp → bắt đầu nhập số
            // Click đơn + drag → thay đổi giá trị
            if (hov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                if (!isEditing) {
                    float delta = (float)(mouseX - lastMouseXForDrag) * 0.02f;
                    if (Math.abs(delta) > 0.0005f) {
                        float nx = vx + (i == 0 ? delta : 0);
                        float ny = vy + (i == 1 ? delta : 0);
                        float nz = vz + (i == 2 ? delta : 0);
                        setter.set(nx, ny, nz);
                    }
                }
            }

            // Double click → edit mode
            if (hov && mousePressedLastFrame == false
                    && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                if (editingVec3Field == i && editingLabel.equals(rowId)) {
                    // Đang edit cùng field → commit
                    commitVec3Edit(values, i, setter, vx, vy, vz);
                } else {
                    editingVec3Field = i;
                    editingLabel = rowId;
                    editingBuffer = String.format("%.3f", values[i]);
                }
            }
        }
        return y + 42;
    }

    private void commitVec3Edit(float[] values, int axis,
            Vec3Setter setter, float vx, float vy, float vz) {
        try {
            float val = Float.parseFloat(editingBuffer);
            float nx = axis == 0 ? val : vx;
            float ny = axis == 1 ? val : vy;
            float nz = axis == 2 ? val : vz;
            setter.set(nx, ny, nz);
        } catch (NumberFormatException ignored) {}
        editingVec3Field = -1;
        editingLabel = "";
        editingBuffer = "";
    }

    private void drawNodeInspector(float x, float y, float w, float h) {
        // Nền
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, rgba(22, 22, 26, 255));
        nvgFill(nvg);

        // Header
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, 32);
        nvgFillColor(nvg, rgba(28, 28, 34, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 10.5f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(130, 130, 145, 255));
        nvgText(nvg, x + 10, y + 16, "INSPECTOR");

        if (selectedNodeIndex < 0 || selectedNodeIndex >= sceneNodes.size()) {
            nvgFontSize(nvg, 12.0f);
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(55, 55, 70, 255));
            nvgText(nvg, x + w / 2, y + 80, "No node selected");
            nvgFontSize(nvg, 10.5f);
            nvgFillColor(nvg, rgba(40, 40, 55, 255));
            nvgText(nvg, x + w / 2, y + 100, "Click a node to inspect");
            return;
        }

        SceneNodeEntry e = sceneNodes.get(selectedNodeIndex);
        if (e.node == null) return;

        float cy = y + 38;
        float pad = 10;

        // --- Node header block ---
        nvgBeginPath(nvg);
        nvgRect(nvg, x + pad, cy, w - pad * 2, 46);
        nvgFillColor(nvg, rgba(26, 26, 32, 255));
        nvgFill(nvg);

        // Icon
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + pad + 6, cy + 8, 30, 30, 6);
        nvgFillColor(nvg, rgba(30, 55, 110, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 14.0f);
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(80, 140, 255, 255));
        String icon = switch (e.type) {
            case "Box3D" -> "▣"; case "Sphere3D" -> "●";
            case "Plane3D" -> "▬"; case "CameraNode" -> "◎";
            case "GroupNode" -> "◈"; default -> "•";
        };
        nvgText(nvg, x + pad + 21, cy + 23, icon);

        // Tên node — có thể inline rename sau
        nvgFontSize(nvg, 13.5f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(215, 215, 228, 255));
        nvgText(nvg, x + pad + 44, cy + 16, e.name);

        // Type badge
        nvgBeginPath(nvg);
        float badgeW = measureText(e.type) + 12;
        nvgRoundedRect(nvg, x + pad + 44, cy + 28, badgeW, 14, 3);
        nvgFillColor(nvg, rgba(30, 50, 90, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 9.5f);
        nvgFillColor(nvg, rgba(80, 130, 220, 255));
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, x + pad + 50, cy + 35, e.type);

        cy += 54;

        // --- TRANSFORM SECTION ---
        cy = inspectorSection(x, cy, w, pad, "TRANSFORM");

        cy = drawVec3Row(x + pad, cy, w - pad * 2, "Position",
            e.node.position.x, e.node.position.y, e.node.position.z,
            (nx, ny, nz) -> e.node.position.set(nx, ny, nz));
        cy += 4;
        cy = drawVec3Row(x + pad, cy, w - pad * 2, "Rotation",
            e.node.rotation.x, e.node.rotation.y, e.node.rotation.z,
            (nx, ny, nz) -> e.node.rotation.set(nx, ny, nz));
        cy += 4;
        cy = drawVec3Row(x + pad, cy, w - pad * 2, "Scale",
            e.node.scale.x, e.node.scale.y, e.node.scale.z,
            (nx, ny, nz) -> e.node.scale.set(nx, ny, nz));
        cy += 8;

        // Reset Transform button
        float resetW = (w - pad * 2 - 4) / 2f;
        boolean resetHov = isMouseOver(x + pad, cy, resetW, 24);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + pad, cy, resetW, 24, 4);
        nvgFillColor(nvg, resetHov ? rgba(45, 45, 58, 255) : rgba(30, 30, 38, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 10.5f);
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(150, 150, 165, 255));
        nvgText(nvg, x + pad + resetW / 2, cy + 12, "Reset Transform");
        if (resetHov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !mousePressedLastFrame) {
            e.node.position.set(0, 0, 0);
            e.node.rotation.set(0, 0, 0);
            e.node.scale.set(1, 1, 1);
        }

        // Focus camera button
        boolean focusHov = isMouseOver(x + pad + resetW + 4, cy, resetW, 24);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + pad + resetW + 4, cy, resetW, 24, 4);
        nvgFillColor(nvg, focusHov ? rgba(30, 60, 110, 255) : rgba(22, 40, 80, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 10.5f);
        nvgFillColor(nvg, rgba(80, 140, 220, 255));
        nvgText(nvg, x + pad + resetW + 4 + resetW / 2, cy + 12, "Focus Camera");
        if (focusHov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !mousePressedLastFrame) {
            // Camera bay về phía node
            editorCore.focusCameraOnNode(e.node);
        }
        cy += 32;

        // --- NODE SETTINGS SECTION ---
        cy = inspectorSection(x, cy, w, pad, "NODE");


        // Visibility toggle — truyền callback
        cy = drawToggleRow(x + pad, cy, w - pad * 2, "Visible", e.visible, () -> {
            e.visible = !e.visible;
            // Ẩn/hiện node bằng cách set scale về 0 hoặc dùng flag
            if (e.node instanceof vn.pmgteam.yanase.node.subnodes.GridNode) {
                ((vn.pmgteam.yanase.node.subnodes.GridNode) e.node).setVisible(e.visible);
            }
            // Cho các node khác: scale về 0 khi ẩn
            if (!e.visible) e.node.scale.set(0, 0, 0);
            else e.node.scale.set(1, 1, 1);
        });
        cy += 2;

        // --- ACTIONS SECTION ---
        cy = inspectorSection(x, cy, w, pad, "ACTIONS");

        // Duplicate
        float actW = (w - pad * 2 - 4) / 2f;
        drawActionBtn(x + pad, cy, actW, 26, "Duplicate",
        	    new int[]{35, 70, 35}, new int[]{50, 100, 50},
        	    () -> handleContextAction("Duplicate", selectedNodeIndex));

       drawActionBtn(x + pad + actW + 4, cy, actW, 26, "Delete",
        	    new int[]{80, 20, 20}, new int[]{140, 35, 35},
        	    () -> handleContextAction("Delete", selectedNodeIndex));
        cy += 34;
    }

    // --- Helper: Section header ---
    private float inspectorSection(float x, float cy, float w, float pad, String title) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, cy, w, 22);
        nvgFillColor(nvg, rgba(18, 18, 24, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 9.5f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(70, 70, 90, 255));
        nvgText(nvg, x + 10, cy + 11, title);
        // Line
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x + 10 + measureText(title) + 6, cy + 11);
        nvgLineTo(nvg, x + w - 10, cy + 11);
        nvgStrokeColor(nvg, rgba(32, 32, 42, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);
        return cy + 26;
    }

    // --- Helper: Toggle row ---
    private float drawToggleRow(float x, float y, float w, String label, boolean value, Runnable onToggle) {
        nvgFontSize(nvg, 11.5f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(140, 140, 155, 255));
        nvgText(nvg, x, y + 12, label);

        float tx = x + w - 36, ty = y + 4;
        boolean hovToggle = isMouseOver(tx, ty, 32, 16);

        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, tx, ty, 32, 16, 8);
        nvgFillColor(nvg, value ? rgba(40, 130, 60, 255) : rgba(50, 50, 60, 255));
        nvgFill(nvg);

        nvgBeginPath(nvg);
        nvgCircle(nvg, value ? tx + 24 : tx + 8, ty + 8, 6);
        nvgFillColor(nvg, rgba(220, 220, 230, 255));
        nvgFill(nvg);

        // ← THÊM click handler
        if (hovToggle && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !mousePressedLastFrame) {
            onToggle.run();
        }

        return y + 24;
    }

    // --- Helper: Action button ---
    private void drawActionBtn(float x, float y, float w, float h,
            String label, int[] base, int[] hover, Runnable action) {
        boolean hov = isMouseOver(x, y, w, h);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, 4);
        int[] c = hov ? hover : base;
        nvgFillColor(nvg, rgba(c[0], c[1], c[2], 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 11.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(210, 210, 220, 255));
        nvgText(nvg, x + w / 2, y + h / 2, label);
        if (hov && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS
                && !mousePressedLastFrame) action.run();
    }
    
    private void syncSceneTree() {
        sceneNodes.clear();
        Object3D content = editorCore.getSceneContent();
        if (content == null) return;
        // Duyệt trực tiếp children của sceneContent (depth=0)
        for (vn.pmgteam.yanase.node.BaseNode child : content.getChildren()) {
            if (child instanceof Object3D) collectNodes((Object3D) child, 0);
        }
    }

    private void collectNodes(Object3D node, int depth) {
        String type = node.getClass().getSimpleName();
        sceneNodes.add(new SceneNodeEntry(node.getNodeName(), type, depth, node));
        for (vn.pmgteam.yanase.node.BaseNode child : node.getChildren()) {
            if (child instanceof Object3D) collectNodes((Object3D) child, depth + 1);
        }
    }
    
    private boolean isClosingChar(char c) {
        return c == ')' || c == ']' || c == '}' || c == '"' || c == '\'';
    }
    
    // --- UNDO ---
    private void pushUndo() {
        if (undoStack.size() >= MAX_UNDO) undoStack.pollFirst();
        undoStack.push(cursorLine + ":" + cursorCol + ":" + codeContent.toString());
    }

    private void popUndo() {
        if (undoStack.isEmpty()) return;
        String state = undoStack.pop();
        int firstColon = state.indexOf(':');
        int secondColon = state.indexOf(':', firstColon + 1);
        cursorLine = Integer.parseInt(state.substring(0, firstColon));
        cursorCol = Integer.parseInt(state.substring(firstColon + 1, secondColon));
        codeContent = new StringBuilder(state.substring(secondColon + 1));
        clearSelection();
        resetCursorBlink();
    }

    // --- SELECTION ---
    private void clearSelection() {
        hasSelection = false;
        selectStartLine = selectEndLine = -1;
        selectStartCol = selectEndCol = -1;
    }

    private void normalizeSelection() {
        // Đảm bảo start < end
        if (selectStartLine > selectEndLine || 
           (selectStartLine == selectEndLine && selectStartCol > selectEndCol)) {
            int tl = selectStartLine, tc = selectStartCol;
            selectStartLine = selectEndLine; selectStartCol = selectEndCol;
            selectEndLine = tl; selectEndCol = tc;
        }
    }

    private void selectAll(String[] lines) {
        selectStartLine = 0; selectStartCol = 0;
        selectEndLine = lines.length - 1;
        selectEndCol = lines[selectEndLine].length();
        cursorLine = selectEndLine; cursorCol = selectEndCol;
        hasSelection = true;
    }

    private String getSelectedText() {
        if (!hasSelection) return "";
        normalizeSelection();
        String[] lines = codeContent.toString().split("\n", -1);
        if (selectStartLine == selectEndLine) {
            return lines[selectStartLine].substring(selectStartCol, selectEndCol);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(lines[selectStartLine].substring(selectStartCol)).append("\n");
        for (int i = selectStartLine + 1; i < selectEndLine; i++)
            sb.append(lines[i]).append("\n");
        sb.append(lines[selectEndLine].substring(0, selectEndCol));
        return sb.toString();
    }

    private void deleteSelection() {
        if (!hasSelection) return;
        normalizeSelection();
        String[] lines = codeContent.toString().split("\n", -1);
        String before = lines[selectStartLine].substring(0, selectStartCol);
        String after = lines[selectEndLine].substring(selectEndCol);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectStartLine; i++) sb.append(lines[i]).append("\n");
        sb.append(before).append(after);
        for (int i = selectEndLine + 1; i < lines.length; i++) sb.append("\n").append(lines[i]);
        codeContent = new StringBuilder(sb.toString());
        cursorLine = selectStartLine; cursorCol = selectStartCol;
        clearSelection();
    }

    // --- CLIPBOARD ---
    private void copySelection() {
        if (!hasSelection) return;
        glfwSetClipboardString(window, getSelectedText());
    }

    private void cutSelection() {
        if (!hasSelection) return;
        glfwSetClipboardString(window, getSelectedText());
        deleteSelection();
    }

    private void pasteClipboard() {
        String text = glfwGetClipboardString(window);
        if (text == null || text.isEmpty()) return;
        if (hasSelection) deleteSelection();
        String[] pasteLines = text.split("\n", -1);
        String[] lines = codeContent.toString().split("\n", -1);
        String before = lines[cursorLine].substring(0, cursorCol);
        String after = lines[cursorLine].substring(cursorCol);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cursorLine; i++) sb.append(lines[i]).append("\n");
        if (pasteLines.length == 1) {
            sb.append(before).append(pasteLines[0]).append(after);
            cursorCol = before.length() + pasteLines[0].length();
        } else {
            sb.append(before).append(pasteLines[0]).append("\n");
            for (int i = 1; i < pasteLines.length - 1; i++) sb.append(pasteLines[i]).append("\n");
            String last = pasteLines[pasteLines.length - 1];
            sb.append(last).append(after);
            cursorLine += pasteLines.length - 1;
            cursorCol = last.length();
        }
        for (int i = cursorLine + 1 - (pasteLines.length - 1); i < lines.length; i++) {
            // đã append after ở trên
        }
        // Rebuild đúng cách
        String[] oldLines = codeContent.toString().split("\n", -1);
        StringBuilder full = new StringBuilder();
        for (int i = 0; i < cursorLine; i++) full.append(lines[i]).append("\n");
        if (pasteLines.length == 1) {
            full.append(before).append(pasteLines[0]).append(after);
            cursorCol = before.length() + pasteLines[0].length();
            for (int i = cursorLine + 1; i < lines.length; i++) full.append("\n").append(lines[i]);
        } else {
            full.append(before).append(pasteLines[0]).append("\n");
            for (int i = 1; i < pasteLines.length - 1; i++) full.append(pasteLines[i]).append("\n");
            String last = pasteLines[pasteLines.length - 1];
            full.append(last).append(after);
            cursorLine += pasteLines.length - 1;
            cursorCol = last.length();
            for (int i = cursorLine - (pasteLines.length - 2); i < lines.length; i++)
                if (i > cursorLine - (pasteLines.length - 1)) full.append("\n").append(lines[i]);
        }
        codeContent = new StringBuilder(full.toString());
        resetCursorBlink();
    }

    // --- AUTO-CLOSE ---
    private String getAutoClose(String ch) {
        return switch (ch) {
            case "(" -> ")";
            case "[" -> "]";
            case "{" -> "}";
            case "\"" -> "\"";
            case "'" -> "'";
            default -> null;
        };
    }

    private boolean isAutoClosePair(char open, char close) {
        return (open == '(' && close == ')') ||
               (open == '[' && close == ']') ||
               (open == '{' && close == '}') ||
               (open == '"' && close == '"') ||
               (open == '\'' && close == '\'');
    }

    // --- TOGGLE COMMENT (Ctrl+/) ---
    private void toggleLineComment(String[] lines) {
        String line = lines[cursorLine];
        String stripped = line.stripLeading();
        int indent = line.length() - stripped.length();
        if (stripped.startsWith("// ")) {
            lines[cursorLine] = " ".repeat(indent) + stripped.substring(3);
        } else if (stripped.startsWith("//")) {
            lines[cursorLine] = " ".repeat(indent) + stripped.substring(2);
        } else {
            lines[cursorLine] = " ".repeat(indent) + "// " + stripped;
        }
        updateCodeContent(lines);
    }

    private void removeLine(String[] lines, int index) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<lines.length; i++) {
            if(i == index) continue;
            sb.append(lines[i]).append(i == lines.length - 1 || (i == lines.length - 2 && index == lines.length - 1) ? "" : "\n");
        }
        codeContent = new StringBuilder(sb.toString());
    }

    private void insertLine(String[] lines, int index, String content) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<lines.length; i++) {
            sb.append(lines[i]);
            if(i == index - 1) sb.append("\n").append(content);
            if(i < lines.length - 1) sb.append("\n");
        }
        codeContent = new StringBuilder(sb.toString());
    }

    private void resetCursorBlink() {
        showCursor = true;
        lastBlinkTime = glfwGetTime();
    }

    private void handleEditorClick(double mx, double my) {
        float contentX = inspectorWidth + 20 + 50; // ← FIX
        float contentY = MENU_HEIGHT + TAB_HEIGHT + 20;
        float lineH = 22.0f;

        String[] lines = codeContent.toString().split("\n", -1);
        int clickedLine = (int) ((my - contentY + codeScrollY) / lineH);

        if (clickedLine >= 0 && clickedLine < lines.length) {
            cursorLine = clickedLine;
            nvgFontFace(nvg, "sans");
            nvgFontSize(nvg, 14.0f);

            int bestCol = 0;
            float minDiff = Float.MAX_VALUE;

            for (int c = 0; c <= lines[cursorLine].length(); c++) {
                String sub = lines[cursorLine].substring(0, c).replace("\t", "    ");
                float[] bounds = new float[4];
                nvgTextBounds(nvg, contentX, 0, sub, bounds);
                float charX = (sub.isEmpty()) ? contentX : bounds[2];
                float diff = Math.abs((float)mx - charX);

                if (diff < minDiff) {
                    minDiff = diff;
                    bestCol = c;
                }
            }
            cursorCol = bestCol;
            resetCursorBlink();
        }
    }

    private void updateCodeContent(String[] lines) {
        codeContent.setLength(0);
        for (int i = 0; i < lines.length; i++) {
            codeContent.append(lines[i]).append(i == lines.length - 1 ? "" : "\n");
        }
    }

    private void update() {
        double currentTime = glfwGetTime();
        double prevMouseX = mouseX; // ← lưu trước khi update
        double prevMouseY = mouseY;

        try (MemoryStack stack = stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1), y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            mouseX = x.get(0);
            mouseY = y.get(0);
        }

        boolean mousePressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean mouseJustPressed = mousePressed && !mousePressedLastFrame;
        boolean mouseDown = mousePressed; // alias rõ ràng hơn
        boolean cursorLocked = glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;

        // --- CODE EDITOR CLICK ---
        if (currentTab == EditorTab.CODE_EDITOR && mouseJustPressed) {
            if (mouseX > inspectorWidth + 20 && mouseY > MENU_HEIGHT + TAB_HEIGHT) {
                handleEditorClick(mouseX, mouseY);
            }
        }
        
     // --- NODE EDITOR INPUT (FIXED FOR ZOOM & BASENODE) ---
        if (currentTab == EditorTab.NODE_EDITOR) {
            float neX = inspectorWidth + 20;
            float neY = MENU_HEIGHT + TAB_HEIGHT;

            boolean middlePressed = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS;
            boolean rightPressed  = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

            // 1. Pan hệ thống (Offset tính theo Screen Space)
            if ((middlePressed || rightPressed) && isMouseOverD(neX, neY, lastViewportW, lastViewportH)) {
                if (!neIsPanning) { 
                    neIsPanning = true; 
                    nePanStartX = (float)mouseX; 
                    nePanStartY = (float)mouseY; 
                }
                neOffsetX += (float)(mouseX - prevMouseX);
                neOffsetY += (float)(mouseY - prevMouseY);
            } else {
                neIsPanning = false;
            }

            // 2. LEFT CLICK: Xử lý chọn Node hoặc Pin
            if (mouseJustPressed && isMouseOverD(neX, neY + 32, lastViewportW, lastViewportH - 32)) {
                boolean hitSomething = false;
                
                // Duyệt ngược danh sách để ưu tiên Node nằm trên cùng (Top-most)
                for (int i = nodeEditorData.nodes.size() - 1; i >= 0; i--) {
                    BaseNode n = nodeEditorData.nodes.get(i);
                    
                    // Chuyển đổi tọa độ Node từ World sang Screen để check va chạm chuột
                    float nx = (n.x * neScale) + neOffsetX + neX;
                    float ny = (n.y * neScale) + neOffsetY + neY;
                    float nw = n.w * neScale;
                    float nh = n.h * neScale;

                    // --- Check va chạm Pin ---
                    for (NodeEditor.Pin pin : n.pins) {
                        float px = pin.isOutput ? nx + nw : nx;
                        float py = ny + (pin.relativeY * neScale);
                        
                        float dx = (float)mouseX - px, dy = (float)mouseY - py;
                        float hitRadius = Math.max(10 * neScale, 8.0f); 
                        
                        if (Math.sqrt(dx*dx + dy*dy) < hitRadius) {
                            neActivePin = pin; 
                            neActivePinNode = n;
                            // Xóa kết nối cũ nếu kéo từ Pin đang có dây (tùy logic bạn muốn)
                            nodeEditorData.connections.removeIf(c ->
                                pin.isOutput ? c.fromId.equals(n.id) : c.toId.equals(n.id));
                            hitSomething = true; break;
                        }
                    }
                    if (hitSomething) break;

                    // --- Check va chạm thân Node (Drag) ---
                    if (mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh) {
                        // TÍNH TOẠ ĐỘ LOCAL (Dùng để check click trúng widget bên trong node)
                        float lx = (float)((mouseX - nx) / neScale);
                        float ly = (float)((mouseY - ny) / neScale);

                        // KIỂM TRA WIDGET TRƯỚC
                        if (n instanceof SetPositionNode) {
                            if (ly >= 40 && ly <= 58) { // Giả sử vùng click của ô X
                                 System.out.println("Focus vào ô nhập liệu X");
                                 // Ngắt không cho Drag node nếu đang tương tác với widget
                                 hitSomething = true;
                                 break; 
                            }
                        }

                        // NẾU KHÔNG TRÚNG WIDGET -> THỰC HIỆN DRAG NODE NHƯ CŨ
                        neDraggingNode = n;
                        neDragStartX = (float)mouseX - nx;
                        neDragStartY = (float)mouseY - ny;
                        
                        nodeEditorData.nodes.remove(i);
                        nodeEditorData.nodes.add(n);
                        hitSomething = true;
                        break;
                    }
                }

                // --- Toolbar Buttons ---
                float tbX = neX + 8, tbY = neY + 5;
                if (isMouseOverD(tbX, tbY, 90, 22)) {
                    // Khi thêm Node mới, cần tạo một Class cụ thể (ví dụ EventTickNode)
                    // Hoặc một Node mặc định nếu bạn chưa tạo các class con
                    String newId = String.valueOf(System.currentTimeMillis());
                    float spawnX = ((float)mouseX - neX - neOffsetX) / neScale;
                    float spawnY = ((float)mouseY - neY - neOffsetY) / neScale;
                    
                    // Ở đây bạn nên dùng một Factory hoặc Menu để chọn loại Node
                    // Tạm thời mình giả định bạn dùng một GenericNode hoặc TickNode
                    nodeEditorData.nodes.add(new EventTickNode(newId, spawnX, spawnY));
                }
            }

            // 3. DRAG NODE: Cập nhật vị trí trong World Space (Chia cho neScale)
            if (mousePressed && neDraggingNode != null) {
                neDraggingNode.x = ((float)mouseX - neOffsetX - neX - neDragStartX) / neScale;
                neDraggingNode.y = ((float)mouseY - neOffsetY - neY - neDragStartY) / neScale;
            }

            // 4. RELEASE: Thả chuột để tạo kết nối
            if (!mousePressed) {
                if (neActivePin != null) {
                    for (BaseNode n : nodeEditorData.nodes) {
                        if (n == neActivePinNode) continue;

                        float nx = (n.x * neScale) + neOffsetX + neX;
                        float ny = (n.y * neScale) + neOffsetY + neY;
                        float nw = n.w * neScale;

                        for (NodeEditor.Pin pin : n.pins) {
                            // Logic: Input nối với Output, không cùng loại
                            if (pin.isOutput == neActivePin.isOutput) continue;

                            float px = pin.isOutput ? nx + nw : nx;
                            float py = ny + (pin.relativeY * neScale);

                            float dx = (float)mouseX - px, dy = (float)mouseY - py;
                            
                            if (Math.sqrt(dx*dx + dy*dy) < Math.max(15 * neScale, 10)) {
                                String from = neActivePin.isOutput ? neActivePinNode.id : n.id;
                                String to   = neActivePin.isOutput ? n.id : neActivePinNode.id;
                                
                                nodeEditorData.connections.add(new NodeEditor.Connection(from, to));
                                // autoSave(); // Thực hiện lưu sau khi nối
                                break;
                            }
                        }
                    }
                }
                neDraggingNode = null; neActivePin = null; neActivePinNode = null;
            }
        }
        
        // --- CONSOLE BUTTONS ---
        if (currentTab == EditorTab.CONSOLE && mouseJustPressed) {
            float x = lastViewportX, y = lastViewportY;
            float w = lastViewportW;
            float bx = x + w - 10;

            // Run
            bx -= 70;
            if (isMouseOverD(bx, y + 5, 65, 24)) {
                BuildSystem.getInstance().setLogListener(line -> {
                    consoleLines.add(line);
                    consoleAutoScroll = true;
                });
                new Thread(() -> {
                    if (BuildSystem.getInstance().build()) {
                        runningProcess = BuildSystem.getInstance().runGame();
                    }
                }, "BuildAndRun").start();
            }
            bx -= 75;
            // Build
            if (isMouseOverD(bx, y + 5, 70, 24)) {
                BuildSystem.getInstance().setLogListener(line -> {
                    consoleLines.add(line);
                    consoleAutoScroll = true;
                });
                new Thread(() -> BuildSystem.getInstance().build(), "Build").start();
            }
            bx -= 70;
            // Pack
            if (isMouseOverD(bx, y + 5, 65, 24)) {
                BuildSystem.getInstance().setLogListener(line -> {
                    consoleLines.add(line);
                    consoleAutoScroll = true;
                });
                new Thread(() -> BuildSystem.getInstance().packGame(), "Pack").start();
            }
            bx -= 70;
            // Clear
            if (isMouseOverD(bx, y + 5, 65, 24)) {
                consoleLines.clear();
                consoleScrollY = 0;
            }
        }

        // --- SCENE 3D ---
        if (currentTab == EditorTab.SCENE_3D) {
            editorCore.update();

            GizmoRenderer gizmo = editorCore.getGizmo();
            boolean gizmoHit = false;

            // --- GIZMO DRAG --- phải check TRƯỚC raycast
            if (selectedNodeIndex >= 0 && selectedNodeIndex < sceneNodes.size()) {
                SceneNodeEntry e = sceneNodes.get(selectedNodeIndex);

                float relX = (float) (mouseX - lastViewportX);
                float relY = (float) (mouseY - lastViewportY);

                if (mouseJustPressed && !cursorLocked && !isDraggingInspector) {
                    int hit = gizmo.hitTest(e.node, editorCore.getEditorCamera(),
                            relX, relY, lastViewportW, lastViewportH);
                    if (hit >= 0) {
                        gizmo.startDrag(hit, relX, relY, e.node);
                        gizmoHit = true; // ← block raycast
                    }
                }

                if (mousePressed && gizmo.isDragging()) {
                    gizmo.updateDrag(relX, relY, e.node);
                    gizmoHit = true;
                }

                if (!mousePressed) gizmo.endDrag();
            }

            // --- RAYCAST --- chỉ khi KHÔNG hit gizmo
            if (mouseJustPressed && !cursorLocked && !isDraggingInspector && !gizmoHit) {
                if (mouseX >= lastViewportX && mouseX <= lastViewportX + lastViewportW
                        && mouseY >= lastViewportY && mouseY <= lastViewportY + lastViewportH) {
                    float relX = (float) (mouseX - lastViewportX);
                    float relY = (float) (mouseY - lastViewportY);
                    performRaycast(relX, relY);
                }
            }

            // --- CROSSHAIR SELECT ---
            if (crosshairSelectRequested) {
                crosshairSelectRequested = false;
                if (cursorLocked) performRaycast(lastViewportW / 2f, lastViewportH / 2f);
            }

            // --- GIZMO TOOLBAR ---
            if (mouseJustPressed) {
                float tbX = lastViewportX + 10, tbY = lastViewportY + 10;
                GizmoRenderer.GizmoMode[] modes = {
                        GizmoRenderer.GizmoMode.TRANSLATE,
                        GizmoRenderer.GizmoMode.ROTATE,
                        GizmoRenderer.GizmoMode.SCALE
                };
                for (int i = 0; i < 3; i++) {
                    if (isMouseOverD(tbX + i * 36, tbY, 32, 28)) {
                        gizmoMode = modes[i];
                        gizmo.setMode(gizmoMode);
                        break;
                    }
                }
            }
        }

        // logic ui từ đây
        // --- MENU BAR CLICK ---
        if (mouseJustPressed) {
            // Click vào menu header
            float curX = 8.0f;
            boolean clickedHeader = false;
            for (int i = 0; i < menuItems.length; i++) {
                float itemW = measureText(menuItems[i]) + 20;
                if (isMouseOverD(curX, 0, itemW, MENU_HEIGHT)) {
                    activeMenuIndex = (activeMenuIndex == i) ? -1 : i;
                    clickedHeader = true;
                    break;
                }
                curX += itemW;
            }

            // Click vào dropdown item
            if (!clickedHeader && activeMenuIndex >= 0) {
                String[] items = MENU_ITEMS_SUB[activeMenuIndex];
                float dropX = getMenuX(activeMenuIndex);
                float dropY = MENU_HEIGHT;
                float itemH = 26;
                float iy = dropY + 4;

                for (String label : items) {
                    if (!label.startsWith("─") && isMouseOverD(dropX + 2, iy, 196, itemH)) {
                        handleMenuAction(activeMenuIndex, label);
                        activeMenuIndex = -1;
                        break;
                    }
                    iy += itemH;
                }

                // Click ngoài dropdown → đóng
                if (!isMouseOverD(0, 0, width, MENU_HEIGHT + items.length * 26 + 8)) {
                    activeMenuIndex = -1;
                }
            }
        }

        // --- SIDE TAB CLICK ---
        if (mouseJustPressed) {
            float treeW = inspectorWidth * 0.55f;
            float mtW = (treeW - 5) / 3f;
            float mtY = MENU_HEIGHT + 10 + 30;
            for (int i = 0; i < 3; i++) {
                if (isMouseOverD(10 + i * mtW, mtY, mtW, 24)) {
                    sideTab = i;
                    if (i == 1) refreshProjectFiles();
                    if (i == 2) refreshLibraries();
                    break;
                }
            }
        }

        // --- FILE TREE CLICK ---
        if (mouseJustPressed && sideTab == 1) {
            float treeW = inspectorWidth * 0.55f;
            float contentY = MENU_HEIGHT + 10 + 54; // header 30 + mini tabs 24
            float itemH = 22;

            for (int i = 0; i < fileTree.size(); i++) {
                FileTreeEntry entry = fileTree.get(i);
                float iy = contentY + 4 + i * itemH - fileExplorerScrollY;

                if (isMouseOverD(10, iy, treeW - 5, itemH)) {
                    if (entry.isDir) {
                        if (expandedDirs.contains(entry.path))
                            expandedDirs.remove(entry.path);
                        else
                            expandedDirs.add(entry.path);
                        refreshProjectFiles();
                    } else {
                        selectedFile = entry.path;
                        String name = entry.path.getFileName().toString();
                        if (name.endsWith(".java")) {
                            try {
                                String content = java.nio.file.Files.readString(entry.path);
                                selectedFile = entry.path; // Gán ngay lập tức

                                // LUÔN LUÔN nạp nội dung vào Code Editor trước để dự phòng
                                codeContent = new StringBuilder(content);
                                clearSelection();
                                cursorLine = 0;
                                cursorCol = 0;

                                if (content.contains("extends BaseScene")) {
                                    // Logic tìm Scene trong project
                                    var scenes = editorCore.getEditorScene().getDetectedScenes();
                                    var match = scenes.stream()
                                            .filter(s -> s.sourceFile.equals(entry.path))
                                            .findFirst().orElse(null);

                                    if (match != null) {
                                        var scene = editorCore.getEditorScene().instantiateScene(match);
                                        if (scene != null) {
                                            // Nếu load được 3D thì nhảy qua 3D
                                            editorCore.loadScene(scene);
                                            currentTab = EditorTab.SCENE_3D;
                                            statusMessage = "Previewing: " + match.simpleName;
                                        } else {
                                            // Nếu chưa Build -> Ở lại Code Editor và báo lỗi
                                            currentTab = EditorTab.CODE_EDITOR;
                                            statusMessage = "Build required to preview " + match.simpleName;
                                        }
                                    } else {
                                        // File Scene mới chưa kịp scan -> Ở lại Editor để user code tiếp
                                        editorCore.getEditorScene().scanProjectScenes();
                                        currentTab = EditorTab.CODE_EDITOR;
                                    }
                                } else {
                                    // File Java thường -> Chắc chắn vào Code Editor
                                    currentTab = EditorTab.CODE_EDITOR;
                                }
                                statusTimer = glfwGetTime();
                            } catch (Exception e) {
                                statusMessage = "Error loading file: " + e.getMessage();
                            }
                        }
                    }
                    break;
                }
            }
        }

        // --- INSPECTOR INPUT ---
        if (selectedNodeIndex >= 0 && selectedNodeIndex < sceneNodes.size()) {
            SceneNodeEntry e = sceneNodes.get(selectedNodeIndex);
            if (e.node != null) {
                // Dùng ĐÚNG tọa độ như trong render()
                float treeW = inspectorWidth * 0.55f;
                float inspW = inspectorWidth * 0.45f - 5;
                float ix = treeW + 15; // x của drawNodeInspector
                float iy = MENU_HEIGHT + 10; // y của drawNodeInspector
                float pad = 10;

                // cy bắt đầu giống drawNodeInspector
                float cy = iy + 38 // header 32px + gap
                        + 54; // node header block

                // section header "TRANSFORM" = 26px
                cy += 26;

                // Vec3 drag — Position/Rotation/Scale
                String[] labels = { "pos", "rot", "scale" };

                for (int li = 0; li < 3; li++) {
                    float fw = (inspW - pad * 2 - 6) / 3f;
                    for (int ai = 0; ai < 3; ai++) {
                        float fx = ix + pad + ai * (fw + 3);
                        float fy = cy + 18;

                        if (mouseJustPressed && isMouseOverD(fx, fy, fw, 20)) {
                            draggingVec3Node = selectedNodeIndex;
                            draggingVec3Axis = ai;
                            draggingVec3Label = labels[li];
                        }
                    }
                    cy += 46;
                    if (li < 2) cy += 4; // gap giữa rows
                }

                // Apply drag — dùng mousePressed KHÔNG phải mouseJustPressed
                if (draggingVec3Node == selectedNodeIndex
                        && mousePressed && draggingVec3Axis >= 0) {
                    float delta = (float) (mouseX - prevMouseX) * 0.05f;
                    int ai = draggingVec3Axis;
                    switch (draggingVec3Label) {
                        case "pos" -> {
                            if (ai == 0) e.node.position.x += delta;
                            else if (ai == 1) e.node.position.y += delta;
                            else e.node.position.z += delta;
                        }
                        case "rot" -> {
                            if (ai == 0) e.node.rotation.x += delta;
                            else if (ai == 1) e.node.rotation.y += delta;
                            else e.node.rotation.z += delta;
                        }
                        default -> {
                            if (ai == 0) e.node.scale.x += delta;
                            else if (ai == 1) e.node.scale.y += delta;
                            else e.node.scale.z += delta;
                        }
                    }
                }
                if (!mousePressed) {
                    draggingVec3Node = -1;
                    draggingVec3Axis = -1;
                }

                // Reset Transform button
                cy += 8; // gap trước button
                float resetW = (inspW - pad * 2 - 4) / 2f;
                if (mouseJustPressed && isMouseOverD(ix + pad, cy, resetW, 24)) {
                    e.node.position.set(0, 0, 0);
                    e.node.rotation.set(0, 0, 0);
                    e.node.scale.set(1, 1, 1);
                }

                // Focus Camera button
                if (mouseJustPressed && isMouseOverD(ix + pad + resetW + 4, cy, resetW, 24)) {
                    editorCore.focusCameraOnNode(e.node);
                }
                cy += 32;

                // Section "NODE" = 26px
                cy += 26;

                // Visible toggle
                float tx = ix + inspW - 10 - 36, ty2 = cy + 4;
                if (mouseJustPressed && isMouseOverD(tx, ty2, 32, 16)) {
                    e.visible = !e.visible;
                    if (!e.visible) e.node.scale.set(0, 0, 0);
                    else e.node.scale.set(1, 1, 1);
                }
            }
        }

        // --- INSPECTOR RESIZE ---
        float resizeHandleX = inspectorWidth + 10;
        boolean onResizeHandle = Math.abs(mouseX - resizeHandleX) < 5 && mouseY > MENU_HEIGHT;

        if (onResizeHandle || isDraggingInspector) {
            glfwSetCursor(window, glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR));
        } else {
            glfwSetCursor(window, NULL);
        }

        if (mouseJustPressed && onResizeHandle) isDraggingInspector = true;
        if (!mousePressed) isDraggingInspector = false;
        if (isDraggingInspector) {
            inspectorWidth = (float) Math.max(INSPECTOR_MIN_WIDTH,
                    Math.min(INSPECTOR_MAX_WIDTH, mouseX - 10));
        }

        if (!statusMessage.isEmpty() && (currentTime - statusTimer > 2.0))
            statusMessage = "";
        if (currentTime - lastTitleUpdate >= 0.5) {
            String pName = ProjectManager.getInstance().isProjectLoaded()
                    ? ProjectManager.getInstance().getProjectName()
                    : (projectName != null ? projectName : "No Project");
            String status = statusMessage.isEmpty() ? "" : " - " + statusMessage;
            glfwSetWindowTitle(window,
                    "Yanase Studio v0.0.1 - " + pName + status);
            fpsCounter = 0;
            lastTitleUpdate = currentTime;
        }
        lastMouseXForDrag = mouseX;
        mousePressedLastFrame = mousePressed; // ← luôn ở CUỐI cùng
    }
    
    private float getMenuX(int idx) {
        float x = 8.0f;
        for (int i = 0; i < idx; i++) x += measureText(menuItems[i]) + 20;
        return x;
    }
    
    private void drawAboutWindow(long vg) {
        if (!showAbout) return;

        // Center window
        float x = (windowWidth - 400) / 2;
        float y = (windowHeight - 300) / 2;

        // Background & Shadow
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, 400, 300, 5);
        nvgFillColor(vg, nvgRGBA(30, 30, 30, 240, color));
        nvgFill(vg);

        // Header - Yanase Studio
        nvgFontSize(vg, 24.0f);
        nvgFontFace(vg, "sans-bold");
        nvgFillColor(vg, nvgRGBA(0, 167, 255, 255, color));
        nvgText(vg, x + 100, y + 50, "YANASE STUDIO");

        // Content
        nvgFontSize(vg, 16.0f);
        nvgFillColor(vg, nvgRGBA(200, 200, 200, 255, color));
        nvgText(vg, x + 40, y + 100, "Version: 0.0.1 (Alpha)");
        nvgText(vg, x + 40, y + 130, "Developer: PmgTeam (Pmg)");
        nvgText(vg, x + 40, y + 160, "Engine: Lotus2D Framework");

        // Nút Close (Sử dụng logic check mouse click cơ bản)
        drawNanoButton(vg, "OK", x + 300, y + 250, 70, 30, () -> showAbout = false);
    }
    
    private boolean drawNanoButton(long vg, String text, float x, float y, float w, float h, Runnable onClick) {
        // Kiểm tra chuột có nằm trong vùng button không
        boolean hovered = isMouseInRect(x, y, w, h);
        boolean clicked = hovered && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;

        // Vẽ Background Button
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 4);
        if (clicked) {
            nvgFillColor(vg, nvgRGBA(0, 120, 215, 255, color)); // Màu khi nhấn
        } else if (hovered) {
            nvgFillColor(vg, nvgRGBA(60, 60, 60, 255, color));  // Màu khi hover
        } else {
            nvgFillColor(vg, nvgRGBA(45, 45, 45, 255, color));  // Màu bình thường
        }
        nvgFill(vg);

        // Vẽ Border
        nvgStrokeWidth(vg, 1.0f);
        nvgStrokeColor(vg, nvgRGBA(100, 100, 100, 255, color));
        nvgStroke(vg);

        // Vẽ Text căn giữa
        nvgFontSize(vg, 15.0f);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, nvgRGBA(255, 255, 255, 255, color));
        nvgText(vg, x + w / 2, y + h / 2, text);

        // Xử lý sự kiện click (nên dùng một cờ để tránh spam click liên tục)
        if (clicked) {
            if (onClick != null) onClick.run();
            return true;
        }
        return false;
    }
    
    private void drawSettingRow(long vg, String label, String value, float x, float y, float width, String fieldId) {
        // Vẽ Label
        nvgFontSize(vg, 16.0f);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(vg, rgba(180, 180, 180, 255));
        nvgText(vg, x, y, label);

        float inputW = 120;
        float inputX = x + width - inputW;
        boolean isFocused = focusField.equals(fieldId);
        boolean hovered = isMouseInRect(inputX, y - 12, inputW, 24);

        // Xử lý Click để lấy Focus
        if (hovered && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            focusField = fieldId;
        }

        // Vẽ Input Box
        nvgBeginPath(vg);
        nvgRoundedRect(vg, inputX, y - 12, inputW, 24, 3);
        nvgFillColor(vg, rgba(20, 20, 20, 255));
        nvgFill(vg);
        
        // Vẽ viền highlight nếu đang Focus hoặc Hover
        nvgStrokeWidth(vg, 1.0f);
        nvgStrokeColor(vg, isFocused ? rgba(0, 167, 255, 255) : (hovered ? rgba(100, 100, 100, 255) : rgba(50, 50, 50, 255)));
        nvgStroke(vg);
        
        // Hiển thị text
        nvgFillColor(vg, isFocused ? rgba(255, 255, 255, 255) : rgba(0, 167, 255, 255));
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(vg, inputX + inputW / 2, y, value + (isFocused && (glfwGetTime() % 1.0 > 0.5) ? "|" : ""));
    }
    
    private boolean drawNanoCheckbox(long vg, float x, float y, boolean checked) {
        float size = 18;
        boolean hovered = isMouseInRect(x, y, size, size);
        
        // Box
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, size, size, 3);
        nvgFillColor(vg, nvgRGBA(30, 30, 30, 255, color));
        nvgFill(vg);
        nvgStrokeColor(vg, hovered ? nvgRGBA(0, 167, 255, 255, color) : nvgRGBA(100, 100, 100, 255, color));
        nvgStroke(vg);

        // Dấu tích nếu checked = true
        if (checked) {
            nvgBeginPath(vg);
            nvgRoundedRect(vg, x + 4, y + 4, size - 8, size - 8, 1);
            nvgFillColor(vg, nvgRGBA(0, 167, 255, 255, color));
            nvgFill(vg);
        }

        // Logic click (trả về true nếu người dùng vừa click)
        return hovered && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }
    
    private NVGColor nvgRGBA(int r, int g, int b, int a, NVGColor out) {
        out.r((byte) r);
        out.g((byte) g);
        out.b((byte) b);
        out.a((byte) a);
        return out;
    }
    
    private boolean isMouseInRect(float x, float y, float w, float h) {
        DoubleBuffer xpos = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xpos, ypos);
        double mx = xpos.get(0);
        double my = ypos.get(0);
        return (mx >= x && mx <= x + w && my >= y && my <= y + h);
    }
    
    private void drawGameSettingsWindow(long vg) {
        if (!showGameSettings) return;

        float x = (windowWidth - 500) / 2;
        float y = (windowHeight - 400) / 2;
        float w = 500, h = 400;

        // Window Base
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, w, h, 8);
        nvgFillColor(vg, nvgRGBA(40, 40, 40, 255, color));
        nvgFill(vg);

        // Title
        nvgFontSize(vg, 20.0f);
        nvgText(vg, x + 20, y + 35, "GAME CONFIGURATION");

        // Option: Resolution
        drawSettingRow(vg, "Resolution Width", resWidth, x + 20, y + 80, 460, "width");
        drawSettingRow(vg, "Resolution Height", resHeight, x + 20, y + 120, 460, "height");

        // Option: VSync (Toggle)
        nvgText(vg, x + 20, y + 160, "Enable VSync");
        boolean vsync = (boolean) ProjectManager.getInstance().getGameConfig().get("vsync");
        if (drawNanoCheckbox(vg, x + 400, y + 145, vsync)) {
            ProjectManager.getInstance().getGameConfig().put("vsync", !vsync);
            // Theo yêu cầu: Tự động lưu khi thay đổi
            ProjectManager.getInstance().saveProject(); 
        }

        // Save & Close
        if (drawNanoButton(vg, "CLOSE", x + w - 100, y + h - 50, 80, 30, () -> showGameSettings = false)) {
            ProjectManager.getInstance().saveProject();
        }
    }

    private void handleMenuAction(int menu, String action) {
        if (action.contains("───")) return; // Chặn các đường kẻ ngang

        switch (action) {
            // --- FILE ---
            case "New Project" -> {
                codeContent = new StringBuilder();
                nodeEditorData.nodes.clear();
                nodeEditorData.connections.clear();
                statusMessage = "Created new project"; statusTimer = glfwGetTime();
            }
            case "Open Project..." -> {
                new Thread(() -> {
                    java.awt.FileDialog fd = new java.awt.FileDialog((java.awt.Frame)null, "Open Project", java.awt.FileDialog.LOAD);
                    fd.setFilenameFilter((d, n) -> n.endsWith(".ygp"));
                    fd.setVisible(true);
                    if (fd.getFile() != null) {
                        ProjectManager.getInstance().loadProject(fd.getDirectory() + fd.getFile());
                        String content = ProjectManager.getInstance().getActiveSourceContent();
                        if (content != null && !content.isEmpty()) {
                            codeContent = new StringBuilder(content);
                            cursorLine = 0; cursorCol = 0;
                        }
                    }
                }).start();
            }
            case "Save Project" -> {
                ProjectManager.getInstance().setActiveSourceContent(codeContent.toString());
                ProjectManager.getInstance().saveProject();
                statusMessage = "Project saved!"; statusTimer = glfwGetTime();
            }
            case "Save As..." -> {
                new Thread(() -> {
                    java.awt.FileDialog fd = new java.awt.FileDialog((java.awt.Frame)null, "Save Project As", java.awt.FileDialog.SAVE);
                    fd.setFile("project.ygp");
                    fd.setVisible(true);
                    if (fd.getFile() != null) ProjectManager.getInstance().saveProjectAs(fd.getDirectory() + fd.getFile());
                }).start();
            }
            case "Exit" -> glfwSetWindowShouldClose(window, true);

            case "Settings" -> showSettings = true; // Cửa sổ cài đặt IDE
            case "Project Settings" -> showGameSettings = true; // Cửa sổ cài đặt Project/Game
            case "About Yanase Studio" -> showAbout = true;
            // --- EDIT ---
            case "Undo" -> popUndo();
            case "Cut" -> {
                if (currentTab == EditorTab.CODE_EDITOR && hasSelection) {
                    glfwSetClipboardString(window, getSelectedText());
                    deleteSelection();
                }
            }
            case "Copy" -> {
                if (currentTab == EditorTab.CODE_EDITOR && hasSelection) {
                    glfwSetClipboardString(window, getSelectedText());
                }
            }
            case "Paste" -> {
                if (currentTab == EditorTab.CODE_EDITOR) {
                    String clip = glfwGetClipboardString(window);
                    if (clip != null) insertTextAtCursor(clip);
                }
            }
            case "Select All" -> {
                if (currentTab == EditorTab.CODE_EDITOR) {
                    String[] lines = codeContent.toString().split("\n", -1);
                    selectAll(lines);
                }
            }

            // --- PROJECT ---
            case "Scan Scenes" -> {
                editorCore.getEditorScene().scanProjectScenes();
                statusMessage = "Scenes scanned!"; statusTimer = glfwGetTime();
            }
            case "Open Project Folder" -> {
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(ProjectManager.getInstance().getProjectDir()));
                } catch (Exception ignored) {}
            }
            case "Reload" -> {
                ProjectManager.getInstance().reloadFromDisk();
                String content = ProjectManager.getInstance().getActiveSourceContent();
                if (content != null) codeContent = new StringBuilder(content);
                statusMessage = "Project reloaded!"; statusTimer = glfwGetTime();
            }

            // --- BUILD ---
            case "Build" -> startBuildThread(() -> BuildSystem.getInstance().build());
            case "Run" -> startBuildThread(() -> {
                if (BuildSystem.getInstance().build()) BuildSystem.getInstance().runGame();
            });
            case "Build & Run" -> handleMenuAction(menu, "Run");
            case "Pack Game" -> startBuildThread(() -> BuildSystem.getInstance().packGame());
            case "Clean Build" -> handleCleanBuild();

            // --- HELP ---
            case "Documentation" -> openURL("https://github.com/PmgTeam/modcoderpack-redevelop");
            case "Report Bug" -> openURL("https://github.com/PmgTeam/modcoderpack-redevelop/issues");
        }
    }

    // --- CÁC HÀM PHỤ TRỢ (HELPERS) ĐỂ FIX LỖI COMPILE ---

    private void startBuildThread(Runnable task) {
        BuildSystem.getInstance().setLogListener(line -> consoleLines.add(line));
        currentTab = EditorTab.CONSOLE;
        new Thread(task, "BuildThread").start();
    }

    private void handleCleanBuild() {
        try {
            String buildDir = ProjectManager.getInstance().getProjectDir() + "/build/classes";
            java.nio.file.Path path = java.nio.file.Paths.get(buildDir);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.walk(path).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { java.nio.file.Files.delete(p); } catch (Exception ignored) {} });
            }
            statusMessage = "Build cleaned!"; statusTimer = glfwGetTime();
        } catch (Exception ignored) {}
    }

    private void insertTextAtCursor(String text) {
        pushUndo();
        String[] lines = codeContent.toString().split("\n", -1);
        lines[cursorLine] = lines[cursorLine].substring(0, cursorCol) + text + lines[cursorLine].substring(cursorCol);
        cursorCol += text.length();
        updateCodeContent(lines);
        resetCursorBlink();
    }

    private void openURL(String url) {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); } catch (Exception ignored) {}
    }
    
    private boolean isMouseOverD(float x, float y, float w, float h) {
        if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) return false;
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
    
    private void performRaycast(float relX, float relY) {
        Object3D hit = editorCore.castRayFromScreen(relX, relY, lastViewportW, lastViewportH);
        System.out.println("[Raycast] relX=" + relX + " relY=" + relY 
                + " vw=" + lastViewportW + " vh=" + lastViewportH 
                + " hit=" + (hit != null ? hit.getNodeName() : "null"));
        if (hit != null) {
            for (int idx = 0; idx < sceneNodes.size(); idx++) {
                if (sceneNodes.get(idx).node == hit) {
                    selectedNodeIndex = idx;
                    editorCore.setSelectedNode(hit);
                    return;
                }
            }
        } else {
            selectedNodeIndex = -1;
            editorCore.setSelectedNode(null);
        }
    }

    private void drawCodeEditor(float x, float y, float w, float h) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, rgba(20, 20, 23, 255));
        nvgFill(nvg);

        nvgSave(nvg);
        nvgScissor(nvg, x, y, w, h);

        String[] lines = codeContent.toString().split("\n", -1);
        float lineH = 22.0f;

        if (glfwGetTime() - lastBlinkTime > 0.5) {
            showCursor = !showCursor;
            lastBlinkTime = glfwGetTime();
        }

        nvgFontFace(nvg, "sans");
        for (int i = 0; i < lines.length; i++) {
            float curY = y + 20 + (i * lineH) - codeScrollY;
            if (curY < y - 20 || curY > y + h + 20) continue;

            // Line number
            nvgFontSize(nvg, 12.0f);
            nvgFillColor(nvg, rgba(80, 80, 90, 255));
            nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
            nvgText(nvg, x + 35, curY, String.valueOf(i + 1));

            // Highlight dòng hiện tại (cursor line)
            if (i == cursorLine) {
                nvgBeginPath(nvg);
                nvgRect(nvg, x + 40, curY - lineH / 2, w - 40, lineH);
                nvgFillColor(nvg, rgba(40, 40, 50, 180));
                nvgFill(nvg);
            }
            
         // Thêm vào trong vòng for, TRƯỚC khi vẽ text:
            if (hasSelection) {
                normalizeSelection();
                if (i >= selectStartLine && i <= selectEndLine) {
                    String lineText2 = lines[i].replace("\t", "    ");
                    float selStartX, selEndX;

                    int colStart = (i == selectStartLine) ? selectStartCol : 0;
                    int colEnd = (i == selectEndLine) ? selectEndCol : lineText2.length();

                    float[] b1 = new float[4], b2 = new float[4];
                    String s1 = lineText2.substring(0, colStart);
                    String s2 = lineText2.substring(0, colEnd);
                    nvgTextBounds(nvg, x + 50, curY, s1, b1);
                    nvgTextBounds(nvg, x + 50, curY, s2, b2);
                    selStartX = s1.isEmpty() ? x + 50 : b1[2];
                    selEndX = s2.isEmpty() ? x + 50 : b2[2];

                    nvgBeginPath(nvg);
                    nvgRect(nvg, selStartX, curY - lineH / 2, selEndX - selStartX, lineH);
                    nvgFillColor(nvg, rgba(50, 100, 180, 120));
                    nvgFill(nvg);
                }
            }

            // Syntax highlighting
            nvgFontSize(nvg, 14.0f);
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            drawHighlightedLine(lines[i].replace("\t", "    "), x + 50, curY);

            // Cursor
         // Cursor
            if (i == cursorLine && showCursor) {
                int safeCol = Math.min(cursorCol, lines[i].length());
                String fullLine = lines[i].replace("\t", "    ");

                nvgFontSize(nvg, 14.0f);
                nvgFontFace(nvg, "sans");
                nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

                float cursorX = x + 50; // default: đầu dòng

                if (safeCol == 0) {
                    cursorX = x + 50;
                } else if (safeCol >= fullLine.length()) {
                    // Cursor ở cuối dòng → dùng maxx() của glyph cuối
                    if (!fullLine.isEmpty()) {
                        NVGGlyphPosition.Buffer glyphs = NVGGlyphPosition.malloc(fullLine.length());
                        int count = nvgTextGlyphPositions(nvg, x + 50, curY, fullLine, glyphs);
                        if (count > 0) cursorX = glyphs.get(count - 1).maxx();
                        glyphs.free();
                    }
                } else {
                    // Cursor ở giữa dòng → lấy x() của glyph tại đúng cursorCol
                    NVGGlyphPosition.Buffer glyphs = NVGGlyphPosition.malloc(fullLine.length());
                    int count = nvgTextGlyphPositions(nvg, x + 50, curY, fullLine, glyphs);
                    if (count > safeCol) {
                        cursorX = glyphs.get(safeCol).x(); // x() = cạnh trái glyph = sát ký tự tiếp theo
                    } else if (count > 0) {
                        cursorX = glyphs.get(count - 1).maxx();
                    }
                    glyphs.free();
                }

                nvgBeginPath(nvg);
                nvgMoveTo(nvg, cursorX, curY - 8);
                nvgLineTo(nvg, cursorX, curY + 8);
                nvgStrokeColor(nvg, rgba(255, 255, 255, 220));
                nvgStrokeWidth(nvg, 1.5f);
                nvgStroke(nvg);
                
                // System.out.println("cursorX=" + cursorX + " x+50=" + (x+50) + " diff=" + (cursorX - (x+50)));
            }
        }
        nvgRestore(nvg);
    }

    // --- Syntax Highlighting Engine ---
    private static final String[] KEYWORDS = {
        "public", "private", "protected", "static", "final", "abstract", "synchronized",
        "class", "interface", "enum", "extends", "implements", "new", "return", "void",
        "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
        "try", "catch", "finally", "throw", "throws", "import", "package", "this",
        "super", "null", "true", "false", "instanceof"
    };

    private static final String[] TYPES = {
        "int", "float", "double", "long", "short", "byte", "char", "boolean", "String",
        "var", "Object", "void"
    };

    private void drawHighlightedLine(String line, float startX, float y) {
        if (line.isEmpty()) return;

        // Parse tokens và màu sắc
        java.util.List<int[]> ranges = new java.util.ArrayList<>(); // [start, end, colorIndex]
        // 0=default, 1=keyword, 2=type, 3=string, 4=comment, 5=number, 6=annotation

        String trimmed = line.stripLeading();
        if (trimmed.startsWith("//")) {
            nvgFontSize(nvg, 14.0f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, rgba(106, 153, 85, 255));
            nvgText(nvg, startX, y, line);
            return;
        }

        int len = line.length();
        int i = 0;
        while (i < len) {
            char c = line.charAt(i);
            if (c == '"') {
                int end = line.indexOf('"', i + 1);
                if (end == -1) end = len - 1;
                ranges.add(new int[]{i, end + 1, 3});
                i = end + 1;
            } else if (c == '\'' && i + 2 < len && line.charAt(i + 2) == '\'') {
                ranges.add(new int[]{i, i + 3, 3});
                i += 3;
            } else if (c == '@') {
                int end = i + 1;
                while (end < len && Character.isLetterOrDigit(line.charAt(end))) end++;
                ranges.add(new int[]{i, end, 6});
                i = end;
            } else if (Character.isDigit(c)) {
                int end = i;
                while (end < len && (Character.isDigit(line.charAt(end)) 
                    || line.charAt(end) == '.' || line.charAt(end) == 'f' 
                    || line.charAt(end) == 'L')) end++;
                ranges.add(new int[]{i, end, 5});
                i = end;
            } else if (Character.isLetter(c) || c == '_') {
                int end = i;
                while (end < len && (Character.isLetterOrDigit(line.charAt(end)) 
                    || line.charAt(end) == '_')) end++;
                String token = line.substring(i, end);
                int colorIdx = isKeyword(token) ? 1 : isType(token) ? 2 
                             : Character.isUpperCase(token.charAt(0)) ? 2 : 0;
                ranges.add(new int[]{i, end, colorIdx});
                i = end;
            } else {
                ranges.add(new int[]{i, i + 1, 0});
                i++;
            }
        }

        // Lấy vị trí pixel chính xác từng glyph bằng GlyphPositions
        nvgFontSize(nvg, 14.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);

        NVGGlyphPosition.Buffer glyphs = NVGGlyphPosition.malloc(len);
        int count = nvgTextGlyphPositions(nvg, startX, y, line, glyphs);

        for (int[] range : ranges) {
            int s = range[0], e = Math.min(range[1], count), colorIdx = range[2];
            if (s >= count) continue;

            // Lấy x bắt đầu và kết thúc từ glyph positions
            float x0 = glyphs.get(s).x();
            float x1 = (e < count) ? glyphs.get(e).x() : glyphs.get(count - 1).maxx();

            // Set màu theo colorIdx
            switch (colorIdx) {
                case 1 -> nvgFillColor(nvg, rgba(86, 156, 214, 255));   // keyword
                case 2 -> nvgFillColor(nvg, rgba(78, 201, 176, 255));   // type/class
                case 3 -> nvgFillColor(nvg, rgba(206, 145, 120, 255));  // string
                case 4 -> nvgFillColor(nvg, rgba(106, 153, 85, 255));   // comment
                case 5 -> nvgFillColor(nvg, rgba(181, 206, 168, 255));  // number
                case 6 -> nvgFillColor(nvg, rgba(220, 220, 170, 255));  // annotation
                default -> nvgFillColor(nvg, rgba(200, 200, 210, 255)); // default
            }

            // Vẽ substring tại đúng vị trí glyph
            nvgText(nvg, x0, y, line.substring(s, Math.min(e, len)));
        }

        glyphs.free();
    }

    private boolean isKeyword(String word) {
        for (String k : KEYWORDS) if (k.equals(word)) return true;
        return false;
    }

    private boolean isType(String word) {
        for (String t : TYPES) if (t.equals(word)) return true;
        return false;
    }

    private float measureText(String text) {
        float[] bounds = new float[4];
        nvgFontSize(nvg, 14.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgTextBounds(nvg, 0, 0, text, bounds);
        return bounds[2] - bounds[0];
    }
    // ... Giữ các hàm init, loop, render, drawMenuBar, drawInspector, vv. từ code cũ ...
    
    private void saveCodeToFile(String path) {
        // Sync với ProjectManager trước
        ProjectManager.getInstance().setActiveSourceContent(codeContent.toString());
        ProjectManager.getInstance().saveProject();

        // Fallback save trực tiếp nếu chưa có project
        if (!ProjectManager.getInstance().isProjectLoaded()) {
            try {
                java.nio.file.Path p = Paths.get(path);
                if (!Files.exists(p.getParent())) Files.createDirectories(p.getParent());
                Files.writeString(p, codeContent.toString());
            } catch (Exception e) { e.printStackTrace(); }
        }
        statusMessage = "Saved!";
        statusTimer = glfwGetTime();
    }

    private NVGColor rgba(int r, int g, int b, int a) {
        colorBuf.r(r / 255.0f); colorBuf.g(g / 255.0f); colorBuf.b(b / 255.0f); colorBuf.a(a / 255.0f);
        return colorBuf;
    }

    private void render() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetWindowSize(window, w, h);
            width = w.get(0); height = h.get(0);
        }
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        float contentX = inspectorWidth + 20;
        float contentY = MENU_HEIGHT + TAB_HEIGHT;
        float contentW = width - contentX;
        float contentH = height - contentY;

        // Lưu lại để dùng cho raycast
        lastViewportX = contentX;
        lastViewportY = contentY;
        lastViewportW = contentW;
        lastViewportH = contentH;

        if (currentTab == EditorTab.SCENE_3D)    drawOpenGLWorld(contentX, contentY, contentW, contentH);
        if (currentTab == EditorTab.CONSOLE)     drawConsole(contentX, contentY, contentW, contentH);
        if (currentTab == EditorTab.NODE_EDITOR)
            drawNodeEditor(contentX, contentY, contentW, contentH);
        
        // Crosshair khi freecam
        if (currentTab == EditorTab.SCENE_3D
                && glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) {
            float cx = lastViewportX + lastViewportW / 2f;
            float cy = lastViewportY + lastViewportH / 2f;
            float s = 10f, g = 3f;

            nvgStrokeWidth(nvg, 1.5f);
            nvgStrokeColor(nvg, rgba(255, 255, 255, 200));

            // Ngang
            nvgBeginPath(nvg); nvgMoveTo(nvg, cx - s, cy); nvgLineTo(nvg, cx - g, cy); nvgStroke(nvg);
            nvgBeginPath(nvg); nvgMoveTo(nvg, cx + g, cy); nvgLineTo(nvg, cx + s, cy); nvgStroke(nvg);
            // Dọc
            nvgBeginPath(nvg); nvgMoveTo(nvg, cx, cy - s); nvgLineTo(nvg, cx, cy - g); nvgStroke(nvg);
            nvgBeginPath(nvg); nvgMoveTo(nvg, cx, cy + g); nvgLineTo(nvg, cx, cy + s); nvgStroke(nvg);
            // Chấm giữa
            nvgBeginPath(nvg); nvgCircle(nvg, cx, cy, 1.5f);
            nvgFillColor(nvg, rgba(255, 255, 255, 220)); nvgFill(nvg);

            // Hint
            nvgFontSize(nvg, 11.0f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_TOP);
            nvgFillColor(nvg, rgba(200, 200, 200, 140));
            nvgText(nvg, cx, cy + 14, "[F] Select node");
        }

        nvgBeginFrame(nvg, width, height, 1.0f);
        drawMenuBar();
        drawTabs(contentX, MENU_HEIGHT, contentW);
        if (currentTab == EditorTab.CODE_EDITOR) drawCodeEditor(contentX, contentY, contentW, contentH);
        // Trong render(), thay phần drawInspector cũ:
        float treeW = inspectorWidth * 0.55f;  // 55% bên trái = Scene Tree
        float inspW = inspectorWidth * 0.45f;  // 45% bên phải = Inspector
        // Gizmo mode toolbar
        // Chỉ vẽ gizmo toolbar khi đang ở SCENE_3D
        if (currentTab == EditorTab.SCENE_3D) {
            float tbX = lastViewportX + 10, tbY = lastViewportY + 10;
            String[] modes = {"T", "R", "S"};
            GizmoRenderer.GizmoMode[] modeVals = {
                GizmoRenderer.GizmoMode.TRANSLATE,
                GizmoRenderer.GizmoMode.ROTATE,
                GizmoRenderer.GizmoMode.SCALE
            };
            String[] hints = {"W", "E", "R"};
            for (int i = 0; i < 3; i++) {
                boolean active = gizmoMode == modeVals[i];
                boolean hov = isMouseOver(tbX + i * 36, tbY, 32, 28);
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, tbX + i * 36, tbY, 32, 28, 5);
                nvgFillColor(nvg, active ? rgba(50, 100, 200, 220)
                              : hov     ? rgba(40, 40, 55, 200)
                                        : rgba(22, 22, 28, 180));
                nvgFill(nvg);
                nvgFontSize(nvg, 13.0f);
                nvgFontFace(nvg, "sans");
                nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
                nvgFillColor(nvg, active ? rgba(255, 255, 255, 255) : rgba(140, 140, 160, 255));
                nvgText(nvg, tbX + i * 36 + 16, tbY + 14, modes[i]);
                nvgFontSize(nvg, 8.5f);
                nvgFillColor(nvg, rgba(70, 70, 90, 255));
                nvgText(nvg, tbX + i * 36 + 26, tbY + 22, hints[i]);
            }
        }

        drawSceneTree(10, MENU_HEIGHT + 10, treeW - 5, height - MENU_HEIGHT - 20);
        drawNodeInspector(treeW + 15, MENU_HEIGHT + 10, inspW - 5, height - MENU_HEIGHT - 20);

        // Handle + resize line giữ nguyên nhưng theo inspectorWidth

        // Overlay menus - vẽ SAU CÙNG để luôn on top
        if (showAddNodeMenu) drawAddNodeMenu(addMenuX, addMenuY);
        if (showContextMenu) drawContextMenu(ctxMenuX, ctxMenuY);
        
        if (activeMenuIndex >= 0) drawDropdown(activeMenuIndex, getMenuX(activeMenuIndex));
        // Resize handle
        float handleX = inspectorWidth + 10;
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, handleX, MENU_HEIGHT);
        nvgLineTo(nvg, handleX, height);
        nvgStrokeColor(nvg, isDraggingInspector ? rgba(80, 150, 255, 255) : rgba(60, 60, 70, 255));
        nvgStrokeWidth(nvg, isDraggingInspector ? 2.0f : 1.0f);
        nvgStroke(nvg);

        nvgEndFrame(nvg);
    }

    private void drawOpenGLWorld(float x, float y, float w, float h) {
        glEnable(GL_SCISSOR_TEST);
        glScissor((int)x, (int)(height - y - h), (int)w, (int)h);
        glViewport((int)x, (int)(height - y - h), (int)w, (int)h);
        glClearColor(0.05f, 0.05f, 0.07f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        if (editorCore != null) editorCore.renderToStudio((int)w, (int)h);
        glDisable(GL_SCISSOR_TEST);
        glViewport(0, 0, width, height);
    }

    private void setupTextFont() {
        String fontPath = "C:/Windows/Fonts/segoeui.ttf";
        if (!Files.exists(Paths.get(fontPath))) fontPath = "C:/Windows/Fonts/arial.ttf";
        nvgCreateFont(nvg, "sans", fontPath);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            update();
            render();
            if (!isVisible && frameCount++ >= WARMUP_FRAMES) {
                glfwShowWindow(window);
                isVisible = true;
            }
            glfwPollEvents();
            glfwSwapBuffers(window);
            fpsCounter++;
        }
    }

    private void cleanup() {
        editorCore.onCleanup();
        nvgDelete(nvg);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) { new MainStudio().run(); }
    
    // Chèn lại các hàm menuBar, Inspector, Button bạn đang dùng vào đây...
    private void drawMenuBar() {
        // Nền + border
        nvgBeginPath(nvg);
        nvgRect(nvg, 0, 0, width, MENU_HEIGHT);
        nvgFillColor(nvg, rgba(240, 240, 245, 255));
        nvgFill(nvg);

        nvgBeginPath(nvg);
        nvgMoveTo(nvg, 0, MENU_HEIGHT); nvgLineTo(nvg, width, MENU_HEIGHT);
        nvgStrokeColor(nvg, rgba(200, 200, 210, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        float curX = 8.0f;
        for (int i = 0; i < menuItems.length; i++) {
            float itemW = measureText(menuItems[i]) + 20;
            boolean hov  = isMouseOver(curX, 0, itemW, MENU_HEIGHT);
            boolean open = activeMenuIndex == i;

            if (hov || open) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, curX + 1, 3, itemW - 2, MENU_HEIGHT - 6, 4);
                nvgFillColor(nvg, open ? rgba(60, 120, 220, 255) : rgba(210, 210, 225, 255));
                nvgFill(nvg);
            }

            nvgFontSize(nvg, 13.0f); nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, open ? rgba(255, 255, 255, 255) : rgba(40, 40, 50, 255));
            nvgText(nvg, curX + itemW / 2, MENU_HEIGHT / 2, menuItems[i]);

            // ← KHÔNG gọi drawDropdown ở đây nữa
            curX += itemW;
        }
    }

    private float[] menuItemXPositions = new float[10];

    private void drawDropdown(int menuIdx, float menuX) {
        String[] items = MENU_ITEMS_SUB[menuIdx];
        float dropW = 200; 
        float itemH = 26;  // Giảm chiều cao mục xuống để menu gọn hơn
        float sepH = 10;   // Chiều cao riêng cho đường kẻ ngang
        
        // Tính tổng chiều cao linh hoạt dựa trên loại item
        float totalH = 8; // Padding top/bottom
        for (String s : items) {
            totalH += s.startsWith("─") ? sepH : itemH;
        }

        float dropX = menuX;
        float dropY = MENU_HEIGHT + 2; 

        if (dropX + dropW > width) dropX = width - dropW - 8;

        // 1. Shadow (Giữ nguyên logic cũ nhưng fit với totalH)
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, dropX, dropY + 2, dropW, totalH, 8);
        NVGPaint shadowPaint = nvgBoxGradient(nvg, dropX, dropY + 1, dropW, totalH, 8, 10, 
                                              rgba(0, 0, 0, 60), rgba(0, 0, 0, 0), NVGPaint.create());
        nvgFillPaint(nvg, shadowPaint);
        nvgFill(nvg);

        // 2. Nền Dropdown
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, dropX, dropY, dropW, totalH, 8);
        nvgFillColor(nvg, rgba(255, 255, 255, 250)); 
        nvgFill(nvg);

        // 3. Render Items
        float iy = dropY + 4;
        for (int i = 0; i < items.length; i++) {
            String label = items[i];
            
            if (label.startsWith("─")) {
                // Separator: Vẽ đường kẻ ở giữa khoảng cách sepH
                nvgBeginPath(nvg);
                nvgMoveTo(nvg, dropX + 10, iy + sepH / 2);
                nvgLineTo(nvg, dropX + dropW - 10, iy + sepH / 2);
                nvgStrokeColor(nvg, rgba(0, 0, 0, 30));
                nvgStrokeWidth(nvg, 1.0f);
                nvgStroke(nvg);
                
                iy += sepH; // Chỉ tăng một khoảng nhỏ cho separator
            } else {
                boolean hov = isMouseOver(dropX + 2, iy, dropW - 4, itemH);
                
                if (hov) {
                    nvgBeginPath(nvg);
                    nvgRoundedRect(nvg, dropX + 4, iy + 1, dropW - 8, itemH - 2, 4);
                    nvgFillColor(nvg, rgba(60, 120, 230, 255));
                    nvgFill(nvg);
                }

                nvgFontSize(nvg, 13.5f);
                nvgFontFace(nvg, "sans");
                nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
                nvgFillColor(nvg, hov ? rgba(255, 255, 255, 255) : rgba(50, 50, 60, 255));
                nvgText(nvg, dropX + 15, iy + itemH / 2, label);
                
                iy += itemH; // Tăng chiều cao mục bình thường
            }
        }
    }

    private void drawTabs(float x, float y, float w) {
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, TAB_HEIGHT);
        nvgFillColor(nvg, rgba(35, 35, 40, 255));
        nvgFill(nvg);
        float tabW = 140;
        drawTabItem(x,           y, tabW, "Scene View",  EditorTab.SCENE_3D);
        drawTabItem(x + tabW,    y, tabW, "Code Editor", EditorTab.CODE_EDITOR);
        drawTabItem(x + tabW*2,  y, tabW, "Console",     EditorTab.CONSOLE);
        drawTabItem(x + tabW*3,  y, tabW, "Node Editor",  EditorTab.NODE_EDITOR);
    }
    
    private void drawNodeEditor(float x, float y, float w, float h) {
        // 1. Nền tĩnh (Không bị ảnh hưởng bởi zoom)
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, rgba(18, 18, 22, 255));
        nvgFill(nvg);

        nvgSave(nvg);
        // Giới hạn vùng vẽ trong Viewport
        nvgScissor(nvg, x, y, w, h);

        // 2. Vẽ Grid (Lưới giờ sẽ co giãn và di chuyển theo neScale)
        drawNodeEditorGrid(x, y, w, h);

        // 3. Render nodes + connections
        // Chúng ta dời gốc tọa độ đến vị trí (x + neOffsetX, y + neOffsetY)
        // Sau đó thực hiện Scale để mọi thứ bên trong nodeEditorRenderer tự phóng to/thu nhỏ
        // Trong MainStudio.java
        nvgSave(nvg);
        nvgTranslate(nvg, x + neOffsetX, y + neOffsetY);
        nvgScale(nvg, neScale, neScale);

        // mx và my phải được chia cho neScale để khớp với không gian đã scale
        float worldMx = ((float)mouseX - (x + neOffsetX)) / neScale;
        float worldMy = ((float)mouseY - (y + neOffsetY)) / neScale;

        nodeEditorRenderer.render(nvg, nodeEditorData, 
            0, 0, // Truyền 0 vì nvgTranslate đã xử lý offset rồi
            neActivePin, neActivePinNode, 
            worldMx, worldMy);

        nvgRestore(nvg);
        
        nvgRestore(nvg); // Kết thúc Scissor

        // 4. Toolbar (Luôn nằm trên cùng, không bị zoom)
        drawNodeEditorToolbar(x, y, w);
    }
    
    private void drawNodeEditorGrid(float x, float y, float w, float h) {
        // Kích thước ô lưới cơ bản nhân với tỉ lệ zoom
        float size = 40.0f * neScale; 
        
        // Nếu zoom quá nhỏ, ẩn bớt lưới để tránh lag (Pentium G5400 sẽ cám ơn bạn)
        if (size < 5.0f) return; 

        // Tính toán điểm bắt đầu vẽ sao cho lưới trượt mượt mà theo Offset
        float offsetX = neOffsetX % size;
        float offsetY = neOffsetY % size;

        // Vẽ đường kẻ lưới
        nvgBeginPath(nvg);
        for (float gx = x + offsetX; gx < x + w; gx += size) {
            nvgMoveTo(nvg, gx, y); 
            nvgLineTo(nvg, gx, y + h);
        }
        for (float gy = y + offsetY; gy < y + h; gy += size) {
            nvgMoveTo(nvg, x, gy); 
            nvgLineTo(nvg, x + w, gy);
        }
        nvgStrokeColor(nvg, rgba(35, 35, 45, 255));
        nvgStrokeWidth(nvg, 1.0f);
        nvgStroke(nvg);

        // Vẽ Dot tại các giao điểm (Chỉ vẽ khi zoom đủ lớn để nhìn thấy)
        if (neScale > 0.5f) {
            for (float gx = x + offsetX; gx < x + w; gx += size) {
                for (float gy = y + offsetY; gy < y + h; gy += size) {
                    nvgBeginPath(nvg);
                    // Kích thước dot cũng scale nhẹ để trông tự nhiên
                    nvgCircle(nvg, gx, gy, 1.2f * neScale);
                    nvgFillColor(nvg, rgba(50, 50, 65, 255));
                    nvgFill(nvg);
                }
            }
        }
    }

    private void drawNodeEditorToolbar(float x, float y, float w) {
        float tbH = 32;
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, tbH);
        nvgFillColor(nvg, rgba(22, 22, 28, 220));
        nvgFill(nvg);

        // Add Node button
        boolean addHov = isMouseOver(x + 8, y + 5, 90, 22);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + 8, y + 5, 90, 22, 4);
        nvgFillColor(nvg, addHov ? rgba(45, 100, 55, 255) : rgba(30, 70, 38, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 11.5f); nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(150, 220, 160, 255));
        nvgText(nvg, x + 53, y + 16, "+ Add Node");

        // Reset View button
        boolean resetHov = isMouseOver(x + 106, y + 5, 90, 22);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x + 106, y + 5, 90, 22, 4);
        nvgFillColor(nvg, resetHov ? rgba(45, 45, 65, 255) : rgba(30, 30, 45, 255));
        nvgFill(nvg);
        nvgFillColor(nvg, rgba(140, 140, 170, 255));
        nvgText(nvg, x + 151, y + 16, "⌂ Reset View");

        // Node count info
        nvgFontSize(nvg, 10.5f);
        nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(60, 60, 80, 255));
        nvgText(nvg, x + w - 10, y + 16,
            nodeEditorData.nodes.size() + " nodes  "
            + nodeEditorData.connections.size() + " connections");
    }
    
    private void drawConsole(float x, float y, float w, float h) {
        // Nền
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, h);
        nvgFillColor(nvg, rgba(14, 14, 18, 255));
        nvgFill(nvg);

        // Toolbar
        float tbH = 34;
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, tbH);
        nvgFillColor(nvg, rgba(20, 20, 26, 255));
        nvgFill(nvg);

        // Build status indicator
        BuildSystem.BuildStatus bs = BuildSystem.getInstance().getStatus();
        int[] statusColor = switch (bs) {
            case SUCCESS  -> new int[]{40, 160, 60};
            case FAILED   -> new int[]{180, 40, 40};
            case COMPILING-> new int[]{180, 140, 20};
            default       -> new int[]{60, 60, 80};
        };
        nvgBeginPath(nvg);
        nvgCircle(nvg, x + 16, y + 17, 5);
        nvgFillColor(nvg, rgba(statusColor[0], statusColor[1], statusColor[2], 255));
        nvgFill(nvg);

        nvgFontSize(nvg, 11.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(130, 130, 145, 255));
        nvgText(nvg, x + 28, y + 17, "BUILD CONSOLE  —  " + bs.name());

        // Buttons
        float bx = x + w - 10;
        bx -= 70; drawConsoleBtn(bx, y + 5, 65, 24, "▶ Run",   rgba(35, 90, 35, 255), rgba(50, 130, 50, 255));
        bx -= 75; drawConsoleBtn(bx, y + 5, 70, 24, "⚙ Build", rgba(30, 60, 120, 255), rgba(45, 90, 175, 255));
        bx -= 70; drawConsoleBtn(bx, y + 5, 65, 24, "📦 Pack",  rgba(80, 50, 20, 255), rgba(120, 80, 30, 255));
        bx -= 70; drawConsoleBtn(bx, y + 5, 65, 24, "🗑 Clear", rgba(40, 20, 20, 255), rgba(70, 35, 35, 255));

        // Console text area
        nvgSave(nvg);
        nvgScissor(nvg, x, y + tbH, w, h - tbH);

        float lineH = 18.0f;
        float textY = y + tbH + 8 - consoleScrollY;

        for (int i = 0; i < consoleLines.size(); i++) {
            float ly = textY + i * lineH;
            if (ly + lineH < y + tbH || ly > y + h) continue;

            String line = consoleLines.get(i);

            // Màu theo prefix
            if (line.contains("[Error]") || line.contains("[Fatal]") || line.contains("✗")) {
                nvgFillColor(nvg, rgba(220, 80, 80, 255));
            } else if (line.contains("[Warning]")) {
                nvgFillColor(nvg, rgba(200, 160, 50, 255));
            } else if (line.contains("✓") || line.contains("SUCCESSFUL")) {
                nvgFillColor(nvg, rgba(80, 200, 100, 255));
            } else if (line.contains("[Build]") || line.contains("[Pack]")) {
                nvgFillColor(nvg, rgba(100, 160, 255, 255));
            } else if (line.contains("[Run]") || line.contains("[Game]")) {
                nvgFillColor(nvg, rgba(180, 220, 180, 255));
            } else if (line.startsWith("─")) {
                nvgFillColor(nvg, rgba(45, 45, 60, 255));
            } else {
                nvgFillColor(nvg, rgba(170, 170, 185, 255));
            }

            nvgFontSize(nvg, 12.5f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgText(nvg, x + 10, ly + lineH / 2, line);
        }

        nvgRestore(nvg);

        // Auto scroll to bottom
        if (consoleAutoScroll && !consoleLines.isEmpty()) {
            float maxScroll = Math.max(0, consoleLines.size() * lineH - (h - tbH) + 16);
            consoleScrollY = maxScroll;
        }
    }

    private void drawConsoleBtn(float x, float y, float w, float h,
            String label, NVGColor base, NVGColor hover) {
        boolean hov = isMouseOver(x, y, w, h);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, 5);
        nvgFillColor(nvg, hov ? hover : base);
        nvgFill(nvg);
        nvgFontSize(nvg, 11.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(210, 210, 220, 255));
        nvgText(nvg, x + w / 2, y + h / 2, label);
    }

    private void drawTabItem(float x, float y, float w, String label, EditorTab tab) {
        boolean active = (currentTab == tab);
        boolean hovered = isMouseOver(x, y, w, TAB_HEIGHT);
        if (hovered && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) currentTab = tab;
        nvgBeginPath(nvg);
        nvgRect(nvg, x, y, w, TAB_HEIGHT);
        nvgFillColor(nvg, active ? rgba(45, 45, 52, 255) : (hovered ? rgba(40, 40, 45, 255) : rgba(0,0,0,0)));
        nvgFill(nvg);
        if (active) {
            nvgBeginPath(nvg);
            nvgRect(nvg, x, y + TAB_HEIGHT - 2, w, 2);
            nvgFillColor(nvg, rgba(80, 150, 255, 255));
            nvgFill(nvg);
        }
        nvgFontSize(nvg, 13.0f);
        nvgFillColor(nvg, active ? rgba(255, 255, 255, 255) : rgba(160, 160, 170, 255));
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(nvg, x + w/2, y + TAB_HEIGHT/2, label);
    }

    // Hàm vẽ Header phụ (Section)
    private float drawCollapsibleHeader(float x, float y, float w, String title, boolean isOpen) {
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, 25, 4);
        nvgFillColor(nvg, rgba(45, 45, 52, 255));
        nvgFill(nvg);
        
        // Icon mũi tên (giả lập đóng mở)
        drawText(x + 10, y + 12, isOpen ? "▼" : "▶", 10, false);
        drawText(x + 25, y + 12, title.toUpperCase(), 11, true);
        
        return y + 30;
    }

    // Hàm vẽ dòng thông số (Label | Value)
    private float drawPropertyRow(float x, float y, float w, String label, String value) {
        // Label (Tên thuộc tính)
        nvgFontSize(nvg, 13.0f);
        nvgFillColor(nvg, rgba(160, 160, 170, 255));
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, x, y + 10, label);

        // Background cho ô nhập liệu (Input Box giả)
        float inputX = x + (w * 0.4f);
        float inputW = w * 0.6f;
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, inputX, y, inputW, 20, 3);
        nvgFillColor(nvg, rgba(15, 15, 18, 255));
        nvgFill(nvg);
        
        // Giá trị (Value)
        nvgFillColor(nvg, rgba(80, 180, 255, 255)); // Màu xanh highlight cho thông số
        nvgText(nvg, inputX + 8, y + 10, value);

        return y + 25;
    }

    private void drawSectionHeader(float x, float y, float w, String title) {
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, x + 15, y + 35);
        nvgLineTo(nvg, x + w - 15, y + 35);
        nvgStrokeColor(nvg, rgba(60, 60, 70, 255));
        nvgStrokeWidth(nvg, 1.0f);
        nvgStroke(nvg);
        
        drawText(x + 15, y + 20, title, 14, true);
    }

    private void drawButton(float x, float y, float w, float h, String label, NVGColor baseColor) {
        boolean hovered = isMouseOver(x, y, w, h);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y, w, h, 5.0f);
        nvgFillColor(nvg, hovered ? rgba((int)(baseColor.r()*255)+20, (int)(baseColor.g()*255)+20, (int)(baseColor.b()*255)+20, 255) : baseColor);
        nvgFill(nvg);
        nvgFontSize(nvg, 15.0f);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(255, 255, 255, 255));
        nvgText(nvg, x + w/2, y + h/2, label);
        if (hovered && label.equals("Save Project") && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
             projectName = "modcoderpack-redevelop";
             statusMessage = "Saving...";
             statusTimer = glfwGetTime();
        }
    }

    private void drawText(float x, float y, String text, float fontSize, boolean isBold) {
        nvgFontSize(nvg, fontSize);
        nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(230, 230, 240, 255));
        nvgText(nvg, x, y, text);
    }

    private boolean isMouseOver(float x, float y, float w, float h) {
        if (glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED) return false;
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}