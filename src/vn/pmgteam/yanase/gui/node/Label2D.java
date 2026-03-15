package vn.pmgteam.yanase.gui.node;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.gui.FontRenderer;
import vn.pmgteam.yanase.node.Object2D;

public class Label2D extends Object2D {
    private String text = "";
    private String lastText = "";
    private java.awt.Color color = java.awt.Color.WHITE;
    private FontRenderer.TextTextureData cachedData = null;
    
    // Giả sử FontRenderer được quản lý tập trung để tiết kiệm RAM
    private static FontRenderer fontRenderer = new FontRenderer("JetBrains Mono", 14);

    public Label2D(String name, String text) {
        super(name);
        this.text = (text != null) ? text : "";
    }

    @Override
    public void render2D() {
        // Chỉ nạp lại Texture khi có sự thay đổi nội dung
        if (!text.equals(lastText)) {
            if (cachedData != null) {
                fontRenderer.deleteTexture(cachedData.id);
            }
            
            cachedData = fontRenderer.getStringTextureData(text, color);
            lastText = text;
        }
        
        // Vẽ texture ra màn hình tại tọa độ (x, y) của Object2D
        if (cachedData != null) {
            // Giả sử Object2D có các biến x, y kế thừa từ cha
            fontRenderer.drawCachedTexture(cachedData.id, getX(), getY(), 
                                           cachedData.width, cachedData.height);
        }
    }
    
    @Override
    public void setTextContent(String textContent) throws DOMException {
        this.text = textContent;
    }

    @Override
    public String getTextContent() throws DOMException {
        return this.text;
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