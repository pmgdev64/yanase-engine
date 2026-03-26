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
    private float padding = 15f; // Khoảng cách lề nội bộ cho các widget con

    public Panel2D(String name, float x, float y, float w, float h) {
        super(name);
        this.position.x = x;
        this.position.y = y;
        this.width = w;
        this.height = h;
    }

    public Panel2D(String name, float x, float w, boolean autoFullHeight) {
        super(name);
        this.position.x = x;
        this.position.y = 0;
        this.width = w;
        this.autoFullHeight = autoFullHeight;
    }

    /**
     * Ghi đè appendChild để tự động ép kích cỡ cho các widget con khi chúng được thêm vào
     */

    @Override
    public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) {
        // 1. Gọi appendChild của BaseNode để lưu vào danh sách children nội bộ
        org.w3c.dom.Node addedNode = super.appendChild(newChild);
        
        // 2. Kiểm tra nếu Node được thêm vào có khả năng thay đổi kích thước
        if (addedNode instanceof vn.pmgteam.yanase.gui.IResizable) {
            vn.pmgteam.yanase.gui.IResizable resizable = (vn.pmgteam.yanase.gui.IResizable) addedNode;
            
            // Tự động ép chiều rộng = Chiều rộng Panel hiện tại - lề (padding) 2 bên
            resizable.setWidth(this.width - (this.padding * 2));
            
            // 3. Tự động căn lề X (Tránh việc widget bị dính sát lề trái)
            // Vì Object2D chứa biến position, chúng ta ép kiểu để gán tọa độ x
            if (addedNode instanceof vn.pmgteam.yanase.node.Object2D) {
                ((vn.pmgteam.yanase.node.Object2D) addedNode).position.x = this.padding;
            }
        }
        
        return addedNode;
    }

    @Override
    public void render2D() {
        if (autoFullHeight) {
            this.height = Engine.getEngine().getWindowHeight(); 
        }

        glPushMatrix();
        // Hệ tọa độ Local: Mọi con bên trong sẽ bắt đầu từ (0,0) của Panel
        glTranslatef(position.x, position.y, 0); 

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Vẽ Background
        glBegin(GL_QUADS);
            glColor4f(0.1f, 0.11f, 0.12f, 0.85f); 
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // Vẽ đường kẻ ngăn cách (Border)
        glLineWidth(1.0f);
        glBegin(GL_LINES);
            glColor4f(0.3f, 0.3f, 0.3f, 1.0f);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            // Nếu là panel bên phải, vẽ thêm viền trái
            glVertex2f(0, 0);
            glVertex2f(0, height);
        glEnd();

        // LƯU Ý: Không vẽ con ở đây. Hàm renderRecursive2D của hệ thống 
        // sẽ tự đệ quy vào các con của Panel sau khi lệnh glTranslatef này có hiệu lực.

        glPopMatrix();
        glDisable(GL_BLEND);
    }

    // --- Layout Helpers ---
    public void setPadding(float padding) { 
        this.padding = padding; 
    }

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
    
    // Giữ lại các hàm DOM trống bên dưới nếu Engine yêu cầu, 
    // nhưng tốt nhất nên xóa sạch để code gọn hơn cho máy 4GB RAM.
}