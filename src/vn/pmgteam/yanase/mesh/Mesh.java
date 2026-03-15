package vn.pmgteam.yanase.mesh;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

public class Mesh {
    private int vboId;
    private int textureId = -1;
    private int vertexCount;
    private float[] color; // màu từ baseColorFactor, dùng khi không có texture

    private static final int STRIDE     = 5 * Float.BYTES; // 20 bytes [X Y Z U V]
    private static final int OFFSET_POS = 0;
    private static final int OFFSET_UV  = 3 * Float.BYTES;
    
 // Trong class Mesh.java
    public static int renderVerticesCount = 0;
    public static int renderTrianglesCount = 0;

    public Mesh(float[] vertices, int textureId) {
        this(vertices, textureId, new float[]{1f, 1f, 1f, 1f});
    }

    public Mesh(float[] vertices, int textureId, float[] color) {
        int vertCount    = vertices.length / 8;
        this.vertexCount = vertCount;
        this.textureId   = textureId;
        this.color       = color;
        

        float[] compact = new float[vertCount * 5];
        for (int i = 0; i < vertCount; i++) {
            compact[i * 5 + 0] = vertices[i * 8 + 0]; // X
            compact[i * 5 + 1] = vertices[i * 8 + 1]; // Y
            compact[i * 5 + 2] = vertices[i * 8 + 2]; // Z
            compact[i * 5 + 3] = vertices[i * 8 + 6]; // U
            compact[i * 5 + 4] = vertices[i * 8 + 7]; // V
        }
        initVBO(compact);
    }

    private void initVBO(float[] data) {
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public int getVerticesCount() {
        return this.vertexCount;
    }
    
    public void render() {
    	renderVerticesCount += this.vertexCount;
        renderTrianglesCount += this.vertexCount / 3;
        if (textureId == -1) {
            // Outline mesh: render màu đen viền bằng cách cull front face
            // Chỉ render nếu color không phải trắng (tránh render rác)
            float r = color[0], g = color[1], b = color[2], a = color.length > 3 ? color[3] : 1f;
            if (r > 0.9f && g > 0.9f && b > 0.9f) return; // bỏ qua mesh trắng không texture

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(r, g, b, a);

            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glEnableClientState(GL_VERTEX_ARRAY);
            glVertexPointer(3, GL_FLOAT, STRIDE, OFFSET_POS);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glDisableClientState(GL_VERTEX_ARRAY);
            glDisable(GL_BLEND);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            return;
        }

        // Mesh có texture — render bình thường
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(3, GL_FLOAT, STRIDE, OFFSET_POS);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(2, GL_FLOAT, STRIDE, OFFSET_UV);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

        glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    public static void resetFrameStats() {
        renderVerticesCount = 0;
        renderTrianglesCount = 0;
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
    }
}