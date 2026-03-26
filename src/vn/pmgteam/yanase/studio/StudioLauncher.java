package vn.pmgteam.yanase.studio;

import vn.pmgteam.yanase.studio.project.ProjectHub;

public class StudioLauncher {
    public static void main(String[] args) {
        // 1. Splash
        SplashScreen splash = new SplashScreen();
        splash.run();

        // 2. ProjectHub
        ProjectHub hub = new ProjectHub();
        String selectedProject = hub.run(); // trả về path .ygp hoặc null nếu thoát

        if (selectedProject == null) return; // User đóng hub

        // 3. Studio
        MainStudio studio = new MainStudio(selectedProject);
        studio.run();
    }
}