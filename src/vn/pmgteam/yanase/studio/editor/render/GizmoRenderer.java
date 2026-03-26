package vn.pmgteam.yanase.studio.editor.render;

import org.joml.*;
import org.joml.Math;

import vn.pmgteam.yanase.node.Object3D;
import vn.pmgteam.yanase.node.subnodes.CameraNode;

import static org.lwjgl.opengl.GL11.*;

public class GizmoRenderer {

    public enum GizmoMode { TRANSLATE, ROTATE, SCALE }
    private GizmoMode mode = GizmoMode.TRANSLATE;

    // Drag state
    private boolean isDragging = false;
    private int dragAxis = -1; // 0=X, 1=Y, 2=Z
    private float dragStartMouseX, dragStartMouseY;
    private Vector3f dragStartPos = new Vector3f();
    private Vector3f dragStartRot = new Vector3f();
    private Vector3f dragStartScale = new Vector3f();

    private static final float AXIS_LENGTH = 1.5f;
    private static final float ARROW_SIZE  = 0.15f;
    private static final float HIT_RADIUS  = 0.18f;

    public GizmoMode getMode() { return mode; }
    public void setMode(GizmoMode m) { this.mode = m; }
    public boolean isDragging() { return isDragging; }

    // ----------------------------------------------------------------
    // RENDER
    // ----------------------------------------------------------------
    public void render(Object3D node, CameraNode cam) {
        if (node == null || cam == null) return;

        glDisable(GL_DEPTH_TEST);
        glLineWidth(2.5f);

        Vector3f pos = node.position;

        glPushMatrix();
        glTranslatef(pos.x, pos.y, pos.z);

        switch (mode) {
            case TRANSLATE -> renderTranslateGizmo();
            case ROTATE    -> renderRotateGizmo();
            case SCALE     -> renderScaleGizmo();
        }

        glPopMatrix();

        glLineWidth(1.0f);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderTranslateGizmo() {
        // X — đỏ
        glBegin(GL_LINES);
        glColor3f(0.9f, 0.2f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(AXIS_LENGTH, 0, 0);
        glEnd();
        drawArrowHead(AXIS_LENGTH, 0, 0, 1, 0, 0, 0.9f, 0.2f, 0.2f);

        // Y — xanh lá
        glBegin(GL_LINES);
        glColor3f(0.2f, 0.9f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(0, AXIS_LENGTH, 0);
        glEnd();
        drawArrowHead(0, AXIS_LENGTH, 0, 0, 1, 0, 0.2f, 0.9f, 0.2f);

        // Z — xanh dương
        glBegin(GL_LINES);
        glColor3f(0.2f, 0.2f, 0.9f);
        glVertex3f(0, 0, 0); glVertex3f(0, 0, AXIS_LENGTH);
        glEnd();
        drawArrowHead(0, 0, AXIS_LENGTH, 0, 0, 1, 0.2f, 0.2f, 0.9f);

        // Center dot
        glPointSize(6);
        glBegin(GL_POINTS);
        glColor3f(1f, 1f, 1f);
        glVertex3f(0, 0, 0);
        glEnd();
        glPointSize(1);
    }

    private void renderScaleGizmo() {
        // X
        glBegin(GL_LINES);
        glColor3f(0.9f, 0.2f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(AXIS_LENGTH, 0, 0);
        glEnd();
        drawCube(AXIS_LENGTH, 0, 0, 0.9f, 0.2f, 0.2f);

        // Y
        glBegin(GL_LINES);
        glColor3f(0.2f, 0.9f, 0.2f);
        glVertex3f(0, 0, 0); glVertex3f(0, AXIS_LENGTH, 0);
        glEnd();
        drawCube(0, AXIS_LENGTH, 0, 0.2f, 0.9f, 0.2f);

        // Z
        glBegin(GL_LINES);
        glColor3f(0.2f, 0.2f, 0.9f);
        glVertex3f(0, 0, 0); glVertex3f(0, 0, AXIS_LENGTH);
        glEnd();
        drawCube(0, 0, AXIS_LENGTH, 0.2f, 0.2f, 0.9f);

        glPointSize(6);
        glBegin(GL_POINTS);
        glColor3f(1f, 1f, 1f);
        glVertex3f(0, 0, 0);
        glEnd();
        glPointSize(1);
    }

    private void renderRotateGizmo() {
        int segs = 48;

        // X — đỏ (vòng tròn YZ)
        glBegin(GL_LINE_LOOP);
        glColor3f(0.9f, 0.2f, 0.2f);
        for (int i = 0; i < segs; i++) {
            double a = 2 * Math.PI * i / segs;
            glVertex3f(0, (float)Math.cos(a) * AXIS_LENGTH, (float)Math.sin(a) * AXIS_LENGTH);
        }
        glEnd();

        // Y — xanh lá (vòng tròn XZ)
        glBegin(GL_LINE_LOOP);
        glColor3f(0.2f, 0.9f, 0.2f);
        for (int i = 0; i < segs; i++) {
            double a = 2 * Math.PI * i / segs;
            glVertex3f((float)Math.cos(a) * AXIS_LENGTH, 0, (float)Math.sin(a) * AXIS_LENGTH);
        }
        glEnd();

        // Z — xanh dương (vòng tròn XY)
        glBegin(GL_LINE_LOOP);
        glColor3f(0.2f, 0.2f, 0.9f);
        for (int i = 0; i < segs; i++) {
            double a = 2 * Math.PI * i / segs;
            glVertex3f((float)Math.cos(a) * AXIS_LENGTH, (float)Math.sin(a) * AXIS_LENGTH, 0);
        }
        glEnd();
    }

    // ----------------------------------------------------------------
    // HIT TEST — project 3D axis endpoint về screen để check click
    // ----------------------------------------------------------------
    public int hitTest(Object3D node, CameraNode cam,
                       float mouseX, float mouseY, float vpW, float vpH) {
        if (node == null || cam == null) return -1;

        Vector3f nodePos = node.position;

        // 3 điểm đầu trục
        Vector3f[] axisEnds = {
            new Vector3f(nodePos.x + AXIS_LENGTH, nodePos.y, nodePos.z),
            new Vector3f(nodePos.x, nodePos.y + AXIS_LENGTH, nodePos.z),
            new Vector3f(nodePos.x, nodePos.y, nodePos.z + AXIS_LENGTH)
        };

        float minDist = HIT_RADIUS * vpH / 8f;
        int hit = -1;

        for (int i = 0; i < 3; i++) {
            Vector2f screen = worldToScreen(axisEnds[i], cam, vpW, vpH);
            if (screen == null) continue;
            float dx = mouseX - screen.x;
            float dy = mouseY - screen.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < minDist) {
                minDist = dist;
                hit = i;
            }
        }

        // Cũng check center
        Vector2f center = worldToScreen(nodePos, cam, vpW, vpH);
        if (center != null) {
            float dx = mouseX - center.x;
            float dy = mouseY - center.y;
            if (Math.sqrt(dx*dx + dy*dy) < 12) hit = 3; // center = all axes
        }

        return hit;
    }

    private Vector2f worldToScreen(Vector3f world, CameraNode cam, float vpW, float vpH) {
        // MVP transform
        Matrix4f view = cam.viewMatrix;
        Matrix4f proj = cam.projectionMatrix;

        Vector4f clip = proj.transform(view.transform(
            new Vector4f(world.x, world.y, world.z, 1.0f)));

        if (clip.w <= 0) return null; // behind camera

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        return new Vector2f(
            (ndcX + 1f) / 2f * vpW,
            (1f - ndcY) / 2f * vpH
        );
    }

    // ----------------------------------------------------------------
    // DRAG
    // ----------------------------------------------------------------
    public void startDrag(int axis, float mouseX, float mouseY, Object3D node) {
        isDragging = true;
        dragAxis = axis;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartPos.set(node.position);
        dragStartRot.set(node.rotation);
        dragStartScale.set(node.scale);
    }

    public void updateDrag(float mouseX, float mouseY, Object3D node) {
        if (!isDragging || node == null) return;
        float dx = (mouseX - dragStartMouseX) * 0.015f;
        float dy = (dragStartMouseY - mouseY) * 0.015f; // Y đảo ngược
        float delta = Math.abs(dx) > Math.abs(dy) ? dx : dy;

        switch (mode) {
            case TRANSLATE -> {
                node.position.set(dragStartPos);
                if (dragAxis == 0) node.position.x += delta * 3;
                else if (dragAxis == 1) node.position.y += delta * 3;
                else if (dragAxis == 2) node.position.z += delta * 3;
                else { // center — move XZ plane
                    node.position.x += dx * 3;
                    node.position.z -= dy * 3;
                }
            }
            case ROTATE -> {
                node.rotation.set(dragStartRot);
                float deg = delta * 150f;
                if (dragAxis == 0) node.rotation.x += deg;
                else if (dragAxis == 1) node.rotation.y += deg;
                else if (dragAxis == 2) node.rotation.z += deg;
            }
            case SCALE -> {
                node.scale.set(dragStartScale);
                float s = 1f + delta * 2f;
                if (dragAxis == 0) node.scale.x = Math.max(0.01f, dragStartScale.x * s);
                else if (dragAxis == 1) node.scale.y = Math.max(0.01f, dragStartScale.y * s);
                else if (dragAxis == 2) node.scale.z = Math.max(0.01f, dragStartScale.z * s);
                else node.scale.set( // center = uniform scale
                    Math.max(0.01f, dragStartScale.x * s),
                    Math.max(0.01f, dragStartScale.y * s),
                    Math.max(0.01f, dragStartScale.z * s));
            }
        }
    }

    public void endDrag() {
        isDragging = false;
        dragAxis = -1;
    }

    // ----------------------------------------------------------------
    // PRIMITIVES
    // ----------------------------------------------------------------
    private void drawArrowHead(float x, float y, float z,
                                float dx, float dy, float dz,
                                float r, float g, float b) {
        glBegin(GL_TRIANGLES);
        glColor3f(r, g, b);
        float s = ARROW_SIZE;
        // Tạo tam giác nhỏ vuông góc với trục
        float px = dy * s, py = -dx * s, pz = dz * s;
        glVertex3f(x + dx * s, y + dy * s, z + dz * s);
        glVertex3f(x - px * 0.5f, y - py * 0.5f, z + pz * 0.5f);
        glVertex3f(x + px * 0.5f, y + py * 0.5f, z - pz * 0.5f);
        glEnd();
    }

    private void drawCube(float x, float y, float z, float r, float g, float b) {
        float s = ARROW_SIZE * 0.8f;
        glBegin(GL_QUADS);
        glColor3f(r, g, b);
        // 6 mặt cube nhỏ
        glVertex3f(x-s, y-s, z+s); glVertex3f(x+s, y-s, z+s);
        glVertex3f(x+s, y+s, z+s); glVertex3f(x-s, y+s, z+s);
        glVertex3f(x-s, y-s, z-s); glVertex3f(x-s, y+s, z-s);
        glVertex3f(x+s, y+s, z-s); glVertex3f(x+s, y-s, z-s);
        glVertex3f(x-s, y+s, z-s); glVertex3f(x-s, y+s, z+s);
        glVertex3f(x+s, y+s, z+s); glVertex3f(x+s, y+s, z-s);
        glVertex3f(x-s, y-s, z-s); glVertex3f(x+s, y-s, z-s);
        glVertex3f(x+s, y-s, z+s); glVertex3f(x-s, y-s, z+s);
        glVertex3f(x+s, y-s, z-s); glVertex3f(x+s, y+s, z-s);
        glVertex3f(x+s, y+s, z+s); glVertex3f(x+s, y-s, z+s);
        glVertex3f(x-s, y-s, z-s); glVertex3f(x-s, y-s, z+s);
        glVertex3f(x-s, y+s, z+s); glVertex3f(x-s, y+s, z-s);
        glEnd();
    }
}