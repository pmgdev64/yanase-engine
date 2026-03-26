package vn.pmgteam.yanase.particle;

import org.lwjgl.opengl.GL11;

public class BaseParticle {
    public float x, y, z;
    public float vx, vy, vz;
    public float life;      // Thời gian sống (giây)
    public float maxLife;
    public boolean active = false;

    public void spawn(float x, float y, float z, float vx, float vy, float vz, float life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.life = life;
        this.maxLife = life;
        this.active = true;
    }

    public void update(float deltaTime) {
        if (!active) return;
        x += vx * deltaTime;
        y += vy * deltaTime;
        z += vz * deltaTime;
        life -= deltaTime;
        if (life <= 0) active = false;
    }
}