package vn.pmgteam.yanase.util;

import org.joml.Vector3f;
import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.node.BaseNode;
import vn.pmgteam.yanase.node.CameraNode;

public class Raycaster {
    private static final float MAX_DISTANCE = 100.0f;
    
    // Biến tạm để lưu kết quả trong quá trình duyệt đệ quy
    private static Object3D closestObject = null;
    private static float minDistance = MAX_DISTANCE;

    /**
     * Hàm chính: Duyệt toàn bộ Scene Graph từ root để tìm vật thể bị nhắm trúng
     */
    public static Object3D findSelectedObject(Vector3f origin, Vector3f direction, BaseNode root) {
        closestObject = null;
        minDistance = MAX_DISTANCE;
        
        searchRecursive(origin, direction, root);
        
        return closestObject;
    }

    private static void searchRecursive(Vector3f origin, Vector3f direction, BaseNode node) {
        // Chỉ kiểm tra nếu là Object3D và không phải Camera
        if (node instanceof Object3D && !(node instanceof CameraNode)) {
            Object3D obj = (Object3D) node;
            
            // Tính khoảng cách va chạm
            float dist = intersectSphere(origin, direction, obj.position, 1.2f); // Bán kính 1.2f để dễ chọn hơn chút
            
            if (dist > 0 && dist < minDistance) {
                minDistance = dist;
                closestObject = obj;
            }
        }

        // Đệ quy xuống các con
        for (BaseNode child : node.getChildren()) {
            searchRecursive(origin, direction, child);
        }
    }

    /**
     * Thuật toán kiểm tra tia đâm xuyên hình cầu (Sphere Intersection)
     * Trả về khoảng cách từ origin đến điểm va chạm, hoặc -1 nếu trượt.
     */
    private static float intersectSphere(Vector3f origin, Vector3f dir, Vector3f center, float radius) {
        // Chuyển đổi tọa độ target sang Vector3f của JOML để tính toán
        Vector3f L = new Vector3f(center.x - origin.x, center.y - origin.y, center.z - origin.z);
        float tca = L.dot(dir);
        
        if (tca < 0) return -1;
        
        float d2 = L.dot(L) - tca * tca;
        if (d2 > radius * radius) return -1;
        
        float thc = (float) Math.sqrt(radius * radius - d2);
        return tca - thc;
    }
}