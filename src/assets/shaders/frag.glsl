#version 330 core
in vec3 ourColor;
out vec4 FragColor;

void main() {
    // Thử debug: FragColor = vec4(1.0, 0.0, 0.0, 1.0);
    // Nếu ra màu đỏ -> Ma trận đúng, VAO sai.
    // Nếu ra trắng -> Shader chưa nạp được biến ourColor.
    FragColor = vec4(ourColor, 1.0);
}
