package vn.pmgteam.yanase.node;

import org.lwjgl.assimp.*;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import static org.lwjgl.assimp.Assimp.*;

/**
 * @deprecated 
 * MMD nạp qua Assimp JAR. 
 * Lưu ý: Assimp không xử lý tốt các "Physics Bone" của MMD.
 */
@Deprecated
public class MmdModel extends Object3D {

    private AIScene scene;

    public MmdModel(String name, String path) {
        super(name);
        loadMmdWithAssimp(path);
    }

    /**
     * @deprecated Phương thức nạp này có thể gây treo quạt CPU Pentium G5400.
     */
    @Deprecated
    private void loadMmdWithAssimp(String path) {
        // Cờ xử lý model: Triangulate (Chia tam giác) và FlipUVs (Đảo ngược tọa độ ảnh)
        int flags = aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_OptimizeMeshes;

        // Đây là nơi dấu gạch ngang xuất hiện nếu bạn gọi hàm này
        scene = aiImportFile(path, flags);

        if (scene == null || scene.mRootNode() == null) {
            System.err.println("[Assimp] Không thể nạp MMD: " + aiGetErrorString());
            return;
        }
        
        System.out.println("[Yanase] Nạp thành công " + scene.mNumMeshes() + " meshes từ MMD.");
    }

    @Override
    @Deprecated
    public void render() {
        if (scene == null) return;
        // Logic render duyệt qua từng Mesh trong scene...
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