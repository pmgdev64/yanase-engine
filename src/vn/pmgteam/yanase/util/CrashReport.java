package vn.pmgteam.yanase.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class CrashReport {
	
	public static final String ERR_NULL_POINTER        = "EXCEPTION_NULL_POINTER_DEREFERENCE";
	public static final String ERR_RESOURCE_NOT_FOUND  = "EXCEPTION_ASSET_NOT_FOUND (Texture/Mesh)";
	public static final String ERR_EMERGENCY_EXIT      = "SYSTEM_EMERGENCY_EXIT (RAM 97% Triggered)";
	public static final String ERR_AUTOSAVE_FAILED     = "EXCEPTION_MODULE_AUTOSAVE_FAILURE";
	public static final String ERR_OPENGL_FATAL        = "EXCEPTION_OPENGL_RENDER_COLLAPSE";
    
    public static final String STATUS_EMERGENCY        = "EMERGENCY_SHUTDOWN_TRIGGERED";

    public static void make(String errorCode, double ramPercent) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "hs_err_yanase_pid_" + timeStamp + ".log";
        
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)))) {
            out.println("#");
            out.println("# A fatal error has been detected by the Yanase Engine Runtime Environment.");
            out.println("#");
            out.println("#  Ma loi: " + errorCode);
            out.println("#  Trang thai RAM: " + String.format("%.2f%%", ramPercent));
            out.println("#");
            out.println("# JRE version: " + System.getProperty("java.version"));
            out.println("# Engine: Yanase Engine (PmgTeam Project)");
            out.println("#");

            out.println("\n---------------  T H R E A D  ---------------");
            out.println("Current thread: " + Thread.currentThread().getName());
            out.println("Status: " + STATUS_EMERGENCY);

            out.println("\n---------------  S Y S T E M  ---------------");
            out.println("OS: " + System.getProperty("os.name") + " build " + System.getProperty("os.version"));
            out.println("Architecture: " + System.getProperty("os.arch"));
            
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            
            long total = osBean.getTotalPhysicalMemorySize() / 1024 / 1024;
            long free = osBean.getFreePhysicalMemorySize() / 1024 / 1024;
            
            out.println("Physical Memory (Total): " + total + " MB");
            out.println("Physical Memory (Free) : " + free + " MB");
            out.println("Physical Memory (Used) : " + (total - free) + " MB");

            out.println("\n---------------  G P U  ---------------");
            try {
                // KIEM TRA CONTEXT TRUOC KHI GOI
                // Neu khong co context, lenh nay se tra ve false thay vi crash JVM
                if (isGLContextAvailable()) {
                    out.println("Renderer: " + GL11.glGetString(GL11.GL_RENDERER));
                    out.println("Vendor  : " + GL11.glGetString(GL11.GL_VENDOR));
                } else {
                    out.println("GPU Status: Context Unavailable (No active OpenGL window)");
                }
            } catch (Throwable t) { 
                // Su dung Throwable de bat ca nhung loi nghiem trong tu Native
                out.println("GPU Status: Error retrieving GPU info (" + t.getMessage() + ")");
            }

            out.println("\n---------------  P M G T E A M  ---------------");
            out.println("Lead: Koyaru");
            out.println("Note: 4GB RAM Hardware Limitation Protocol.");
            out.println("Mana: 1.8M (Waiting for RAM Price Normalization)");
            out.println("Action: Module Toggled -> AutoSave Check -> Graceful Exit.");

            out.println("\n# [End of Crash Report]");
            out.flush();
            
            System.err.println("\n[Yanase] Fatal error log saved to: " + fileName);
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("notepad.exe", fileName).start();
            }
            
        } catch (IOException e) {
            System.err.println("[Yanase] Could not write crash report: " + e.getMessage());
        }
    }

    /**
     * Kiem tra xem OpenGL Context co san sang de truy van khong
     */
    private static boolean isGLContextAvailable() {
        try {
            return GL.getCapabilities() != null;
        } catch (Exception | Error e) {
            return false;
        }
    }
}