package vn.pmgteam.yanase.node.subnodes;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object3D;

/**
 * Node này đóng vai trò như một thư mục hoặc "Empty Object".
 * Dùng để làm rootNode cho Scene mà không cần vẽ gì cả.
 */
public class GroupNode extends Object3D {
    public GroupNode(String name) {
        super(name);
    }

    @Override
    public void render() {
        // Không vẽ gì, chỉ đóng vai trò vật chứa
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