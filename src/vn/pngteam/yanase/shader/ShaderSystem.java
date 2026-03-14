package vn.pngteam.yanase.shader;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public class ShaderSystem {
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    public ShaderSystem(String vertexFile, String fragmentFile) {
        // 1. Load source code từ file .glsl
        String vertexCode = loadSource(vertexFile);
        String fragmentCode = loadSource(fragmentFile);

        // 2. Tạo và compile Vertex Shader
        vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderId, vertexCode);
        glCompileShader(vertexShaderId);
        checkCompileErrors(vertexShaderId, "VERTEX");

        // 3. Tạo và compile Fragment Shader
        fragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderId, fragmentCode);
        glCompileShader(fragmentShaderId);
        checkCompileErrors(fragmentShaderId, "FRAGMENT");

        // 4. Link các Shader thành Program hoàn chỉnh
        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        glValidateProgram(programId);
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform(String name, FloatBuffer buffer) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            // false: vì JOML và OpenGL cùng dùng Column-major
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    private String loadSource(String path) {
        StringBuilder source = new StringBuilder();
        // Dùng ClassLoader để đọc resource từ bên trong JAR
        try (InputStream in = getClass().getResourceAsStream(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            
            if (in == null) {
                throw new RuntimeException("Không tìm thấy shader tại: " + path);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Lỗi đọc shader: " + e.getMessage());
            e.printStackTrace();
        }
        return source.toString();
    }

    private void checkCompileErrors(int shader, String type) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Yanase Shader Error [" + type + "]: " + glGetShaderInfoLog(shader, 512));
        }
    }
}