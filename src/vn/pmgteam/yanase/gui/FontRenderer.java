package vn.pmgteam.yanase.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Class hỗ trợ render văn bản bằng AWT Font và OpenGL Texture.
 */
public class FontRenderer {
    
    private Font awtFont;

    /**
     * Lớp chứa dữ liệu Texture bao gồm ID và kích thước thực tế.
     */
    public static class TextTextureData {
        public final int id;
        public final int width;
        public final int height;

        public TextTextureData(int id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }

    public FontRenderer(String fontName, int size) {
        this.awtFont = new Font(fontName, Font.PLAIN, size);
    }

    /**
     * Tạo texture từ chuỗi văn bản và trả về đối tượng chứa ID, width, height.
     */
    public TextTextureData getStringTextureData(String text, Color color) {
        // 1. Tính toán kích thước chuỗi dựa trên FontMetrics
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();
        g2d.setFont(awtFont);
        FontMetrics metrics = g2d.getFontMetrics();
        
        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();
        int ascent = metrics.getAscent();
        g2d.dispose();

        // Trường hợp chuỗi rỗng
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;

        // 2. Vẽ văn bản lên BufferedImage
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        
        // Khử răng cưa cho chữ mượt hơn
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setFont(awtFont);
        g2d.setColor(color);
        g2d.drawString(text, 0, ascent);
        g2d.dispose();

        // 3. Upload lên GPU
        int textureID = uploadTexture(img);
        
        return new TextTextureData(textureID, width, height);
    }

    private int uploadTexture(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        
        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();

        int textureID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Thiết lập tham số Texture
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        return textureID;
    }

    /**
     * Vẽ texture đã được cache lên màn hình tại tọa độ x, y với kích thước tùy chỉnh.
     */
    public void drawCachedTexture(int textureID, float x, float y, float width, float height) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        
        // Bật Alpha Blending để hỗ trợ độ trong suốt của Font
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Trả về độ rộng của chuỗi văn bản tính bằng pixel dựa trên font hiện tại.
     */
    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        
        // Sử dụng một BufferedImage 1x1 tạm thời để lấy Graphics context
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();
        g2d.setFont(this.awtFont);
        FontMetrics metrics = g2d.getFontMetrics();
        int width = metrics.stringWidth(text);
        g2d.dispose();
        
        return (float) width;
    }
    
    /**
     * Giải phóng một texture cụ thể khi không còn sử dụng.
     * Cần gọi hàm này sau khi vẽ xong các chuỗi văn bản không cố định.
     */
    public void deleteTexture(int textureID) {
        if (textureID != -1) {
            GL11.glDeleteTextures(textureID);
        }
    }

    /**
     * Dọn dẹp tài nguyên Font (nếu có cache nội bộ sau này).
     */
    public void cleanup() {
        // Hiện tại awtFont do JVM quản lý, nhưng nếu bạn có 
        // danh sách cache TextureID ở đây, hãy loop để glDeleteTextures toàn bộ.
        awtFont = null;
    }
}