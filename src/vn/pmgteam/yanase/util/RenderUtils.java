package vn.pmgteam.yanase.util;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

public class RenderUtils {
    // Tái sử dụng buffer này cho mọi lần gửi Matrix, không tạo rác
    public static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
}