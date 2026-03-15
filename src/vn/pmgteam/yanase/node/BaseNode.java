package vn.pmgteam.yanase.node;

import org.w3c.dom.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseNode implements Node {
    protected BaseNode parent; // Đổi từ Node sang BaseNode
    // Chuyển danh sách con sang kiểu BaseNode của Engine
    protected List<BaseNode> children = new ArrayList<>(); 
    protected String nodeName;

    public BaseNode(String name) { this.nodeName = name; }
    
    public String getName() {
        return nodeName;
    }
    
    // Bây giờ kiểu dữ liệu đã khớp hoàn toàn
    public List<BaseNode> getChildren() {
        return children;
    }

    public void cleanup() {
        for (BaseNode child : children) {
            child.cleanup(); // Gọi trực tiếp không cần ép kiểu phức tạp
        }
        children.clear();
    }
    
    // Thêm vào BaseNode.java
    public float getGlobalX() {
        float gx = (this instanceof Object2D) ? ((Object2D)this).position.x : 0;
        BaseNode p = (BaseNode) getParentNode();
        if (p != null) gx += p.getGlobalX();
        return gx;
    }

    public float getGlobalY() {
        float gy = (this instanceof Object2D) ? ((Object2D)this).position.y : 0;
        BaseNode p = (BaseNode) getParentNode();
        if (p != null) gy += p.getGlobalY();
        return gy;
    }

    @Override
    public Node appendChild(Node newChild) {
        if (newChild instanceof BaseNode) {
            BaseNode baseChild = (BaseNode) newChild;
            baseChild.parent = this;
            children.add(baseChild);
            return baseChild;
        }
        return null; // Hoặc throw exception nếu không phải BaseNode
    }

    // --- Các hàm DOM bắt buộc phải Override ---
    
    @Override 
    public NodeList getChildNodes() {
        return new NodeList() {
            @Override public Node item(int index) { return children.get(index); }
            @Override public int getLength() { return children.size(); }
        };
    }

    @Override public Node getParentNode() { return parent; }
    @Override public Node getFirstChild() { return children.isEmpty() ? null : children.get(0); }
    @Override public Node getLastChild() { return children.isEmpty() ? null : children.get(children.size()-1); }
    @Override public boolean hasChildNodes() { return !children.isEmpty(); }
    @Override public String getNodeName() { return nodeName; }
    @Override public short getNodeType() { return Node.ELEMENT_NODE; }

    // Các hàm stub khác giữ nguyên...
    @Override public String getNodeValue() { return null; }
    @Override public Node getNextSibling() { return null; }
    @Override public NamedNodeMap getAttributes() { return null; }
    @Override public Document getOwnerDocument() { return null; }
    @Override public Node getPreviousSibling() { return null; }
    @Override public Node insertBefore(Node n, Node r) { return null; }
    @Override public Node replaceChild(Node n, Node o) { return null; }
    @Override public Node removeChild(Node o) { 
        if(o instanceof BaseNode) children.remove((BaseNode)o); 
        return o; 
    }
    // ... implement các hàm còn lại của interface Node
}