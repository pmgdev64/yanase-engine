package vn.pmgteam.yanase.node;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.BufferUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.util.RenderUtils;

import java.nio.FloatBuffer;

public class Box3D extends Object3D {
    private int vaoId, vboId;
    private int vertexCount;

    public Box3D(String name) {
        super(name);
        initMesh();
    }

    private void initMesh() {
    	float[] vertices = {
    		    // Tọa độ (X, Y, Z)      // Màu sắc (R, G, B)
    		    // Mặt trước (Z+) - Đỏ
    		    -0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
    		     0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
    		     0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,
    		    -0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 0.0f,

    		    // Mặt sau (Z-) - Xanh lá
    		    -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
    		    -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
    		     0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 0.0f,
    		     0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,

    		    // Mặt trên (Y+) - Xanh dương
    		    -0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,
    		    -0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
    		     0.5f,  0.5f,  0.5f,  0.0f, 0.0f, 1.0f,
    		     0.5f,  0.5f, -0.5f,  0.0f, 0.0f, 1.0f,

    		    // Mặt dưới (Y-) - Vàng
    		    -0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
    		     0.5f, -0.5f, -0.5f,  1.0f, 1.0f, 0.0f,
    		     0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.0f,
    		    -0.5f, -0.5f,  0.5f,  1.0f, 1.0f, 0.0f,

    		    // Mặt phải (X+) - Tím
    		     0.5f, -0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
    		     0.5f,  0.5f, -0.5f,  1.0f, 0.0f, 1.0f,
    		     0.5f,  0.5f,  0.5f,  1.0f, 0.0f, 1.0f,
    		     0.5f, -0.5f,  0.5f,  1.0f, 0.0f, 1.0f,

    		    // Mặt trái (X-) - Cyan
    		    -0.5f, -0.5f, -0.5f,  0.0f, 1.0f, 1.0f,
    		    -0.5f, -0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
    		    -0.5f,  0.5f,  0.5f,  0.0f, 1.0f, 1.0f,
    		    -0.5f,  0.5f, -0.5f,  0.0f, 1.0f, 1.0f
    		};
        vertexCount = vertices.length / 6;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        // Attribute 0: Position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Attribute 1: Color
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
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

        // Bật trạng thái Client để nhận mảng đỉnh và mảng màu
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        // Chỉ rõ cho OpenGL: 3 số đầu là Vertex, 3 số sau là Color (Stride = 6 * 4 bytes)
        glVertexPointer(3, GL_FLOAT, 6 * Float.BYTES, 0);
        glColorPointer(3, GL_FLOAT, 6 * Float.BYTES, 3 * Float.BYTES);

        glDrawArrays(GL_QUADS, 0, vertexCount);

        // Tắt trạng thái sau khi vẽ để không ảnh hưởng Node khác
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();
    }
    
    // Đừng quên cleanup khi xóa object
    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node getPreviousSibling() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node removeChild(Node oldChild) throws DOMException {
		// TODO Auto-generated method stub
		return null;
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