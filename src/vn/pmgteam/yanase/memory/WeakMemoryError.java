package vn.pmgteam.yanase.memory;

/**
 * Được ném ra khi hệ thống chạm ngưỡng RAM Hardware Limitation.
 * Extends Error vì đây là tình huống không thể recover.
 */
public class WeakMemoryError extends Error {
    private final long totalMemory;
    private final long usedMemory;
    private final long freeMemory;
    private final long timestamp;

    public WeakMemoryError(String message, long used, long total) {
        super(String.format("%s [RAM Usage: %.2f%% - %dMB/%dMB]",
              message, ((double) used / total) * 100,
              used / 1024 / 1024, total / 1024 / 1024));
        this.usedMemory  = used;
        this.totalMemory = total;
        this.freeMemory  = total - used;
        this.timestamp   = System.currentTimeMillis();
    }

    public long getUsedMemory()  { return usedMemory; }
    public long getTotalMemory() { return totalMemory; }
    public long getFreeMemory()  { return freeMemory; }
    public long getTimestamp()   { return timestamp; }

    /** Tiện cho logging */
    public double getUsagePercent() {
        return ((double) usedMemory / totalMemory) * 100.0;
    }
}