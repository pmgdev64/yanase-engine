package vn.pmgteam.yanase.memory;

/**
 * Được ném ra khi hệ thống chạm ngưỡng 4GB RAM Hardware Limitation.
 * Hỗ trợ lưu trữ trạng thái RAM tại thời điểm xảy ra lỗi.
 */
public class WeakMemoryError extends RuntimeException {
    private final long totalMemory;
    private final long usedMemory;

    public WeakMemoryError(String message, long used, long total) {
        super(String.format("%s [RAM Usage: %.2f%% - %dMB/%dMB]", 
              message, ((double)used/total)*100, used / 1024 / 1024, total / 1024 / 1024));
        this.usedMemory = used;
        this.totalMemory = total;
    }

    // Các getter để phục vụ cho việc ghi log/AutoSave trước khi thoát
    public long getUsedMemory() { return usedMemory; }
    public long getTotalMemory() { return totalMemory; }
}