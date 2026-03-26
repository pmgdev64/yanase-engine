package vn.pmgteam.yanase.node.subnodes;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.BufferUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.util.RenderUtils;
import java.nio.FloatBuffer;

public class Box3D extends Object3D {
    private int vaoId, vboId;
    private int vertexCount;
    private int textureId = -1; // -1 nghĩa là không có texture, dùng màu đơn sắc

    public Box3D(String name) {
        super(name);
        initMesh();
    }

    // HÀM GÁN TEXTURE BẠN CẦN
    public void setTexture(int id) {
        this.textureId = id;
    }

    private void initMesh() {
        // Cấu trúc: 3 float Position + 2 float Texture Coord (UV)
        float[] vertices = {
            // Mặt trước (Z+)
            -0.5f, -0.5f,  0.5f,  0, 1,   0.5f, -0.5f,  0.5f,  1, 1,   0.5f,  0.5f,  0.5f,  1, 0,  -0.5f,  0.5f,  0.5f,  0, 0,
            // Mặt sau (Z-)
            -0.5f, -0.5f, -0.5f,  1, 1,  -0.5f,  0.5f, -0.5f,  1, 0,   0.5f,  0.5f, -0.5f,  0, 0,   0.5f, -0.5f, -0.5f,  0, 1,
            // Mặt trên (Y+)
            -0.5f,  0.5f, -0.5f,  0, 0,  -0.5f,  0.5f,  0.5f,  0, 1,   0.5f,  0.5f,  0.5f,  1, 1,   0.5f,  0.5f, -0.5f,  1, 0,
            // Mặt dưới (Y-)
            -0.5f, -0.5f, -0.5f,  0, 1,   0.5f, -0.5f, -0.5f,  1, 1,   0.5f, -0.5f,  0.5f,  1, 0,  -0.5f, -0.5f,  0.5f,  0, 0,
            // Mặt phải (X+)
             0.5f, -0.5f, -0.5f,  1, 1,   0.5f,  0.5f, -0.5f,  1, 0,   0.5f,  0.5f,  0.5f,  0, 0,   0.5f, -0.5f,  0.5f,  0, 1,
            // Mặt trái (X-)
            -0.5f, -0.5f, -0.5f,  0, 1,  -0.5f, -0.5f,  0.5f,  1, 1,  -0.5f,  0.5f,  0.5f,  1, 0,  -0.5f,  0.5f, -0.5f,  0, 0
        };
        vertexCount = vertices.length / 5;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        // Attribute 0: Position (3 float)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Attribute 1: Texture Coords (2 float)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    @Override
    public void render() {
        glPushMatrix();
        RenderUtils.matrixBuffer.clear();
        worldMatrix.get(RenderUtils.matrixBuffer);
        glMultMatrixf(RenderUtils.matrixBuffer);

        if (textureId != -1) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glColor3f(1, 1, 1); // Reset màu về trắng để hiển thị đúng màu ảnh PNG
        } else {
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.5f, 0.5f, 0.5f); // Màu mặc định nếu không có texture
        }

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY); // Bật mảng tọa độ texture

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        // Pointer cho đỉnh (3 float đầu)
        glVertexPointer(3, GL_FLOAT, 5 * Float.BYTES, 0);
        // Pointer cho UV (2 float sau)
        glTexCoordPointer(2, GL_FLOAT, 5 * Float.BYTES, 3 * Float.BYTES);

        glDrawArrays(GL_QUADS, 0, vertexCount);

        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        
        if (textureId != -1) glDisable(GL_TEXTURE_2D);
        
        glPopMatrix();
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node cloneNode(boolean deep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void normalize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSupported(String feature, String version) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNamespaceURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getBaseURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTextContent() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSameNode(Node other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEqualNode(Node arg) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getFeature(String feature, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getUserData(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}