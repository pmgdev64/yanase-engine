package vn.pmgteam.yanase.resource;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.*;
import org.lwjgl.BufferUtils;

public class ResourceManager {
    private static final Map<String, File> packMap = new HashMap<>();

    /**
     * Đăng ký một Resource Pack (.zip) vào hệ thống
     */
    public static void registerPack(String packID, String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.getName().endsWith(".zip")) {
            packMap.put(packID, file);
            System.out.println("[ResourceManager] Registered Pack: " + packID);
        }
    }

    /**
     * Lấy dữ liệu thô từ Pack dưới dạng ByteBuffer (Dùng cho LWJGL 3.4.1)
     * ByteBuffer này được allocateDirect để không chiếm Heap RAM của Java
     */
    public static ByteBuffer loadToByteBuffer(String packID, String fileName) {
        File zipFile = packMap.get(packID);
        if (zipFile == null) return null;

        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipEntry entry = zip.getEntry(fileName);
            if (entry == null) return null;

            try (InputStream is = zip.getInputStream(entry)) {
                byte[] bytes = is.readAllBytes();
                ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
                buffer.put(bytes);
                buffer.flip();
                return buffer;
            }
        } catch (IOException e) {
            System.err.println("[ResourceManager] Error loading: " + fileName);
            return null;
        }
    }
}