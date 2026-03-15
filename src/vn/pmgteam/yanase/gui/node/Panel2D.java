package vn.pmgteam.yanase.gui.node;

import static org.lwjgl.opengl.GL11.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object2D;
import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.base.Engine;

public class Panel2D extends Object2D {
    private float width, height;
    private boolean autoFullHeight = false;

    public Panel2D(String name, float x, float y, float w, float h) {
        super(name);
        this.position.x = x;
        this.position.y = y;
        this.width = w;
        this.height = h;
    }

    /**
     * Constructor đặc biệt để tạo SideBar tự động full màn hình
     */
    public Panel2D(String name, float x, float w, boolean autoFullHeight) {
        super(name);
        this.position.x = x;
        this.position.y = 0;
        this.width = w;
        this.autoFullHeight = autoFullHeight;
    }

    @Override
    public void render2D() {
        // Ép Panel luôn cao bằng cửa sổ hiện tại
        this.height = Engine.getEngine().getWindowHeight(); 

        glPushMatrix();
        // Đảm bảo position.y = 0 để nó bắt đầu từ đỉnh màn hình
        glTranslatef(position.x, 0, 0); 

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        glBegin(GL_QUADS);
            glColor4f(0.1f, 0.11f, 0.12f, 0.8f); // Màu xám đen Editor
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // Vẽ viền phải để tách biệt vùng làm việc 3D
        glLineWidth(1.0f);
        glBegin(GL_LINES);
            glColor4f(0.2f, 0.2f, 0.2f, 1.0f);
            glVertex2f(width, 0);
            glVertex2f(width, height);
        glEnd();

        // Render các con (Checkbox, Button...)
        for (BaseNode child : getChildren()) {
            if (child instanceof Object2D) {
                ((Object2D) child).render2D();
            }
        }

        glPopMatrix();
        glDisable(GL_BLEND);
    }

    // --- GETTERS & SETTERS ---
    public float getWidth() { return width; }
    public float getHeight() { return height; }

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
    
    // XÓA BỎ TOÀN BỘ các hàm @Override của org.w3c.dom.Node tại đây để tiết kiệm RAM 4GB
}