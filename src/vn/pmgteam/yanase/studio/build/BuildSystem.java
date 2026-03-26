package vn.pmgteam.yanase.studio.build;

import vn.pmgteam.yanase.studio.project.ProjectManager;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public class BuildSystem {

    private static BuildSystem instance;
    public static BuildSystem getInstance() {
        if (instance == null) instance = new BuildSystem();
        return instance;
    }

    public enum BuildStatus { IDLE, COMPILING, SUCCESS, FAILED }
    private BuildStatus status = BuildStatus.IDLE;
    private final List<String> buildLog = new ArrayList<>();
    private Consumer<String> logListener;

    public BuildStatus getStatus() { return status; }
    public List<String> getBuildLog() { return Collections.unmodifiableList(buildLog); }
    public void setLogListener(Consumer<String> listener) { this.logListener = listener; }

    // ----------------------------------------------------------------
    // BUILD CLASSPATH HELPER — dùng chung cho build + run
    // ----------------------------------------------------------------
    private String buildClasspath(String buildDir, String projectDir) throws IOException {
        StringBuilder cp = new StringBuilder(buildDir);

        // Scan project/lib/ — engine libs đã được copy vào đây khi tạo project
        Path projectLib = Paths.get(projectDir, "lib");
        if (Files.exists(projectLib)) {
            Files.list(projectLib)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> !p.getFileName().toString().contains("-sources"))
                .filter(p -> !p.getFileName().toString().contains("-javadoc"))
                .sorted()
                .forEach(jar -> cp.append(File.pathSeparator).append(jar.toAbsolutePath()));
            log("[Build] Classpath: " + Files.list(projectLib)
                .filter(p -> p.toString().endsWith(".jar")).count() + " jar(s) from lib/");
        } else {
            log("[Warning] No lib/ folder found in project!");
        }

        return cp.toString();
    }

    // ----------------------------------------------------------------
    // BUILD
    // ----------------------------------------------------------------
    public boolean build() {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) { log("[Error] No project loaded!"); return false; }

        status = BuildStatus.COMPILING;
        buildLog.clear();
        log("[Build] Starting build: " + pm.getProjectName());
        log("[Build] Time: " + new java.util.Date());
        log("─────────────────────────────────");

        String projectDir = pm.getProjectDir();
        String buildDir   = projectDir + "/build/classes";

        try {
            Files.createDirectories(Paths.get(buildDir));

            // Thu thập .java files
            List<Path> sourceFiles = new ArrayList<>();
            Path srcPath = Paths.get(projectDir, "src");
            if (Files.exists(srcPath)) {
                Files.walk(srcPath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(sourceFiles::add);
            }

            if (sourceFiles.isEmpty()) {
                log("[Warning] No .java files found in src/");
                status = BuildStatus.SUCCESS;
                return true;
            }
            log("[Build] Found " + sourceFiles.size() + " source file(s)");

            // Classpath
            String cp = buildClasspath(buildDir, projectDir);

            // Compile bằng Eclipse JDT
            StringWriter outWriter = new StringWriter();
            StringWriter errWriter = new StringWriter();

            List<String> args = new ArrayList<>(Arrays.asList(
                "-source", "17", "-target", "17",
                "-encoding", "UTF-8",
                "-cp", cp,
                "-d", buildDir
                // bỏ -nowarn để thấy lỗi rõ hơn
            ));
            sourceFiles.forEach(f -> args.add(f.toAbsolutePath().toString()));

            log("[Build] Compiling with Eclipse JDT...");
            boolean success = BatchCompiler.compile(
                args.toArray(new String[0]),
                new PrintWriter(outWriter),
                new PrintWriter(errWriter),
                null
            );

            String out = outWriter.toString();
            String err = errWriter.toString();
            if (!out.isBlank()) for (String l : out.split("\n")) if (!l.isBlank()) log(l.trim());
            if (!err.isBlank()) for (String l : err.split("\n")) if (!l.isBlank()) log("[Error] " + l.trim());

            // Đếm .class files được tạo ra
            long classCount = Files.walk(Paths.get(buildDir))
                .filter(p -> p.toString().endsWith(".class")).count();

            log("─────────────────────────────────");
            if (success) {
                log("[Build] ✓ BUILD SUCCESSFUL — " + classCount + " class(es)");
                log("[Build] Output: " + buildDir);
                status = BuildStatus.SUCCESS;
            } else {
                log("[Build] ✗ BUILD FAILED");
                status = BuildStatus.FAILED;
            }
            return success;

        } catch (Exception e) {
            log("[Fatal] " + e.getMessage());
            status = BuildStatus.FAILED;
            return false;
        }
    }

    // ----------------------------------------------------------------
    // RUN
    // ----------------------------------------------------------------
    public Process runGame() {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) { log("[Error] No project loaded!"); return null; }

        String projectDir = pm.getProjectDir();
        String buildDir   = projectDir + "/build/classes";

        Object mainClassObj = pm.getGameConfig().get("main_class");
        String mainClass = mainClassObj != null
            ? mainClassObj.toString()
            : pm.getProjectName().toLowerCase() + ".Main";

        try {
            String cp = buildClasspath(buildDir, projectDir);

            String javaExe = ProcessHandle.current().info().command().orElse("java");
            List<String> cmd = new ArrayList<>(Arrays.asList(
                javaExe, "-cp", cp, mainClass
            ));

            log("[Run] Launching: " + mainClass);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(projectDir));
            pb.redirectErrorStream(true);

            Process proc = pb.start();
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) log("[Game] " + line);
                    log("[Run] Process exited: " + proc.exitValue());
                } catch (IOException e) {
                    log("[Run] Stream closed.");
                }
            }, "GameOutputReader").start();

            return proc;

        } catch (Exception e) {
            log("[Fatal] " + e.getMessage());
            return null;
        }
    }

    // ----------------------------------------------------------------
    // PACK
    // ----------------------------------------------------------------
    public boolean packGame() {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) { log("[Error] No project loaded!"); return false; }

        String projectDir   = pm.getProjectDir();
        String buildClasses = projectDir + "/build/classes";
        String outputDir    = projectDir + "/dist";
        String distLib      = outputDir  + "/lib";
        String projectName  = pm.getProjectName();

        try {
            log("[Pack] Packaging: " + projectName);
            Files.createDirectories(Paths.get(outputDir));
            Files.createDirectories(Paths.get(distLib));

            // 1. Kiểm tra build/classes
            Path classesPath = Paths.get(buildClasses);
            if (!Files.exists(classesPath)) {
                log("[Error] build/classes not found! Run Build first.");
                status = BuildStatus.FAILED;
                return false;
            }
            long classCount = Files.walk(classesPath)
                .filter(p -> p.toString().endsWith(".class")).count();
            if (classCount == 0) {
                log("[Error] No .class files! Run Build first.");
                status = BuildStatus.FAILED;
                return false;
            }
            log("[Pack] Found " + classCount + " .class file(s)");

            // 2. Copy project/lib/ → dist/lib/
            Path projectLib = Paths.get(projectDir, "lib");
            int[] libCount = {0};
            if (Files.exists(projectLib)) {
                Files.list(projectLib)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("-sources"))
                    .filter(p -> !p.getFileName().toString().contains("-javadoc"))
                    .forEach(jar -> {
                        try {
                            Files.copy(jar,
                                Paths.get(distLib, jar.getFileName().toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                            libCount[0]++;
                        } catch (IOException e) {
                            log("[Warning] Failed to copy: " + jar.getFileName());
                        }
                    });
                log("[Pack] Copied " + libCount[0] + " lib(s) → dist/lib/");
            } else {
                log("[Warning] No lib/ folder found");
            }

            // 3. Tạo JAR
            String jarPath = outputDir + "/" + projectName + ".jar";
            createJar(buildClasses, jarPath, pm.getGameConfig(), projectName, distLib);

            // 4. Copy assets
            Path assetsSrc = Paths.get(projectDir, "assets");
            if (Files.exists(assetsSrc)) {
                copyDirectory(assetsSrc, Paths.get(outputDir, "assets"));
                log("[Pack] Copied assets/");
            }

            // 5. Launch scripts
            createLaunchScript(outputDir, projectName, pm.getGameConfig());

            log("─────────────────────────────────");
            log("[Pack] ✓ PACK SUCCESSFUL");
            log("[Pack] Output: " + outputDir);
            return true;

        } catch (Exception e) {
            log("[Fatal] Pack failed: " + e.getMessage());
            e.printStackTrace();
            status = BuildStatus.FAILED;
            return false;
        }
    }

    private void createJar(String classesDir, String jarPath,
                           Map<String, Object> gameConfig,
                           String projectName, String distLib) throws Exception {
        Object mainClassObj = gameConfig.get("main_class");
        String mainClass = mainClassObj != null
            ? mainClassObj.toString()
            : projectName.toLowerCase() + ".Main";

        // Class-Path manifest — trỏ đến lib/*.jar
        StringBuilder cpManifest = new StringBuilder();
        Path libDir = Paths.get(distLib);
        if (Files.exists(libDir)) {
            Files.list(libDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .sorted()
                .forEach(jar -> cpManifest.append("lib/")
                    .append(jar.getFileName()).append(" "));
        }

        // Manifest
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        java.util.jar.Attributes attrs = manifest.getMainAttributes();
        attrs.put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(java.util.jar.Attributes.Name.MAIN_CLASS, mainClass);
        if (cpManifest.length() > 0)
            attrs.put(java.util.jar.Attributes.Name.CLASS_PATH, cpManifest.toString().trim());
        attrs.putValue("Created-By", "YanaseStudio 0.0.1");
        attrs.putValue("Built-Date", new java.util.Date().toString());

        // Ghi JAR
        Path classRoot = Paths.get(classesDir);
        int[] added = {0};

        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(
                new FileOutputStream(jarPath), manifest)) {
            Files.walk(classRoot)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    // QUAN TRỌNG: dùng / trong JAR entry (Windows dùng \)
                    String entryName = classRoot.relativize(file)
                        .toString().replace("\\", "/");
                    try {
                        jos.putNextEntry(new java.util.jar.JarEntry(entryName));
                        jos.write(Files.readAllBytes(file));
                        jos.closeEntry();
                        added[0]++;
                    } catch (IOException e) {
                        log("[Warning] Skip entry: " + entryName);
                    }
                });
        }
        log("[Pack] JAR: " + Paths.get(jarPath).getFileName()
            + " (" + added[0] + " entries, Main-Class: " + mainClass + ")");
    }

    private void createLaunchScript(String outputDir, String projectName,
                                    Map<String, Object> gameConfig) throws IOException {
        Object mainClassObj = gameConfig.get("main_class");
        String mainClass = mainClassObj != null
            ? mainClassObj.toString()
            : projectName.toLowerCase() + ".Main";

        // Windows batch
        String bat = "@echo off\r\n" +
            "title " + projectName + "\r\n" +
            "java -cp \"" + projectName + ".jar;lib/*\" " + mainClass + "\r\n" +
            "pause\r\n";
        Files.writeString(Paths.get(outputDir, "run.bat"), bat);

        // Linux/Mac
        String sh = "#!/bin/bash\n" +
            "java -cp \"" + projectName + ".jar:lib/*\" " + mainClass + "\n";
        Files.writeString(Paths.get(outputDir, "run.sh"), sh);

        // README
        String readme = "# " + projectName + "\n\n" +
            "**Run on Windows:** Double-click `run.bat`\n" +
            "**Run on Linux/Mac:** `bash run.sh`\n\n" +
            "```\njava -cp \"" + projectName + ".jar:lib/*\" " + mainClass + "\n```\n\n" +
            "Built with Yanase Studio v0.0.1\n";
        Files.writeString(Paths.get(outputDir, "README.md"), readme);

        log("[Pack] Created run.bat, run.sh, README.md");
    }

    private void copyDirectory(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            Path target = dest.resolve(src.relativize(source));
            try {
                if (Files.isDirectory(source)) Files.createDirectories(target);
                else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        });
    }

    private void log(String msg) {
        buildLog.add(msg);
        if (logListener != null) logListener.accept(msg);
        System.out.println(msg);
    }
}