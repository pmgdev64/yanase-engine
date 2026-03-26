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

public class GridNode extends Object3D {
    private int vaoId, vboId;
    private int vertexCount;
    private boolean visible = true;

    public GridNode(String name, int size, float step) {
        super(name);
        initGrid(size, step);
    }

    private void initGrid(int size, float step) {
        // Tính toán số lượng đỉnh cần thiết
        int linesPerSide = (int) (size * 2 / step) + 1;
        vertexCount = linesPerSide * 4 + 4; // Grid + 2 Trục chính
        
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertexCount * 6); // 3 Pos + 3 Color

        // Màu xám cho Grid
        float[] color = {0.4f, 0.4f, 0.4f};

        for (float i = -size; i <= size; i += step) {
            // Song song Z
            buffer.put(i).put(0).put(-size).put(color[0]).put(color[1]).put(color[2]);
            buffer.put(i).put(0).put(size).put(color[0]).put(color[1]).put(color[2]);
            // Song song X
            buffer.put(-size).put(0).put(i).put(color[0]).put(color[1]).put(color[2]);
            buffer.put(size).put(0).put(i).put(color[0]).put(color[1]).put(color[2]);
        }

        // Trục X (Đỏ)
        buffer.put(-size).put(0.01f).put(0).put(0.8f).put(0.2f).put(0.2f);
        buffer.put(size).put(0.01f).put(0).put(0.8f).put(0.2f).put(0.2f);
        // Trục Z (Xanh)
        buffer.put(0).put(0.01f).put(-size).put(0.2f).put(0.2f).put(0.8f);
        buffer.put(0).put(0.01f).put(size).put(0.2f).put(0.2f).put(0.8f);

        buffer.flip();

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    @Override
    public void render() {
    	if (!visible) return; // Dừng render nếu visible = false
        glPushMatrix();
        
        RenderUtils.matrixBuffer.clear();
        worldMatrix.get(RenderUtils.matrixBuffer);
        glMultMatrixf(RenderUtils.matrixBuffer);

        // Bật trạng thái Client để OpenGL cũ hiểu được mảng dữ liệu
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        
        // Cấu hình Pointer (6 * 4 bytes là khoảng cách giữa các đỉnh - Stride)
        glVertexPointer(3, GL_FLOAT, 6 * Float.BYTES, 0);
        glColorPointer(3, GL_FLOAT, 6 * Float.BYTES, 3 * Float.BYTES);

        // Vẽ Grid hoặc Mesh
        glDrawArrays(GL_LINES, 0, vertexCount);

        // Dọn dẹp để không lỗi các object khác
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
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
    
    // Đừng quên dọn dẹp các Override W3C Node thừa như MeshObject3D...
}