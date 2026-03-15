package vn.pmgteam.yanase.gui.node;

import static org.lwjgl.opengl.GL11.*;
import java.awt.Color;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object2D;
import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.gui.IClickable;
import vn.pmgteam.yanase.gui.FontRenderer; // FontRenderer bạn vừa viết
import vn.pmgteam.yanase.base.Engine;      // Giả sử FontRenderer nằm trong Engine

public class Button2D extends Object2D implements IClickable {

    private float width, height;
    private float[] color = {0.25f, 0.25f, 0.25f}; 
    private String text = "";
    private Runnable callback;
    
    // Quản lý Texture của Text để tránh Memory Leak
    private FontRenderer.TextTextureData textTextureData;
    private boolean needsTextUpdate = false;

    public Button2D(String name, float w, float h) {
        super(name);
        this.width = w;
        this.height = h;
    }

    public Button2D(String name, float x, float y, float w, float h) {
        super(name);
        this.position.x = x;
        this.position.y = y;
        this.width = w;
        this.height = h;
    }

    public void setText(String text) { 
        if (!this.text.equals(text)) {
            this.text = text;
            this.needsTextUpdate = true; // Đánh dấu cần render lại texture chữ
        }
    }

    public void setCallback(Runnable action) { this.callback = action; }

    public void performClick() {
        if (callback != null) {
            System.out.println("[UI] Button Clicked: " + getName());
            callback.run();
        }
    }

    @Override
    public void render2D() {
        glPushMatrix();
        
        glTranslatef(position.x, position.y, 0);
        glRotatef(rotation, 0, 0, 1);
        glScalef(scale.x, scale.y, 1);

        // 1. Vẽ thân nút
        glBegin(GL_QUADS);
            glColor3fv(color);
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // 2. Vẽ viền
        glLineWidth(1.0f);
        glBegin(GL_LINE_LOOP);
            glColor3f(0.8f, 0.8f, 0.8f); 
            glVertex2f(0, 0);
            glVertex2f(width, 0);
            glVertex2f(width, height);
            glVertex2f(0, height);
        glEnd();

        // 3. VẼ TEXT (Sử dụng FontRenderer của bạn)
        renderText();

        // 4. Render các thành phần con
        for (BaseNode child : getChildren()) {
            if (child instanceof Object2D) {
                ((Object2D) child).render2D();
            }
        }

        glPopMatrix();
        glColor3f(1, 1, 1); // Reset màu
    }

    private void renderText() {
        if (text == null || text.isEmpty()) return;

        FontRenderer fr = Engine.getEngine().getFontRenderer(); // Truy cập FontRenderer chung
        
        // Cập nhật texture nếu text thay đổi
        if (needsTextUpdate || textTextureData == null) {
            if (textTextureData != null) {
                fr.deleteTexture(textTextureData.id); // Xóa texture cũ trên GPU
            }
            textTextureData = fr.getStringTextureData(text, Color.WHITE);
            needsTextUpdate = false;
        }

        if (textTextureData != null) {
            // Căn giữa text vào giữa nút
            float tx = (width - textTextureData.width) / 2;
            float ty = (height - textTextureData.height) / 2;
            fr.drawCachedTexture(textTextureData.id, tx, ty, textTextureData.width, textTextureData.height);
        }
    }

    @Override
    public boolean isMouseOver(float mx, float my) {
        // Nếu nút trong Group, nhớ dùng getGlobalX()
        return mx >= position.x && mx <= position.x + width &&
               my >= position.y && my <= position.y + height;
    }

    @Override
    public void onMousePressed(float mx, float my, int button) {
        if (button == 0) {
            setColor(0.15f, 0.15f, 0.15f); // Nhấn vào thì tối màu đi
            performClick();
        }
    }

    @Override
    public void onMouseReleased(float mx, float my, int button) {
        setColor(0.25f, 0.25f, 0.25f); // Thả ra trả lại màu xám đậm
    }

    @Override
    public void cleanup() {
        // RẤT QUAN TRỌNG: Giải phóng Texture khỏi GPU khi xóa Node
        if (textTextureData != null) {
            Engine.getEngine().getFontRenderer().deleteTexture(textTextureData.id);
        }
        super.cleanup();
    }

    public void setColor(float r, float g, float b) {
        this.color[0] = r; this.color[1] = g; this.color[2] = b;
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