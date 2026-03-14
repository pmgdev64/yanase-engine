package vn.pngteam.yanase.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL11.*;

public class TextureManager {

    public static int loadTexture(String path) {
        ByteBuffer image;
        int width, height;

        try {
            // Đọc tệp từ trong JAR thành ByteBuffer
            ByteBuffer container = ioResourceToByteBuffer(path, 8 * 1024);
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                // Nạp từ bộ nhớ thay vì nạp từ đường dẫn tệp
                STBImage.stbi_set_flip_vertically_on_load(false);
                image = STBImage.stbi_load_from_memory(container, w, h, comp, 4);
                
                if (image == null) {
                    throw new RuntimeException("STBImage lỗi nạp resource: " + STBImage.stbi_failure_reason());
                }

                width = w.get();
                height = h.get();
            }

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 0x812F); // GL_CLAMP_TO_EDGE
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 0x812F); // GL_CLAMP_TO_EDGE

            STBImage.stbi_image_free(image);
            return id;

        } catch (Exception e) {
            System.err.println("[Yanase Engine] Lỗi nạp Texture từ JAR: " + path);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Chuyển đổi Resource nội bộ thành ByteBuffer để STBImage có thể đọc được
     */
    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws Exception {
        ByteBuffer buffer;
        
        // Đảm bảo đường dẫn bắt đầu bằng '/' để tìm trong classpath
        if (!resource.startsWith("/")) resource = "/" + resource;

        try (InputStream source = TextureManager.class.getResourceAsStream(resource)) {
            if (source == null) throw new java.io.FileNotFoundException(resource);
            
            try (ReadableByteChannel rbc = Channels.newChannel(source)) {
                buffer = BufferUtils.createByteBuffer(bufferSize);

                while (true) {
                    int bytes = rbc.read(buffer);
                    if (bytes == -1) break;
                    if (buffer.remaining() == 0) {
                        // Mở rộng buffer nếu file quá lớn
                        ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 2);
                        buffer.flip();
                        newBuffer.put(buffer);
                        buffer = newBuffer;
                    }
                }
            }
        }
        buffer.flip();
        return buffer;
    }
}