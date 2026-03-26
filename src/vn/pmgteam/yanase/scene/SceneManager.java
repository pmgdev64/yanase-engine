package vn.pmgteam.yanase.scene;

public class SceneManager {
    private BaseScene currentScene;

    public void setScene(BaseScene scene) {
        if (currentScene != null) currentScene.cleanup();
        this.currentScene = scene;
        this.currentScene.init();
    }

    public BaseScene getCurrentScene() {
        return currentScene;
    }

    public void update(long window, float deltaTime) {
        if (currentScene != null) currentScene.update(window, deltaTime);
    }
}