package vn.pngteam.yanase.node;

import org.w3c.dom.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseNode implements Node {
    protected Node parent;
    // Đổi sang List<Object3D> nếu có thể, hoặc giữ Node nhưng truy cập trực tiếp
    public List<Node> children = new ArrayList<>(); 
    protected String nodeName;

    public BaseNode(String name) { this.nodeName = name; }

    @Override public String getNodeName() { return nodeName; }
    @Override public Node getParentNode() { return parent; }

    // TỐI ƯU: Không tạo Object mới mỗi frame nữa
    private final NodeList cachedList = new NodeList() {
        @Override public Node item(int index) { return children.get(index); }
        @Override public int getLength() { return children.size(); }
    };

    @Override public NodeList getChildNodes() {
        return cachedList; 
    }

    @Override
    public Node appendChild(Node newChild) {
        if (newChild instanceof BaseNode) {
            ((BaseNode) newChild).parent = this;
        }
        children.add(newChild);
        return newChild;
    }

    // --- Giữ nguyên các hàm khác để không lỗi Object3D ---
    @Override public String getNodeValue() { return null; }
    @Override public short getNodeType() { return Node.ELEMENT_NODE; }
    @Override public Node getFirstChild() { return children.isEmpty() ? null : children.get(0); }
    @Override public Node getLastChild() { return children.isEmpty() ? null : children.get(children.size()-1); }
    @Override public boolean hasChildNodes() { return !children.isEmpty(); }
    @Override public Node getNextSibling() { return null; }
    @Override public NamedNodeMap getAttributes() { return null; }
    @Override public Document getOwnerDocument() { return null; }
}