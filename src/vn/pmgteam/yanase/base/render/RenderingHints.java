package vn.pmgteam.yanase.base.render;

public interface RenderingHints {
    // --- Anti-Aliasing ---
    int AA_NONE = 0;
    int AA_MSAA_4X = 4;
    int AA_MSAA_8X = 8;
    int AA_MSAA_16X = 16;

    // --- Texture Filtering (Quan trọng khi dùng jgltf) ---
    int FILTER_NEAREST = 0; // Kiểu Pixel art
    int FILTER_BILINEAR = 1;
    int FILTER_TRILINEAR = 2;
    int FILTER_ANISOTROPIC_16X = 16;

    // --- Shadow Quality ---
    int SHADOW_LOW = 512;
    int SHADOW_MEDIUM = 1024;
    int SHADOW_HIGH = 2048;
    int SHADOW_ULTRA = 4096;
}