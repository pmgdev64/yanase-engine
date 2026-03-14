package vn.pngteam.yanase.util;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import vn.pngteam.yanase.node.Model3D;
import vn.pngteam.yanase.mesh.Mesh;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class ModelLoader {

    private static final Map<ImageModel, Integer> textureCache = new HashMap<>();

    public static Model3D loadGLB(String path) {
        textureCache.clear();
        try {
            GltfModelReader reader = new GltfModelReader();
            GltfModel gltfModel = reader.read(new File(path).toURI());

            int imgInFile = gltfModel.getImageModels().size();
            System.out.println("\n[Yanase Engine] Đang nạp: " + path);
            System.out.println("[Yanase Engine] Số lượng ảnh: " + imgInFile);

            Model3D model = new Model3D(new File(path).getName());

            // Duyệt theo node tree để lấy đúng transform của từng node
            for (SceneModel scene : gltfModel.getSceneModels()) {
                for (NodeModel rootNode : scene.getNodeModels()) {
                    processNode(rootNode, new float[]{
                        1,0,0,0,
                        0,1,0,0,
                        0,0,1,0,
                        0,0,0,1
                    }, model);
                }
            }

            System.out.println("[Yanase Engine] Hoàn tất. Texture: " + textureCache.size() + "/" + imgInFile + "\n");
            return model;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Duyệt đệ quy node tree, tích lũy transform matrix (column-major 4x4)
     */
    private static void processNode(NodeModel node, float[] parentMatrix, Model3D model) {
        // Tính local matrix của node này
        float[] localMatrix = getNodeMatrix(node);
        // Nhân với parent matrix
        float[] worldMatrix = multiply4x4(parentMatrix, localMatrix);

        // Nếu node có mesh → xử lý từng primitive
        List<MeshModel> meshModels = node.getMeshModels();
        if (meshModels != null) {
            for (MeshModel meshModel : meshModels) {
                for (MeshPrimitiveModel primitive : meshModel.getMeshPrimitiveModels()) {
                    Mesh mesh = buildMesh(primitive, meshModel, worldMatrix);
                    if (mesh != null) model.addMesh(mesh);
                }
            }
        }

        // Duyệt tiếp các node con
        for (NodeModel child : node.getChildren()) {
            processNode(child, worldMatrix, model);
        }
    }

    private static float[] getNodeMatrix(NodeModel node) {
        // Nếu node có matrix trực tiếp thì dùng
        float[] mat = node.getMatrix();
        if (mat != null && mat.length == 16) return mat.clone();

        // Ngược lại tính từ TRS
        float[] t = node.getTranslation(); // [x, y, z]
        float[] r = node.getRotation();    // [x, y, z, w] quaternion
        float[] s = node.getScale();       // [x, y, z]

        float tx = (t != null) ? t[0] : 0f;
        float ty = (t != null) ? t[1] : 0f;
        float tz = (t != null) ? t[2] : 0f;

        float qx = (r != null) ? r[0] : 0f;
        float qy = (r != null) ? r[1] : 0f;
        float qz = (r != null) ? r[2] : 0f;
        float qw = (r != null) ? r[3] : 1f;

        float sx = (s != null) ? s[0] : 1f;
        float sy = (s != null) ? s[1] : 1f;
        float sz = (s != null) ? s[2] : 1f;

        // Quaternion → rotation matrix (column-major)
        float[] m = new float[16];
        m[ 0] = (1 - 2*(qy*qy + qz*qz)) * sx;
        m[ 1] = (    2*(qx*qy + qz*qw)) * sx;
        m[ 2] = (    2*(qx*qz - qy*qw)) * sx;
        m[ 3] = 0;

        m[ 4] = (    2*(qx*qy - qz*qw)) * sy;
        m[ 5] = (1 - 2*(qx*qx + qz*qz)) * sy;
        m[ 6] = (    2*(qy*qz + qx*qw)) * sy;
        m[ 7] = 0;

        m[ 8] = (    2*(qx*qz + qy*qw)) * sz;
        m[ 9] = (    2*(qy*qz - qx*qw)) * sz;
        m[10] = (1 - 2*(qx*qx + qy*qy)) * sz;
        m[11] = 0;

        m[12] = tx;
        m[13] = ty;
        m[14] = tz;
        m[15] = 1;

        return m;
    }

    /** Nhân 2 ma trận 4x4 column-major: result = a * b */
    private static float[] multiply4x4(float[] a, float[] b) {
        float[] c = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += a[k*4 + row] * b[col*4 + k];
                }
                c[col*4 + row] = sum;
            }
        }
        return c;
    }

    /** Transform một điểm XYZ bằng ma trận 4x4 column-major */
    private static float[] transformPoint(float[] m, float x, float y, float z) {
        float rx = m[0]*x + m[4]*y + m[ 8]*z + m[12];
        float ry = m[1]*x + m[5]*y + m[ 9]*z + m[13];
        float rz = m[2]*x + m[6]*y + m[10]*z + m[14];
        return new float[]{rx, ry, rz};
    }

    private static Mesh buildMesh(MeshPrimitiveModel primitive, MeshModel meshModel, float[] worldMatrix) {
        try {
            AccessorModel posAcc   = primitive.getAttributes().get("POSITION");
            AccessorModel uvAcc    = primitive.getAttributes().get("TEXCOORD_0");
            AccessorModel indexAcc = primitive.getIndices();

            AccessorFloatData posData = (AccessorFloatData) posAcc.getAccessorData();
            AccessorFloatData uvData  = (uvAcc != null) ? (AccessorFloatData) uvAcc.getAccessorData() : null;

            int count = (indexAcc != null) ? indexAcc.getCount() : posAcc.getCount();
            float[] interleavedData = new float[count * 8];
            AccessorIntData indexData = (indexAcc != null) ? (AccessorIntData) indexAcc.getAccessorData() : null;

            // baseColorFactor cho mesh không có texture
            float[] baseColorFactor = new float[]{1f, 1f, 1f, 1f};
            MaterialModel mat = primitive.getMaterialModel();
            if (mat instanceof MaterialModelV2) {
                float[] factor = ((MaterialModelV2) mat).getBaseColorFactor();
                if (factor != null && factor.length >= 4) baseColorFactor = factor;
            }

            for (int i = 0; i < count; i++) {
                int idx = (indexData != null) ? indexData.get(i) : i;

                float lx = posData.get(idx * 3 + 0);
                float ly = posData.get(idx * 3 + 1);
                float lz = posData.get(idx * 3 + 2);

                // Apply world matrix transform
                float[] wp = transformPoint(worldMatrix, lx, ly, lz);

                interleavedData[i * 8 + 0] = wp[0];
                interleavedData[i * 8 + 1] = wp[1];
                interleavedData[i * 8 + 2] = wp[2];
                interleavedData[i * 8 + 3] = baseColorFactor[0];
                interleavedData[i * 8 + 4] = baseColorFactor[1];
                interleavedData[i * 8 + 5] = baseColorFactor[2];

                if (uvData != null) {
                    interleavedData[i * 8 + 6] = uvData.get(idx * 2 + 0);
                    interleavedData[i * 8 + 7] = uvData.get(idx * 2 + 1);
                }
            }

            // Lấy texture theo material
            int textureId = -1;
            if (mat instanceof MaterialModelV2) {
                MaterialModelV2 matV2 = (MaterialModelV2) mat;
                String matName = mat.getName() != null ? mat.getName() : "";
                boolean isOutline = matName.toLowerCase().contains("outline");

                // Bỏ hoàn toàn outline mesh — engine chưa hỗ trợ render outline đúng cách
                if (isOutline) return null;

                TextureModel texModel = matV2.getEmissiveTexture();
                if (texModel == null) texModel = matV2.getBaseColorTexture();
                if (texModel == null) texModel = matV2.getMetallicRoughnessTexture();
                if (texModel != null && texModel.getImageModel() != null) {
                    textureId = getOrCreateTexture(texModel.getImageModel());
                }
            }

            return new Mesh(interleavedData, textureId, baseColorFactor);

        } catch (Exception e) {
            System.err.println("[Yanase Engine] LỖI khi build mesh: " + e.getMessage());
            return null;
        }
    }

    private static int getOrCreateTexture(ImageModel imgModel) {
        if (textureCache.containsKey(imgModel)) return textureCache.get(imgModel);

        ByteBuffer rawData = imgModel.getImageData();
        if (rawData == null) return -1;

        ByteBuffer safeBuffer = BufferUtils.createByteBuffer(rawData.remaining());
        int oldPos = rawData.position();
        safeBuffer.put(rawData);
        rawData.position(oldPos);
        safeBuffer.flip();

        int id = loadTextureFromBuffer(safeBuffer);
        if (id != -1) textureCache.put(imgModel, id);
        return id;
    }

    private static int loadTextureFromBuffer(ByteBuffer buffer) {
        STBImage.stbi_set_flip_vertically_on_load(false);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w    = stack.mallocInt(1);
            IntBuffer h    = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            ByteBuffer image = STBImage.stbi_load_from_memory(buffer, w, h, comp, 4);
            if (image == null) return -1;

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

            STBImage.stbi_image_free(image);
            return id;
        }
    }
}