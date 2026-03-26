package vn.pmgteam.yanase.node.subnodes;

import static org.lwjgl.opengl.GL11.*;
import org.w3c.dom.*;
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.util.RenderUtils;

public class Sphere3D extends Object3D {
    private int segments;

    public Sphere3D(String name) {
        super(name);
        this.segments = 16;
    }

    @Override
    public void render() {
        glPushMatrix();
        RenderUtils.matrixBuffer.clear();
        worldMatrix.get(RenderUtils.matrixBuffer);
        glMultMatrixf(RenderUtils.matrixBuffer);

        glColor3f(0.6f, 0.6f, 0.8f);
        // Vẽ sphere bằng GL_LINE_LOOP (wireframe đơn giản, nhẹ RAM)
        for (int i = 0; i < segments; i++) {
            glBegin(GL_LINE_LOOP);
            for (int j = 0; j < segments; j++) {
                double theta = 2.0 * Math.PI * j / segments;
                double phi = Math.PI * i / segments;
                float x = (float)(Math.sin(phi) * Math.cos(theta));
                float y = (float)(Math.cos(phi));
                float z = (float)(Math.sin(phi) * Math.sin(theta));
                glVertex3f(x * 0.5f, y * 0.5f, z * 0.5f);
            }
            glEnd();
        }
        glPopMatrix();
    }

    // DOM stubs
    @Override public void setNodeValue(String v) throws DOMException {}
    @Override public Node getPreviousSibling() { return null; }
    @Override public Node insertBefore(Node n, Node r) throws DOMException { return null; }
    @Override public Node replaceChild(Node n, Node o) throws DOMException { return null; }
    @Override public Node removeChild(Node o) throws DOMException { return null; }
    @Override public Node cloneNode(boolean d) { return null; }
    @Override public void normalize() {}
    @Override public boolean isSupported(String f, String v) { return false; }
    @Override public String getNamespaceURI() { return null; }
    @Override public String getPrefix() { return null; }
    @Override public void setPrefix(String p) throws DOMException {}
    @Override public String getLocalName() { return null; }
    @Override public boolean hasAttributes() { return false; }
    @Override public String getBaseURI() { return null; }
    @Override public short compareDocumentPosition(Node o) throws DOMException { return 0; }
    @Override public String getTextContent() throws DOMException { return null; }
    @Override public void setTextContent(String t) throws DOMException {}
    @Override public boolean isSameNode(Node o) { return false; }
    @Override public String lookupPrefix(String n) { return null; }
    @Override public boolean isDefaultNamespace(String n) { return false; }
    @Override public String lookupNamespaceURI(String p) { return null; }
    @Override public boolean isEqualNode(Node a) { return false; }
    @Override public Object getFeature(String f, String v) { return null; }
    @Override public Object setUserData(String k, Object d, UserDataHandler h) { return null; }
    @Override public Object getUserData(String k) { return null; }
}