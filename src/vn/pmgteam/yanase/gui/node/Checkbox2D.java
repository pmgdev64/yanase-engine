package vn.pmgteam.yanase.gui.node;

import static org.lwjgl.opengl.GL11.*;
import java.awt.Color;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object2D;
import vn.pmgteam.yanase.gui.IClickable;
import vn.pmgteam.yanase.gui.FontRenderer;
import vn.pmgteam.yanase.base.Engine;

public class Checkbox2D extends Object2D implements IClickable {
    private boolean checked = false;
    private float size;
    private String label;
    private Runnable onToggle;
    
    // Cache texture cho label để không tạo mới mỗi frame
    private FontRenderer.TextTextureData labelData;

    public Checkbox2D(String name, String label, float x, float y, float size) {
        super(name);
        this.label = label;
        this.position.x = x;
        this.position.y = y;
        this.size = size;
    }

    @Override
    public void render2D() {
        glPushMatrix();
        glTranslatef(position.x, position.y, 0);

        // 1. Vẽ khung ô tích
        glLineWidth(2.0f);
        glBegin(GL_LINE_LOOP);
            glColor3f(0.7f, 0.7f, 0.7f);
            glVertex2f(0, 0); glVertex2f(size, 0);
            glVertex2f(size, size); glVertex2f(0, size);
        glEnd();

        // 2. Nếu đang check, vẽ ô đặc bên trong
        if (checked) {
            glBegin(GL_QUADS);
                glColor3f(0.2f, 0.6f, 1.0f);
                float padding = size * 0.2f;
                glVertex2f(padding, padding);
                glVertex2f(size - padding, padding);
                glVertex2f(size - padding, size - padding);
                glVertex2f(padding, size - padding);
            glEnd();
        }

        // 3. Vẽ Label (Tối ưu Cache)
        if (label != null && !label.isEmpty()) {
            FontRenderer fr = Engine.getEngine().getFontRenderer();
            if (labelData == null) {
                labelData = fr.getStringTextureData(label, Color.WHITE);
            }
            // Vẽ label lệch sang phải 10px so với ô checkbox
            fr.drawCachedTexture(labelData.id, size + 10, (size / 2) - (labelData.height / 2f), labelData.width, labelData.height);
        }

        glPopMatrix();
        glColor3f(1, 1, 1);
    }

    @Override
    public void onMousePressed(float mx, float my, int button) {
        if (button == 0) { // Chuột trái
            this.checked = !this.checked;
            if (onToggle != null) onToggle.run();
        }
    }

    @Override
    public boolean isMouseOver(float mx, float my) {
        // Mở rộng vùng click bao gồm cả phần chữ để dễ bấm hơn
        float totalWidth = size + (labelData != null ? labelData.width + 10 : 0);
        return mx >= position.x && mx <= position.x + totalWidth &&
               my >= position.y && my <= position.y + size;
    }

    @Override
    public void cleanup() {
        if (labelData != null) {
            Engine.getEngine().getFontRenderer().deleteTexture(labelData.id);
        }
        super.cleanup();
    }

    public void setOnToggle(Runnable action) { this.onToggle = action; }
    public boolean isChecked() { return checked; }
    public void setChecked(boolean state) { this.checked = state; }

    @Override
    public void onMouseReleased(float mx, float my, int button) {}

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