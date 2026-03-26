package vn.pmgteam.yanase.node.subnodes;

import static org.lwjgl.opengl.GL11.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.util.RenderUtils;

public class Plane3D extends Object3D {

    public Plane3D(String name) {
        super(name);
    }

    @Override
    public void render() {
        glPushMatrix();
        
        // 1. Áp dụng Ma trận thế giới (World Matrix)
        RenderUtils.matrixBuffer.clear();
        worldMatrix.get(RenderUtils.matrixBuffer);
        glMultMatrixf(RenderUtils.matrixBuffer);

        // 2. Vẽ mặt phẳng (Ground)
        glBegin(GL_QUADS);
            glColor3f(0.2f, 0.4f, 0.2f); // Màu xanh cỏ tối
            glVertex3f(-10.0f, 0.0f,  10.0f);
            glVertex3f( 10.0f, 0.0f,  10.0f);
            glVertex3f( 10.0f, 0.0f, -10.0f);
            glVertex3f(-10.0f, 0.0f, -10.0f);
        glEnd();

        // 3. Đệ quy render các Node con
        // Vì children nằm trong BaseNode, bạn có thể duyệt trực tiếp
        for (Node child : children) {
            if (child instanceof Object3D) {
                ((Object3D) child).render();
            }
        }

        glPopMatrix();
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