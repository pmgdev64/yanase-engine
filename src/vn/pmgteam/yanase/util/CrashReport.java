package vn.pmgteam.yanase.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

// OSHI Imports
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.concurrent.CountDownLatch;

/**
 * Yanase Engine - Global Crash Handler
 * Project Start: 2025-08-15
 * Last Build: 2026-03-20 (Version 0.0.3-alpha)
 * Developer: pmgdev64 (Koyaru) | PmgTeam
 */
public class CrashReport {

    // --- Engine Metadata ---
    private static final String ENGINE_NAME = "Yanase Engine (Project-Y)";
    private static final String ENGINE_VERSION = "0.0.3-alpha-build2026";
    private static final String RELEASE_DATE = "March 20, 2026 (Initial Prototype)";
    
    // --- Error Constants ---
    public static final String ERR_NULL_POINTER = "java.lang.NullPointerException";
    public static final String ERR_EMERGENCY_EXIT = "SYSTEM_HALT_BY_RAM_LIMIT (97%)";
    public static final String ERR_OPENGL_FATAL = "OPENGL_RENDER_THREAD_FAILURE";

    private static final String MC_FONT_PATH = "/assets/fonts/Minecraftia.ttf";
    private static Font mcFont;

    /**
     * Hàm tự động nhận diện mã lỗi từ Throwable
     */
    public static String getErrorCode(Throwable t) {
        if (t instanceof NullPointerException) return "NULL_POINTER_EXCEPTION";
        if (t instanceof OutOfMemoryError) return "MEMORY_LIMIT_EXCEEDED (OOM)";
        if (t instanceof StackOverflowError) return "STACK_OVERFLOW_FATAL";
        if (t instanceof ArrayIndexOutOfBoundsException) return "ARRAY_INDEX_OUT_OF_BOUNDS";
        if (t.getMessage() != null && t.getMessage().contains("GLFW")) return "GLFW_CONTEXT_FAILURE";
        return t.getClass().getSimpleName().toUpperCase();
    }

    public static void make(String errorCode, double ramPercent, Throwable t) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        
        String finalCode = (errorCode == null || errorCode.isEmpty()) ? getErrorCode(t) : errorCode;
        String logContent = generateDetailedLog(finalCode, ramPercent, t, hal);

        // 1. Console & File Log
        System.err.println("\n[FATAL ERROR] " + ENGINE_NAME + " has encountered a problem.");
        System.err.println(logContent);
        saveLogToFile(logContent, timeStamp);

        // 2. Tạo một cái chốt (Latch) với giá trị 1
        CountDownLatch latch = new CountDownLatch(1);

        // 3. Hiển thị GUI
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(() -> {
                    loadFonts();
                    showModernCrashScreen(finalCode, logContent, latch);
                });

                // QUAN TRỌNG: Đóng băng luồng Main của Engine để chờ người dùng xem lỗi
                latch.await(); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateDetailedLog(String err, double ram, Throwable t, HardwareAbstractionLayer hal) {
        StringBuilder sb = new StringBuilder();
        String currentTime = new SimpleDateFormat("M/d/yy h:mm a").format(new Date());
        CentralProcessor cpu = hal.getProcessor();
        String cpuName = cpu.getProcessorIdentifier().getName();

        List<oshi.hardware.GraphicsCard> gpus = hal.getGraphicsCards();
        String gpuName = (!gpus.isEmpty()) ? gpus.get(0).getName() : getGPUInfo();

        sb.append("---- ").append(ENGINE_NAME).append(" Crash Report ----\n");
        sb.append("// Hardware: ").append(cpuName).append(" | ").append(gpuName).append("\n");
        sb.append("// ").append(getMcJoke(hal)).append("\n\n");

        sb.append("-- Engine Identity --\n");
        sb.append("  Engine Name      : ").append(ENGINE_NAME).append("\n");
        sb.append("  Current Version  : ").append(ENGINE_VERSION).append("\n");
        sb.append("  Lead Developer   : pmgdev64 (Koyaru)\n\n");

        sb.append("-- Crash Analysis --\n");
        sb.append("  Time of Crash    : ").append(currentTime).append("\n");
        sb.append("  Fault Description: ").append(err).append("\n");
        sb.append("  RAM Usage Trigger: ").append(String.format("%.2f%%", ram)).append("\n\n");

        if (t != null) {
            sb.append("-- Stacktrace --\n");
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append(sw.toString());
        }

        sb.append("\n-- System Details (OSHI Detected) --\n");
        sb.append("  OS               : ").append(System.getProperty("os.name")).append("\n");
        sb.append("  CPU Processor    : ").append(cpuName).append("\n");
        sb.append("  Memory Hardware  : ").append(hal.getMemory().getTotal() / (1024 * 1024)).append("MB Total\n\n");

        sb.append("#@!@# Game crashed! Panic log exported to project root #@!@#");
        return sb.toString();
    }
    
    // Đã thêm static để tránh lỗi "No enclosing instance"
    private static class ModernButton extends JButton {
        public ModernButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createLineBorder(new Color(255, 60, 60), 1));
            setForeground(Color.WHITE);
            setFont(new Font("Consolas", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isPressed()) g2.setColor(new Color(150, 0, 0));
            else if (getModel().isRollover()) g2.setColor(new Color(200, 30, 30));
            else g2.setColor(new Color(40, 40, 40));
            
            g2.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
            g2.dispose();
        }
    }
    
    private static void showModernCrashScreen(String err, String fullLog, java.util.concurrent.CountDownLatch latch) {
        JFrame frame = new JFrame("Yanase Engine - Critical Panic");
        frame.setUndecorated(true); 
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        // Thay thế frame.setAlwaysOnTop(true); bằng đoạn này:
        frame.setAlwaysOnTop(true); // Vẫn để mặc định là nổi lên

        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                frame.setAlwaysOnTop(true); // Khi nhấn vào thì lại nổi lên
                frame.setOpacity(1.0f);     // Hiện rõ 100%
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                frame.setAlwaysOnTop(false); // Khi nhấn ra ngoài thì cho phép ẩn xuống dưới
                frame.setOpacity(0.8f);      // Làm mờ nhẹ 80% để bạn vẫn nhìn thấy code phía sau
            }
        });

        // Biến lưu tọa độ phục vụ kéo thả cửa sổ
        final Point dragOffset = new Point();

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, new Color(25, 25, 25), 0, getHeight(), new Color(10, 10, 10)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 60, 60, 150));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // --- Header Area (Thêm logic kéo thả) ---
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setCursor(new Cursor(Cursor.MOVE_CURSOR));

        header.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset.x = e.getX();
                dragOffset.y = e.getY();
            }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                frame.setLocation(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y);
            }
        });

        JLabel title = new JLabel("KERNEL PANIC // " + err);
        title.setFont(mcFont != null ? mcFont.deriveFont(28f) : new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(255, 60, 60));
        
        JLabel subTitle = new JLabel("Engine Version: " + ENGINE_VERSION + " | Status: TERMINATED_BY_FATAL_ERROR");
        subTitle.setFont(new Font("Consolas", Font.PLAIN, 12));
        subTitle.setForeground(new Color(100, 100, 100));

        header.add(title, BorderLayout.NORTH);
        header.add(subTitle, BorderLayout.SOUTH);

        // --- Log Area ---
        JTextArea consoleArea = new JTextArea(fullLog);
        consoleArea.setBackground(new Color(15, 15, 15));
        consoleArea.setForeground(new Color(180, 180, 180));
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        consoleArea.setEditable(false);
        consoleArea.setCaretColor(new Color(255, 60, 60));

        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 40, 40)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                this.thumbColor = new Color(60, 60, 60);
                this.trackColor = new Color(20, 20, 20);
            }
        });

        // --- Bottom Action Area ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 20));
        bottomPanel.setOpaque(false);

        // Cố định kích thước cho cả 2 nút
        Dimension buttonSize = new Dimension(220, 45);

        ModernButton copyBtn = new ModernButton(" COPY STACKTRACE ");
        copyBtn.setPreferredSize(buttonSize);
        copyBtn.addActionListener(e -> {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(fullLog), null);
            copyBtn.setText(" COPIED! ");
        });

        ModernButton exitBtn = new ModernButton(" TERMINATE PROCESS ");
        exitBtn.setPreferredSize(buttonSize);
        exitBtn.addActionListener(e -> frame.dispose());

        bottomPanel.add(copyBtn);
        bottomPanel.add(exitBtn);

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(scroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { latch.countDown(); }
        });
        
        frame.setVisible(true);
    }
    
    private static void loadFonts() {
        if (mcFont != null) return;
        try (InputStream is = CrashReport.class.getResourceAsStream(MC_FONT_PATH)) {
            if (is != null) {
                mcFont = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(mcFont);
            }
        } catch (Exception e) {}
    }

    private static void saveLogToFile(String content, String time) {
        File dir = new File("crash-reports");
        if (!dir.exists()) dir.mkdir();
        try (FileWriter fw = new FileWriter(new File(dir, "crash-" + time + ".txt"))) {
            fw.write(content);
        } catch (IOException ignored) {}
    }

    private static String getMcJoke(HardwareAbstractionLayer hal) {
        String cpuName = hal.getProcessor().getProcessorIdentifier().getName();
        long totalMemoryBytes = hal.getMemory().getTotal();
        double totalGB = totalMemoryBytes / (1024.0 * 1024.0 * 1024.0);
        
        // Clean up CPU name for a cleaner roast
        String cpuShort = cpuName.split("@")[0].replace("CPU", "").trim();

        java.util.List<String> jokes = new java.util.ArrayList<>();

        // --- Dynamic Hardware Roast (English Only) ---
        if (totalGB < 5) {
            jokes.add(String.format("%.1fGB RAM? Even Chrome is laughing at you.", totalGB));
            jokes.add("Managed Heap Protocol: 'I tried my best with this tiny RAM, Koyaru.'");
            jokes.add("Your RAM went to buy milk and never came back.");
            jokes.add("Is this a PC or a toaster? Because the RAM just burnt out.");
        } else if (totalGB > 15) {
            jokes.add(String.format("%.0fGB RAM and you still hit a crash? That's impressive.", totalGB));
            jokes.add("Imagine having this much RAM and still failing a Null check.");
        }

        if (cpuName.contains("Pentium") || cpuName.contains("Celeron") || cpuName.contains("Gold G5400")) {
            jokes.add(cpuShort + " is crying in the corner...");
            jokes.add("This CPU is better at grilling steaks than running engines.");
            jokes.add(cpuShort + ": 'Please boss, have mercy on my 2 cores!'");
            jokes.add("Thermal Throttling: 'Allow me to introduce myself.'");
        } else if (cpuName.contains("Core i9") || cpuName.contains("Ryzen 9")) {
            jokes.add("Top-tier CPU, bottom-tier logic. What a waste of silicon.");
        }

        // --- VTuber & Global Dev Culture ---
        jokes.add("Gawr Gura: 'A... looks like the engine just drowned.'");
        jokes.add("Nanashi Mumei: 'Oh! I think I forgot to initialize this... Hehe!'");
        jokes.add("Murasaki Shion: 'My magic can't fix this NPE, you know.'");
        jokes.add("Hoshino Honekawa: 'Is the darkness getting to the code too?'");

        // --- Engine & Project Specific ---
        jokes.add("why my game was crashed????!");
        jokes.add("It's not a bug, it's an undocumented feature.");
        jokes.add("Stacktrace is longer than my future.");
        jokes.add("Kodoku No Yami: The real horror is this log file.");
        jokes.add("Lotus2D: Zero-RAM doesn't mean Zero-Brain, dev.");
        jokes.add("AutoSave: 'I saved the wreckage for you. You're welcome.'");
        jokes.add("NullPointerException: A developer's best friend.");
        jokes.add("I swear, it worked on my machine five minutes ago.");

        // --- General Tech Humor ---
        jokes.add("Have you tried turning it off and on again?");
        jokes.add("System.exit(0) would have been more graceful than this.");
        jokes.add("Keyboard not found. Press F1 to continue.");
        jokes.add("Error 404: Brain cells not found while writing this line.");
        jokes.add("To be or not to be... that is the NPE.");

        return jokes.get((int)(Math.random() * jokes.size()));
    }

    private static String getGPUInfo() {
        try { if (GL.getCapabilities() != null) return GL11.glGetString(GL11.GL_RENDERER); } catch (Exception e) {}
        return "Context Disconnected";
    }
}