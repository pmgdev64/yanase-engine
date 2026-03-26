package vn.pmgteam.yanase.studio.project;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Quản lý file .ygp (Yanase Game Project)
 * Load/Save project, quản lý danh sách source files và assets.
 */
public class ProjectManager {

    // --- Singleton ---
    private static ProjectManager instance;
    public static ProjectManager getInstance() {
        if (instance == null) instance = new ProjectManager();
        return instance;
    }

    // --- Project State ---
    private String projectFilePath = null;   // Đường dẫn tới file .ygp
    private String projectDir = null;        // Thư mục gốc của project
    private Map<String, Object> projectData = new LinkedHashMap<>();

    // --- Cache nội dung file đang mở trong CodeEditor ---
    private String activeSourceFile = null;  // Path tương đối, ví dụ "src/PlayerController.java"
    private String activeSourceContent = ""; // Nội dung file đang mở

    // =========================================================
    //  LOAD PROJECT
    // =========================================================

    /**
     * Mở file .ygp và load toàn bộ cấu hình.
     * @param ygpPath Đường dẫn tuyệt đối đến file .ygp
     */
    public boolean loadProject(String ygpPath) {
        try {
            Path path = Paths.get(ygpPath);
            if (!Files.exists(path)) {
                System.err.println("[ProjectManager] File không tồn tại: " + ygpPath);
                return false;
            }

            Yaml yaml = new Yaml();
            try (InputStream is = Files.newInputStream(path)) {
                projectData = yaml.load(is);
            }

            projectFilePath = ygpPath;
            projectDir = path.getParent().toAbsolutePath().toString();

            // Cập nhật last_opened
            getProjectSection().put("last_opened", java.time.LocalDate.now().toString());

            // Load file source đang active (nếu có)
            String activeFile = getActiveSourceFile();
            if (activeFile != null) {
                loadSourceFile(activeFile);
            }

            System.out.println("[ProjectManager] Loaded project: " + getProjectName());
            return true;

        } catch (Exception e) {
            System.err.println("[ProjectManager] Lỗi load project: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // =========================================================
    //  SAVE PROJECT
    // =========================================================

    public boolean saveProject() {
        if (projectFilePath == null) {
            System.err.println("[ProjectManager] Chưa có project nào được mở!");
            return false;
        }
        try {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            try (Writer writer = Files.newBufferedWriter(Paths.get(projectFilePath))) {
                yaml.dump(projectData, writer);
            }

            // Save file source đang active nếu có thay đổi
            if (activeSourceFile != null) {
                saveActiveSourceFile();
            }

            System.out.println("[ProjectManager] Saved: " + projectFilePath);
            return true;

        } catch (Exception e) {
            System.err.println("[ProjectManager] Lỗi save project: " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    //  TẠO PROJECT MỚI
    // =========================================================

    public boolean createProject(String dirPath, String projectName) {
        try {
            String pkg = projectName.toLowerCase();
            Path dir = Paths.get(dirPath);

            // Tạo cấu trúc thư mục
            Files.createDirectories(dir.resolve("src/" + pkg));
            Files.createDirectories(dir.resolve("assets/textures"));
            Files.createDirectories(dir.resolve("assets/sounds"));
            Files.createDirectories(dir.resolve("assets/fonts"));
            Files.createDirectories(dir.resolve("build/classes"));
            Files.createDirectories(dir.resolve("dist"));

            // Copy engine libs
            Path projectLib = dir.resolve("lib");
            Files.createDirectories(projectLib);
            copyEngineLibs(projectLib);

            // Main.java
            String mainJava =
                "package " + pkg + ";\n\n" +
                "import vn.pmgteam.yanase.base.Engine;\n\n" +
                "public class Main extends Engine {\n\n" +
                "    private GameScene world;\n\n" +
                "    @Override\n" +
                "    public void onInit() {\n" +
                "        world = new GameScene();\n" +
                "        world.init();\n" +
                "        this.setMainScene(world);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onLoop() {\n" +
                "        world.update(windowHandle, getDeltaTime());\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onCleanup() {\n" +
                "        if (world != null) world.cleanup();\n" +
                "    }\n\n" +
                "    public static void main(String[] args) {\n" +
                "        Main app = new Main();\n" +
                "        app.getGameSettings().setWindowTitle(\"" + projectName + "\");\n" +
                "        app.getGameSettings().setVSyncEnable(true);\n" +
                "        app.startApplication();\n" +
                "    }\n" +
                "}\n";

            // GameScene.java
            String gameSceneJava =
                "package " + pkg + ";\n\n" +
                "import vn.pmgteam.yanase.base.Engine;\n" +
                "import vn.pmgteam.yanase.node.*;\n" +
                "import vn.pmgteam.yanase.node.subnodes.*;\n" +
                "import vn.pmgteam.yanase.scene.BaseScene;\n" +
                "import vn.pmgteam.yanase.util.TextureManager;\n\n" +
                "public class GameScene extends BaseScene {\n\n" +
                "    private CameraNode player;\n\n" +
                "    public GameScene() {\n" +
                "        super(\"GameScene\");\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void init() {\n" +
                "        // Camera\n" +
                "        player = new CameraNode(\"Player\");\n" +
                "        player.position.set(0, 5, 10);\n" +
                "        player.setMode(CameraMode.PLAYER);\n" +
                "        player.setupControls(Engine.getEngine().getWindowHandle());\n\n" +
                "        // Ground\n" +
                "        Box3D ground = new Box3D(\"Ground\");\n" +
                "        ground.position.set(0, 0, 0);\n" +
                "        ground.scale.set(20, 1, 20);\n\n" +
                "        rootNode.appendChild(player);\n" +
                "        rootNode.appendChild(ground);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void update(long window, float deltaTime) {\n" +
                "        player.update(window, deltaTime);\n" +
                "        rootNode.updateTransform(null);\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void cleanup() {\n" +
                "        if (rootNode instanceof BaseNode)\n" +
                "            ((BaseNode) rootNode).cleanup();\n" +
                "    }\n\n" +
                "    public CameraNode getPlayer() { return player; }\n" +
                "}\n";

            Files.writeString(dir.resolve("src/" + pkg + "/Main.java"), mainJava);
            Files.writeString(dir.resolve("src/" + pkg + "/GameScene.java"), gameSceneJava);

            // Build YAML
            projectData = new LinkedHashMap<>();

            Map<String, Object> project = new LinkedHashMap<>();
            project.put("name", projectName);
            project.put("version", "1.0.0");
            project.put("engine_version", "0.0.1");
            project.put("created", java.time.LocalDate.now().toString());
            project.put("last_opened", java.time.LocalDate.now().toString());
            projectData.put("project", project);

            Map<String, Object> game = new LinkedHashMap<>();
            game.put("title", projectName);
            game.put("width", 1280);
            game.put("height", 720);
            game.put("vsync", true);
            game.put("target_fps", 60);
            game.put("main_class", pkg + ".Main"); // ← quan trọng cho build
            projectData.put("game", game);

            Map<String, Object> assets = new LinkedHashMap<>();
            assets.put("textures", new ArrayList<>());
            assets.put("sounds", new ArrayList<>());
            assets.put("fonts", new ArrayList<>());
            projectData.put("assets", assets);

            Map<String, Object> sources = new LinkedHashMap<>();
            List<String> sourceList = new ArrayList<>();
            sourceList.add("src/" + pkg + "/Main.java");
            sourceList.add("src/" + pkg + "/GameScene.java");
            sources.put("files", sourceList);
            sources.put("active_file", "src/" + pkg + "/Main.java");
            projectData.put("sources", sources);

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("target", "build/output");
            output.put("platform", "WINDOWS");
            output.put("jar_name", projectName + ".jar");
            output.put("include_jre", false);
            output.put("obfuscate", false);
            projectData.put("output", output);

            // Lưu .ygp
            projectFilePath = dir.resolve(projectName + ".ygp").toAbsolutePath().toString();
            projectDir = dir.toAbsolutePath().toString();
            saveProject();

            // Load file active vào editor
            loadSourceFile("src/" + pkg + "/Main.java");

            System.out.println("[ProjectManager] Created project: " + projectName + " at " + dirPath);
            return true;

        } catch (Exception e) {
            System.err.println("[ProjectManager] Lỗi tạo project: " + e.getMessage());
            return false;
        }
    }
    
    // =========================================================
    //  COPY ENGINE LIBS
    // =========================================================

    private void copyEngineLibs(Path destLib) {
    	try {
    		// Lấy thư mục chứa JAR đang chạy
    		Path runningJar = Paths.get(
    				ProjectManager.class.getProtectionDomain()
    				.getCodeSource().getLocation().toURI()
    				);
    		Path studioDir = runningJar.getParent();

    		// Tìm yanase-studio_lib theo thứ tự ưu tiên

    		List<Path> candidates = Arrays.asList(
    			    studioDir.resolve("yanase-launchwrappers_lib"), // ← ưu tiên cái này
    			    studioDir.resolve("yanase-studio_lib"),
    			    studioDir.getParent().resolve("yanase-launchwrappers_lib"),
    			    studioDir.getParent().resolve("yanase-studio_lib"),
    			    Paths.get(System.getProperty("user.dir"), "yanase-launchwrappers_lib")
    			);
    		Path sourceLib = null;
    		for (Path c : candidates) {
    			try {
    				Path normalized = c.normalize().toAbsolutePath();
    				if (Files.exists(normalized) && Files.isDirectory(normalized)) {
    					sourceLib = normalized;
    					System.out.println("[ProjectManager] Found engine libs: " + normalized);
    					break;
    					}
    				} catch (Exception ignored) {}
    			}

    		if (sourceLib == null) {
    			System.err.println("[ProjectManager] Warning: yanase-studio_lib not found, skipping lib copy");
    			return;
    			}

    		// Copy jar — bỏ sources/javadoc
    		final Path src = sourceLib;
    		int[] count = {0};
    		Files.list(src)
    		.filter(p -> p.toString().endsWith(".jar"))
    		.filter(p -> !p.getFileName().toString().contains("-sources"))
            .filter(p -> !p.getFileName().toString().contains("-javadoc"))
            .forEach(jar -> {
            	try {
            		Files.copy(jar, destLib.resolve(jar.getFileName()),
            				StandardCopyOption.REPLACE_EXISTING);
                    count[0]++;
                } catch (IOException e) {
                    System.err.println("[ProjectManager] Failed to copy: " + jar.getFileName());
                }
            });

    		System.out.println("[ProjectManager] Copied " + count[0] + " lib(s) to project/lib/");

    	} catch (Exception e) {
    		System.err.println("[ProjectManager] copyEngineLibs error: " + e.getMessage());
    	}
    }
    
 // =========================================================
    //  QUẢN LÝ NODE EDITOR DATA
    // =========================================================

    /** * Tải dữ liệu node từ projectData (YAML) 
     * Gọi hàm này khi bắt đầu mở Tab Node Editor
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getNodeData() {
        return (Map<String, Object>) projectData.computeIfAbsent("nodes", k -> new LinkedHashMap<>());
    }

    /** * Cập nhật dữ liệu node vào bộ nhớ tạm trước khi Save 
     * @param data Map chứa danh sách nodes và connections
     */
    public void setNodeData(Map<String, Object> data) {
        projectData.put("nodes", data);
    }

    /**
     * Đồng bộ nội dung từ ổ đĩa (Dùng cho case "Reload")
     */
    public void reloadFromDisk() {
        if (projectFilePath != null) {
            loadProject(projectFilePath);
        }
    }
    
    /**
     * Lưu project với tên file mới (Dùng cho case "Save As...")
     */
    public boolean saveProjectAs(String newPath) {
        this.projectFilePath = newPath;
        this.projectDir = Paths.get(newPath).getParent().toAbsolutePath().toString();
        return saveProject();
    }

    // =========================================================
    //  QUẢN LÝ SOURCE FILES
    // =========================================================

    /**
     * Load nội dung file .java vào CodeEditor.
     * @param relativePath Path tương đối so với thư mục project, ví dụ "src/Main.java"
     */
    public boolean loadSourceFile(String relativePath) {
        if (projectDir == null) return false;
        try {
            Path fullPath = Paths.get(projectDir, relativePath);
            if (!Files.exists(fullPath)) {
                System.err.println("[ProjectManager] File không tồn tại: " + fullPath);
                return false;
            }
            activeSourceFile = relativePath;
            activeSourceContent = Files.readString(fullPath);

            // Cập nhật active_file trong .ygp
            getSourcesSection().put("active_file", relativePath);

            System.out.println("[ProjectManager] Opened: " + relativePath);
            return true;

        } catch (Exception e) {
            System.err.println("[ProjectManager] Lỗi đọc file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lưu nội dung hiện tại của CodeEditor vào file .java.
     */
    public boolean saveActiveSourceFile() {
        if (projectDir == null || activeSourceFile == null) return false;
        try {
            Path fullPath = Paths.get(projectDir, activeSourceFile);
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, activeSourceContent);
            System.out.println("[ProjectManager] Saved source: " + activeSourceFile);
            return true;
        } catch (Exception e) {
            System.err.println("[ProjectManager] Lỗi lưu file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thêm file .java mới vào project.
     */
    public boolean addSourceFile(String relativePath) {
        if (projectDir == null) return false;
        try {
            Path fullPath = Paths.get(projectDir, relativePath);
            Files.createDirectories(fullPath.getParent());
            if (!Files.exists(fullPath)) {
                String className = fullPath.getFileName().toString().replace(".java", "");
                Files.writeString(fullPath, "// " + className + "\n\nvoid update() {\n    \n}\n");
            }
            getSourcesList().add(relativePath);
            System.out.println("[ProjectManager] Added source: " + relativePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================
    //  GETTERS - Dùng trong MainStudio / CodeEditor
    // =========================================================

    public boolean isProjectLoaded() { return projectFilePath != null; }
    public String getProjectName() {
        Object p = getProjectSection().get("name");
        return p != null ? p.toString() : "Unnamed";
    }
    public String getProjectDir() { return projectDir; }
    public String getActiveSourceFile() {
        Object s = getSourcesSection().get("active_file");
        return s != null ? s.toString() : null;
    }

    /** Nội dung file .java đang mở — dùng để khởi tạo codeContent trong MainStudio */
    public String getActiveSourceContent() { return activeSourceContent; }

    /** Gọi mỗi khi CodeEditor thay đổi nội dung (để sync trước khi save) */
    public void setActiveSourceContent(String content) { this.activeSourceContent = content; }

    @SuppressWarnings("unchecked")
    public List<String> getSourcesList() {
        Map<String, Object> sources = getSourcesSection();
        Object list = sources.get("files");
        if (list instanceof List) return (List<String>) list;
        List<String> newList = new ArrayList<>();
        sources.put("files", newList);
        return newList;
    }

    public Map<String, Object> getGameConfig() {
        Object g = projectData.get("game");
        if (g instanceof Map) return (Map<String, Object>) g;
        return new LinkedHashMap<>();
    }

    public Map<String, Object> getOutputConfig() {
        Object o = projectData.get("output");
        if (o instanceof Map) return (Map<String, Object>) o;
        return new LinkedHashMap<>();
    }

    // =========================================================
    //  INTERNAL HELPERS
    // =========================================================

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProjectSection() {
        return (Map<String, Object>) projectData.computeIfAbsent("project", k -> new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSourcesSection() {
        return (Map<String, Object>) projectData.computeIfAbsent("sources", k -> new LinkedHashMap<>());
    }
}