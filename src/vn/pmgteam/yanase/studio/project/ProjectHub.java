package vn.pmgteam.yanase.studio.project;

import org.lwjgl.glfw.*;
import org.lwjgl.nanovg.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.awt.FileDialog;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class ProjectHub {

    private long window;
    private long nvg;
    private static final int W = 920, H = 580;

    private String result = null;
    private boolean running = true;

    // Recent projects (load từ file)
    private final List<RecentEntry> recentProjects = new ArrayList<>();
    private static final String RECENT_FILE =
        System.getProperty("user.home") + "/.yanase_studio_recent";

    // Mouse
    private double mouseX, mouseY;
    private boolean mousePressedLast = false;

    // Scroll
    private float listScrollY = 0;

    // Search
    private String searchText = "";
    private boolean searchFocused = false;

    // New project dialog
    private boolean showNewDialog = false;
    private String dlgName = "MyGame";
    private String dlgPath = System.getProperty("user.home") + "/YanaseProjects/MyGame";
    private boolean dlgNameFocused = true;
    private boolean dlgPathFocused = false;
    private String dlgError = "";

    private final NVGColor colorBuf = NVGColor.create();

    private static class RecentEntry {
        String name, path, date;
        boolean exists;
        RecentEntry(String name, String path, String date) {
            this.name = name; this.path = path; this.date = date;
            this.exists = Files.exists(Paths.get(path));
        }
    }

    public ProjectHub() {}

    public String run() {
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(W, H, "Yanase Studio — Project Hub", NULL, NULL);
        if (window == NULL) return null;

        // Căn giữa màn hình
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null)
            glfwSetWindowPos(window, (vid.width() - W) / 2, (vid.height() - H) / 2);

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        // Trong ProjectHub.run(), sau GL.createCapabilities():
        vn.pmgteam.yanase.util.TextureManager.setWindowIcons(window,
            "/assets/icons/icon_16x16.png",
            "/assets/icons/icon_32x32.png");
        glEnable(GL_MULTISAMPLE);
        glfwSwapInterval(1);

        nvg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        setupFont();
        setupCallbacks();
        loadRecent();

        glfwShowWindow(window);

        while (!glfwWindowShouldClose(window) && running) {
            render();
            glfwPollEvents();
            glfwSwapBuffers(window);
        }

        saveRecent();
        nvgDelete(nvg);
        glfwDestroyWindow(window);
        return result;
    }

    // ----------------------------------------------------------------
    // CALLBACKS
    // ----------------------------------------------------------------

    private void setupCallbacks() {
        glfwSetScrollCallback(window, (win, xoff, yoff) -> {
            if (!showNewDialog) {
                listScrollY -= (float)(yoff * 38);
                listScrollY = Math.max(0, listScrollY);
            }
        });

        glfwSetCharCallback(window, (win, codepoint) -> {
            String ch = new String(Character.toChars(codepoint));
            if (searchFocused) {
                searchText += ch;
            } else if (showNewDialog) {
                if (dlgNameFocused) {
                    dlgName += ch;
                    // Auto-update path khi đổi tên
                    String base = Paths.get(dlgPath).getParent() != null
                        ? Paths.get(dlgPath).getParent().toString()
                        : System.getProperty("user.home") + "/YanaseProjects";
                    dlgPath = base + "/" + dlgName;
                } else if (dlgPathFocused) {
                    dlgPath += ch;
                }
                dlgError = "";
            }
        });

        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
            if (key == GLFW_KEY_ESCAPE) {
                if (showNewDialog) { showNewDialog = false; dlgError = ""; }
                else { searchFocused = false; }
            }
            if (key == GLFW_KEY_BACKSPACE) {
                if (searchFocused && !searchText.isEmpty())
                    searchText = searchText.substring(0, searchText.length() - 1);
                else if (showNewDialog) {
                    if (dlgNameFocused && !dlgName.isEmpty()) {
                        dlgName = dlgName.substring(0, dlgName.length() - 1);
                        String base = Paths.get(dlgPath).getParent() != null
                            ? Paths.get(dlgPath).getParent().toString()
                            : System.getProperty("user.home") + "/YanaseProjects";
                        dlgPath = base + "/" + dlgName;
                    } else if (dlgPathFocused && !dlgPath.isEmpty())
                        dlgPath = dlgPath.substring(0, dlgPath.length() - 1);
                    dlgError = "";
                }
            }
            if (key == GLFW_KEY_TAB && showNewDialog) {
                dlgNameFocused = !dlgNameFocused;
                dlgPathFocused = !dlgPathFocused;
            }
            if (key == GLFW_KEY_ENTER && showNewDialog) doCreateProject();
        });
    }

    // ----------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------

    private void render() {
        try (MemoryStack stack = stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1), y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            mouseX = x.get(0); mouseY = y.get(0);
        }
        boolean mp = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean click = mp && !mousePressedLast;

        glClearColor(0.085f, 0.085f, 0.10f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        nvgBeginFrame(nvg, W, H, 1.0f);
        drawSidebar(click);
        drawMain(click);
        if (showNewDialog) drawNewDialog(click);
        nvgEndFrame(nvg);

        mousePressedLast = mp;
    }

    // ----------------------------------------------------------------
    // SIDEBAR
    // ----------------------------------------------------------------

    private void drawSidebar(boolean click) {
        float sw = 230;

        // Nền sidebar
        nvgBeginPath(nvg);
        nvgRect(nvg, 0, 0, sw, H);
        nvgFillColor(nvg, rgba(16, 16, 20, 255));
        nvgFill(nvg);

        // Border phải
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, sw, 0); nvgLineTo(nvg, sw, H);
        nvgStrokeColor(nvg, rgba(28, 28, 38, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        // Logo
        nvgFontFace(nvg, "sans");
        nvgFontSize(nvg, 22.0f);
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(230, 230, 240, 255));
        nvgText(nvg, 20, 38, "Yanase Studio");

        nvgFontSize(nvg, 11.0f);
        nvgFillColor(nvg, rgba(65, 85, 130, 255));
        nvgText(nvg, 20, 58, "v0.0.1  ·  pmgdev64");

        // Divider
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, 15, 75); nvgLineTo(nvg, sw - 15, 75);
        nvgStrokeColor(nvg, rgba(30, 30, 40, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        // Buttons
        float by = 88;
        sideBtn(15, by, sw - 30, "＋  New Project", false, click, () -> {
            showNewDialog = true;
            dlgName = "MyGame";
            dlgPath = System.getProperty("user.home") + "/YanaseProjects/MyGame";
            dlgNameFocused = true; dlgPathFocused = false; dlgError = "";
        }); by += 44;

        sideBtn(15, by, sw - 30, "📂  Open .ygp file", false, click,
            this::doOpenFileDialog); by += 44;

        // Section label
        by += 8;
        nvgFontSize(nvg, 9.5f);
        nvgFillColor(nvg, rgba(55, 55, 75, 255));
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, 20, by + 5, "RECENT PROJECTS");
        by += 22;

        // Stats
        long validCount = recentProjects.stream().filter(p -> p.exists).count();
        nvgFontSize(nvg, 10.5f);
        nvgFillColor(nvg, rgba(50, 50, 70, 255));
        nvgText(nvg, 20, by + 5, validCount + " / " + recentProjects.size() + " accessible");

        // Footer
        nvgFontSize(nvg, 9.5f);
        nvgFillColor(nvg, rgba(40, 40, 60, 255));
        nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(nvg, sw / 2, H - 18, "© 2026 PmgTeam · YanaseEngine");
    }

    private void sideBtn(float x, float y, float w, String label,
            boolean danger, boolean click, Runnable action) {
    	boolean hov = isOver(x, y, w, 36);
    	nvgBeginPath(nvg);
    	nvgRoundedRect(nvg, x, y, w, 36, 7);
    	nvgFillColor(nvg, hov
    			? (danger ? rgba(120, 30, 30, 255) : rgba(30, 65, 130, 255))
    					: rgba(22, 22, 30, 255));
    	nvgFill(nvg);
    	nvgFontSize(nvg, 13.0f);
    	nvgFontFace(nvg, "sans");
    	nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE); // ← CENTER
    	nvgFillColor(nvg, hov ? rgba(220, 220, 235, 255) : rgba(150, 150, 165, 255));
    	nvgText(nvg, x + w / 2f, y + 18, label); // ← x + w/2 thay vì x + 14
    	if (hov && click) action.run();
    }

    // ----------------------------------------------------------------
    // MAIN AREA
    // ----------------------------------------------------------------

    private void drawMain(boolean click) {
        float sx = 230;

        // Header bar
        nvgBeginPath(nvg);
        nvgRect(nvg, sx, 0, W - sx, 58);
        nvgFillColor(nvg, rgba(12, 12, 16, 255));
        nvgFill(nvg);

        // Search bar
        float sbX = sx + 16, sbY = 13, sbW = 300, sbH = 32;
        boolean sbHov = isOver(sbX, sbY, sbW, sbH);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, sbX, sbY, sbW, sbH, 7);
        nvgFillColor(nvg, rgba(20, 20, 26, 255));
        nvgFill(nvg);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, sbX, sbY, sbW, sbH, 7);
        nvgStrokeColor(nvg, searchFocused ? rgba(55, 110, 210, 255) : rgba(35, 35, 48, 255));
        nvgStrokeWidth(nvg, 1.5f); nvgStroke(nvg);

        // Search icon + text
        nvgFontSize(nvg, 13.0f); nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        if (searchText.isEmpty() && !searchFocused) {
            nvgFillColor(nvg, rgba(50, 50, 70, 255));
            nvgText(nvg, sbX + 12, sbY + 16, "🔍  Search projects...");
        } else {
            nvgFillColor(nvg, rgba(200, 200, 215, 255));
            nvgText(nvg, sbX + 12, sbY + 16, searchText + (searchFocused ? "|" : ""));
        }
        if (sbHov && click) {
            searchFocused = true; dlgNameFocused = false; dlgPathFocused = false;
        }
        if (!sbHov && click) searchFocused = false;

        // Count label
        List<RecentEntry> filtered = getFiltered();
        nvgFontSize(nvg, 11.0f);
        nvgFillColor(nvg, rgba(50, 50, 70, 255));
        nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, W - 16, 29, filtered.size() + " project(s)");

        // Divider header
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, sx, 58); nvgLineTo(nvg, W, 58);
        nvgStrokeColor(nvg, rgba(22, 22, 32, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        // Column headers
        float hY = 68;
        nvgFontSize(nvg, 10.0f);
        nvgFillColor(nvg, rgba(55, 55, 75, 255));
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, sx + 70, hY, "PROJECT NAME");
        nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
        nvgText(nvg, W - 16, hY, "LAST OPENED");

        nvgBeginPath(nvg);
        nvgMoveTo(nvg, sx + 8, hY + 12); nvgLineTo(nvg, W - 8, hY + 12);
        nvgStrokeColor(nvg, rgba(20, 20, 28, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        // Project list
        nvgSave(nvg);
        nvgScissor(nvg, sx, 82, W - sx, H - 82);

        if (filtered.isEmpty()) {
            nvgFontSize(nvg, 13.0f);
            nvgFillColor(nvg, rgba(45, 45, 65, 255));
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgText(nvg, sx + (W - sx) / 2f, H / 2f - 10,
                searchText.isEmpty() ? "No projects yet. Create one!" : "No results for \"" + searchText + "\"");
        }

        float itemH = 66;
        for (int i = 0; i < filtered.size(); i++) {
            RecentEntry p = filtered.get(i);
            float iy = 82 + i * itemH - listScrollY;
            if (iy + itemH < 82 || iy > H) continue;

            boolean hov = isOver(sx, iy, W - sx, itemH);

            // Row bg
            nvgBeginPath(nvg);
            nvgRect(nvg, sx, iy, W - sx, itemH);
            nvgFillColor(nvg, hov ? rgba(20, 28, 48, 255) : rgba(12, 12, 16, 255));
            nvgFill(nvg);

            // Row divider
            nvgBeginPath(nvg);
            nvgMoveTo(nvg, sx + 16, iy + itemH - 1);
            nvgLineTo(nvg, W - 16, iy + itemH - 1);
            nvgStrokeColor(nvg, rgba(20, 20, 28, 255));
            nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

            // Icon
            nvgBeginPath(nvg);
            nvgRoundedRect(nvg, sx + 16, iy + 13, 40, 40, 8);
            nvgFillColor(nvg, p.exists ? rgba(22, 45, 95, 255) : rgba(50, 22, 22, 255));
            nvgFill(nvg);
            nvgFontSize(nvg, 18.0f);
            nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, p.exists ? rgba(70, 130, 240, 255) : rgba(180, 60, 60, 255));
            nvgText(nvg, sx + 36, iy + 33, p.exists ? "Y" : "!");

            // Name
            nvgFontSize(nvg, 15.0f);
            nvgFontFace(nvg, "sans");
            nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
            nvgFillColor(nvg, p.exists ? rgba(215, 215, 228, 255) : rgba(130, 80, 80, 255));
            nvgText(nvg, sx + 68, iy + 24, p.name);

            // Path
            nvgFontSize(nvg, 10.5f);
            nvgFillColor(nvg, p.exists ? rgba(65, 65, 88, 255) : rgba(100, 50, 50, 255));
            // Truncate path nếu quá dài
            String displayPath = p.path.length() > 55 ? "..." + p.path.substring(p.path.length() - 52) : p.path;
            nvgText(nvg, sx + 68, iy + 44, displayPath);

            // Date
            nvgFontSize(nvg, 10.5f);
            nvgFillColor(nvg, rgba(55, 55, 75, 255));
            nvgTextAlign(nvg, NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE);
            nvgText(nvg, W - 16, iy + 33, p.date);

            // Missing badge
            if (!p.exists) {
                nvgBeginPath(nvg);
                nvgRoundedRect(nvg, W - 100, iy + 20, 68, 18, 4);
                nvgFillColor(nvg, rgba(80, 20, 20, 255));
                nvgFill(nvg);
                nvgFontSize(nvg, 9.5f);
                nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
                nvgFillColor(nvg, rgba(200, 80, 80, 255));
                nvgText(nvg, W - 66, iy + 29, "NOT FOUND");
            }

            // Click → mở project
            if (hov && click && p.exists) doOpenProject(p.path);
        }

        nvgRestore(nvg);
    }

    // ----------------------------------------------------------------
    // NEW PROJECT DIALOG
    // ----------------------------------------------------------------

    private void drawNewDialog(boolean click) {
        // Overlay
        nvgBeginPath(nvg); nvgRect(nvg, 0, 0, W, H);
        nvgFillColor(nvg, rgba(0, 0, 0, 170)); nvgFill(nvg);

        float dw = 500, dh = 310;
        float dx = (W - dw) / 2f, dy = (H - dh) / 2f;

        // Dialog box
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, dx, dy, dw, dh, 12);
        nvgFillColor(nvg, rgba(18, 18, 24, 255)); nvgFill(nvg);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, dx, dy, dw, dh, 12);
        nvgStrokeColor(nvg, rgba(40, 40, 58, 255));
        nvgStrokeWidth(nvg, 1.5f); nvgStroke(nvg);

        // Title
        nvgFontSize(nvg, 17.0f); nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(215, 215, 228, 255));
        nvgText(nvg, dx + 24, dy + 30, "New Project");

        // Divider
        nvgBeginPath(nvg);
        nvgMoveTo(nvg, dx + 16, dy + 50); nvgLineTo(nvg, dx + dw - 16, dy + 50);
        nvgStrokeColor(nvg, rgba(30, 30, 44, 255));
        nvgStrokeWidth(nvg, 1.0f); nvgStroke(nvg);

        float fy = dy + 66;

        // Project Name field
        dlgField(dx + 24, fy, dw - 48, "Project Name", dlgName, dlgNameFocused, click, () -> {
            dlgNameFocused = true; dlgPathFocused = false;
        }); fy += 72;

        // Location field
        dlgField(dx + 24, fy, dw - 48, "Location (folder path)", dlgPath, dlgPathFocused, click, () -> {
            dlgPathFocused = true; dlgNameFocused = false;
        }); fy += 72;

        // Preview path
        nvgFontSize(nvg, 10.5f);
        nvgFillColor(nvg, rgba(50, 80, 130, 255));
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        String preview = dlgPath + "/" + dlgName + ".ygp";
        if (preview.length() > 60) preview = "..." + preview.substring(preview.length() - 57);
        nvgText(nvg, dx + 24, fy - 8, "→ " + preview);

        // Error
        if (!dlgError.isEmpty()) {
            nvgFontSize(nvg, 11.0f);
            nvgFillColor(nvg, rgba(200, 70, 70, 255));
            nvgText(nvg, dx + 24, fy + 10, "⚠ " + dlgError);
        }

        // Buttons
        float btnY = dy + dh - 52;
        // Cancel
        sideBtn(dx + dw - 250, btnY, 110, "Cancel", false, click,
            () -> { showNewDialog = false; dlgError = ""; });
        // Create
        boolean createHov = isOver(dx + dw - 130, btnY, 110, 36);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, dx + dw - 130, btnY, 110, 36, 7);
        nvgFillColor(nvg, createHov ? rgba(45, 100, 200, 255) : rgba(30, 75, 165, 255));
        nvgFill(nvg);
        nvgFontSize(nvg, 13.0f); nvgTextAlign(nvg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(220, 220, 235, 255));
        nvgText(nvg, dx + dw - 130 + 55, btnY + 18, "Create");
        if (createHov && click) doCreateProject();
    }

    private void dlgField(float x, float y, float w, String label,
                          String value, boolean focused, boolean click, Runnable onFocus) {
        nvgFontSize(nvg, 11.0f); nvgFontFace(nvg, "sans");
        nvgTextAlign(nvg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgFillColor(nvg, rgba(80, 80, 105, 255));
        nvgText(nvg, x, y + 7, label);

        boolean hov = isOver(x, y + 18, w, 34);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y + 18, w, 34, 6);
        nvgFillColor(nvg, rgba(12, 12, 16, 255)); nvgFill(nvg);
        nvgBeginPath(nvg);
        nvgRoundedRect(nvg, x, y + 18, w, 34, 6);
        nvgStrokeColor(nvg, focused ? rgba(55, 110, 210, 255) : rgba(35, 35, 50, 255));
        nvgStrokeWidth(nvg, 1.5f); nvgStroke(nvg);

        nvgFontSize(nvg, 13.0f);
        nvgFillColor(nvg, rgba(200, 200, 215, 255));
        nvgText(nvg, x + 12, y + 35, value + (focused ? "|" : ""));
        if (hov && click) onFocus.run();
    }

    // ----------------------------------------------------------------
    // ACTIONS
    // ----------------------------------------------------------------

    private void doCreateProject() {
        if (dlgName.isBlank()) { dlgError = "Project name cannot be empty"; return; }
        if (dlgPath.isBlank()) { dlgError = "Location cannot be empty"; return; }

        String fullDir = dlgPath;
        boolean ok = ProjectManager.getInstance().createProject(fullDir, dlgName);
        if (!ok) { dlgError = "Failed to create project. Check path."; return; }

        String ygpPath = fullDir + "/" + dlgName + ".ygp";
        showNewDialog = false;
        doOpenProject(ygpPath);
    }

    private void doOpenProject(String ygpPath) {
        boolean ok = ProjectManager.getInstance().loadProject(ygpPath);
        if (!ok) { dlgError = "Cannot load project: " + ygpPath; return; }

        // Update recent list
        recentProjects.removeIf(p -> p.path.equals(ygpPath));
        String name = ProjectManager.getInstance().getProjectName();
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        recentProjects.add(0, new RecentEntry(name, ygpPath, date));
        if (recentProjects.size() > 25) recentProjects.remove(25);

        result = ygpPath;
        running = false;
    }

    private void doOpenFileDialog() {
        FileDialog fd = new FileDialog((java.awt.Frame) null,
            "Open Yanase Project", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> name.endsWith(".ygp"));
        fd.setVisible(true);
        String dir = fd.getDirectory(), file = fd.getFile();
        if (dir != null && file != null) doOpenProject(dir + file);
    }

    // ----------------------------------------------------------------
    // HELPERS
    // ----------------------------------------------------------------

    private List<RecentEntry> getFiltered() {
        if (searchText.isEmpty()) return recentProjects;
        String q = searchText.toLowerCase();
        return recentProjects.stream()
            .filter(p -> p.name.toLowerCase().contains(q) || p.path.toLowerCase().contains(q))
            .toList();
    }

    private void loadRecent() {
        try {
            Path p = Paths.get(RECENT_FILE);
            if (!Files.exists(p)) return;
            for (String line : Files.readAllLines(p)) {
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) recentProjects.add(new RecentEntry(parts[0], parts[1], parts[2]));
            }
        } catch (IOException ignored) {}
    }

    private void saveRecent() {
        try {
            List<String> lines = recentProjects.stream()
                .map(p -> p.name + "|" + p.path + "|" + p.date).toList();
            Files.write(Paths.get(RECENT_FILE), lines);
        } catch (IOException ignored) {}
    }

    private boolean isOver(float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private NVGColor rgba(int r, int g, int b, int a) {
        colorBuf.r(r/255f); colorBuf.g(g/255f); colorBuf.b(b/255f); colorBuf.a(a/255f);
        return colorBuf;
    }

    private void setupFont() {
        String fp = "C:/Windows/Fonts/segoeui.ttf";
        if (!Files.exists(Paths.get(fp))) fp = "C:/Windows/Fonts/arial.ttf";
        nvgCreateFont(nvg, "sans", fp);
    }
}