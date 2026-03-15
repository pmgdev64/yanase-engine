package vn.pmgteam.yanase.render;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL40.*;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

import vn.pmgteam.yanase.base.Engine;

public class RenderSystem {

    // --- LIGHTING DATA ---
    private static final FloatBuffer SUN_POS = BufferUtils.createFloatBuffer(4).put(new float[]{0.2f, 1.0f, -0.7f, 0.0f}).flip();
    private static final FloatBuffer SUN_DIFFUSE = BufferUtils.createFloatBuffer(4).put(new float[]{1.0f, 1.0f, 0.9f, 1.0f}).flip();
    private static final FloatBuffer AO_BUFFER = BufferUtils.createFloatBuffer(4);
    private static final FloatBuffer FOG_BUFFER = BufferUtils.createFloatBuffer(4);
    // --- TIME SYSTEM (Giống _Time trong Unity Shader) ---
    private static float engineTime = 0;

    // --- DEPTH SYSTEM ---
    public static void enableDepthTest() {
        glEnable(GL_DEPTH_TEST);
    }

    public static void disableDepthTest() {
        glDisable(GL_DEPTH_TEST);
    }

    public static void depthFunc(int func) {
        glDepthFunc(func); // Thường là GL_LEQUAL
    }

    // --- BLEND SYSTEM (Quan trọng cho Label2D) ---
    public static void enableBlend() {
        glEnable(GL_BLEND);
    }

    public static void disableBlend() {
        glDisable(GL_BLEND);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        glBlendFunc(srcFactor, dstFactor);
    }

    public static void defaultBlendFunc() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    // --- CULL FACE (Tối ưu render cho UHD 610) ---
    public static void enableCull() {
        glEnable(GL_CULL_FACE);
    }

    public static void disableCull() {
        glDisable(GL_CULL_FACE);
    }

    // --- LIGHTING SYSTEM ---
    public static void enableLighting() {
        glEnable(GL_LIGHTING);
    }

    public static void disableLighting() {
        glDisable(GL_LIGHTING);
    }

    public static void setupSunLighting() {
        enableLighting();
        glEnable(GL_LIGHT0);
        glLightfv(GL_LIGHT0, GL_POSITION, SUN_POS);
        glLightfv(GL_LIGHT0, GL_DIFFUSE, SUN_DIFFUSE);
        
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
    }

    // --- COLOR & TEXTURE ---
    public static void color4f(float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
    }

    public static void bindTexture(int textureID) {
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public static void enableTexture() {
        glEnable(GL_TEXTURE_2D);
    }

    public static void disableTexture() {
        glDisable(GL_TEXTURE_2D);
    }
    
 // --- FOG SYSTEM (Che khuyết điểm render xa, tiết kiệm RAM/GPU) ---
    public static void setupFog(float start, float end, float r, float g, float b) {
        glEnable(GL_FOG);
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, start);
        glFogf(GL_FOG_END, end);
        
        FloatBuffer fogColor = BufferUtils.createFloatBuffer(4).put(new float[]{r, g, b, 1.0f}).flip();
        glFogfv(GL_FOG_COLOR, fogColor);
    }

    // --- MATERIAL SYSTEM (Giúp nhân vật MMD/GLB có độ bóng) ---
    public static void setMaterial(float shininess, float specularR, float specularG, float specularB) {
        FloatBuffer spec = BufferUtils.createFloatBuffer(4).put(new float[]{specularR, specularG, specularB, 1.0f}).flip();
        glMaterialfv(GL_FRONT, GL_SPECULAR, spec);
        glMaterialf(GL_FRONT, GL_SHININESS, shininess);
    }

    // --- VIEWPORT & PROJECTION (Chuyển đổi 2D/3D linh hoạt) ---
    public static void setMode2D(int width, int height) {
        disableDepthTest();
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
    }

    public static void setMode3D() {
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        enableDepthTest();
    }

    // --- SMOOTHING (Khử răng cưa cơ bản cho Texture) ---
    public static void enableSmooth() {
        glEnable(GL_LINE_SMOOTH);
        glEnable(GL_POINT_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
    }

    // --- CLEAR SYSTEM ---
    public static void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static void setClearColor(float r, float g, float b, float a) {
        glClearColor(r, g, b, a);
    }
    
    public static void updateEngineTime(float deltaTime) {
        engineTime += deltaTime;
    }

    // --- SCREEN FADE (Hiệu ứng chuyển cảnh - Unity Camera Fade) ---
    public static void renderScreenOverlay(float r, float g, float b, float alpha) {
        setMode2D(Engine.getEngine().getWindowWidth(), Engine.getEngine().getWindowHeight());
        disableTexture();
        enableBlend();
        defaultBlendFunc();
        color4f(r, g, b, alpha);
        
        glBegin(GL_QUADS);
            glVertex2f(0, 0);
            glVertex2f(Engine.getEngine().getWindowWidth(), 0);
            glVertex2f(Engine.getEngine().getWindowWidth(), Engine.getEngine().getWindowHeight());
            glVertex2f(0, Engine.getEngine().getWindowHeight());
        glEnd();
        
        setMode3D();
    }

    // --- BLOOM-LIKE AMBIENT (Giả lập Bloom bằng cách tăng độ sáng vùng sáng) ---
    public static void setBrightnessThreshold(float threshold) {
        // Trong Legacy, ta dùng glAlphaFunc để giả lập lọc vùng sáng
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, threshold); 
    }

    // --- UNIT-SCALE HELPER (Giống Unreal Units) ---
    // Chuyển đổi từ tọa độ Engine sang tọa độ Render chuẩn
    public static void setWorldScale(float scale) {
        glScalef(scale, scale, scale);
    }

    // --- POST-PROCESS: VIGNETTE (Tạo góc tối màn hình tăng chiều sâu) ---
    public static void drawVignette(int textureID) {
        enableBlend();
        glBlendFunc(GL_ZERO, GL_SRC_COLOR); // Multiply blend mode giống Photoshop/UE
        bindTexture(textureID);
        renderScreenOverlay(1, 1, 1, 1); // Vẽ một texture gradient hình tròn đen mờ
        defaultBlendFunc();
    }

    // --- WIREFRAME MODE (Giống Scene View của Unity) ---
    public static void setWireframe(boolean enable) {
        if (enable) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }
    
    public static void setupFakeAO(float intensity) {
        // FIX: Đổi từ AMBIENT_CONTROL sang COLOR_CONTROL
        // GL_SEPARATE_SPECULAR_COLOR giúp highlight bóng loáng không bị xỉn màu bởi ambient
        glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL, GL_SEPARATE_SPECULAR_COLOR);
        
        // Tái sử dụng Buffer thay vì tạo mới
        AO_BUFFER.clear();
        AO_BUFFER.put(new float[]{intensity, intensity, intensity, 1.0f}).flip();
        glLightModelfv(GL_LIGHT_MODEL_AMBIENT, AO_BUFFER);
        
        // Mẹo Unity: Bật Rescale Normal để ánh sáng trên Model MMD không bị lỗi khi Scale
        glEnable(GL_RESCALE_NORMAL);
    }

    // --- ENHANCED FOG (Giống sương mù trong Unreal Engine) ---
    public static void setupAdvancedFog(float start, float end, float r, float g, float b) {
        glEnable(GL_FOG);
        // GL_EXP2 tạo cảm giác sương mù dày đặc và thật hơn GL_LINEAR
        glFogi(GL_FOG_MODE, GL_EXP2); 
        glFogf(GL_FOG_DENSITY, 0.05f); // Độ dày sương mù
        
        FOG_BUFFER.clear();
        FOG_BUFFER.put(new float[]{r, g, b, 1.0f}).flip();
        glFogfv(GL_FOG_COLOR, FOG_BUFFER);
        
        // Giúp card Intel UHD 610 xử lý sương mù mịn hơn
        glHint(GL_FOG_HINT, GL_NICEST);
    }

    // --- CAMERA SHAKE (Hiệu ứng rung chấn - Unreal Camera Shake) ---
    private static float shakeMagnitude = 0;
    public static void setCameraShake(float intensity) {
        shakeMagnitude = intensity;
    }

    public static void applyCameraShake() {
        if (shakeMagnitude > 0) {
            float tx = (float) (Math.random() * 2 - 1) * shakeMagnitude;
            float ty = (float) (Math.random() * 2 - 1) * shakeMagnitude;
            glTranslatef(tx, ty, 0);
            shakeMagnitude *= 0.9f; // Giảm dần độ rung (Damping)
        }
    }

    // --- SKYBOX RENDERING (Tạo bầu trời chân thực) ---
    public static void drawSkybox(int cubemapTexture) {
        disableDepthTest();
        glDepthMask(false); // Không ghi vào Depth Buffer
        // Vẽ khối Cube bao quanh Camera tại đây
        glDepthMask(true);
        enableDepthTest();
    }

    // --- EXPOSURE CONTROL (Giống Auto-Exposure trong Unreal) ---
    public static void setExposure(float exposure) {
        // Điều chỉnh độ sáng tổng thể của Scene
        glPixelTransferf(GL_RED_SCALE, exposure);
        glPixelTransferf(GL_GREEN_SCALE, exposure);
        glPixelTransferf(GL_BLUE_SCALE, exposure);
    }
}