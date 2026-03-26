package vn.pmgteam.yanase.studio.build;

import vn.pmgteam.yanase.studio.project.ProjectManager;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class LibraryManager {

    private static LibraryManager instance;
    public static LibraryManager getInstance() {
        if (instance == null) instance = new LibraryManager();
        return instance;
    }

    public List<LibEntry> getLibraries() {
        List<LibEntry> libs = new ArrayList<>();
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) return libs;

        Path libDir = Paths.get(pm.getProjectDir(), "lib");
        if (!Files.exists(libDir)) return libs;

        try {
            Files.list(libDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .sorted()
                .forEach(p -> {
                    try {
                        long size = Files.size(p);
                        libs.add(new LibEntry(p.getFileName().toString(),
                            p.toAbsolutePath().toString(), size));
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
        return libs;
    }

    public boolean addLibrary(String sourcePath) {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) return false;
        try {
            Path libDir = Paths.get(pm.getProjectDir(), "lib");
            Files.createDirectories(libDir);
            Path src = Paths.get(sourcePath);
            Files.copy(src, libDir.resolve(src.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean removeLibrary(String jarName) {
        ProjectManager pm = ProjectManager.getInstance();
        if (!pm.isProjectLoaded()) return false;
        try {
            Path jar = Paths.get(pm.getProjectDir(), "lib", jarName);
            Files.deleteIfExists(jar);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static class LibEntry {
        public final String name, path;
        public final long sizeBytes;
        public LibEntry(String name, String path, long size) {
            this.name = name; this.path = path; this.sizeBytes = size;
        }
        public String getSizeStr() {
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024*1024) return (sizeBytes/1024) + " KB";
            return String.format("%.1f MB", sizeBytes / (1024.0*1024));
        }
    }
}