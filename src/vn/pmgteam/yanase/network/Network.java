package vn.pmgteam.yanase.network;

import com.codedisaster.steamworks.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Network {
    // Chỉ cấp phát 1 lần duy nhất để tránh leak Direct Memory trên 4GB RAM
    protected static final ByteBuffer sharedBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.LITTLE_ENDIAN);
    protected boolean isClientActive = false;

    public Network() {
        // Khởi tạo Steam Networking Messages qua Steamworks4j
    }

    /**
     * Gửi dữ liệu an toàn. Steamworks sẽ copy data từ sharedBuffer 
     * sang bộ nhớ Native của nó, nên ta có thể reuse sharedBuffer ngay lập tức.
     */
    protected void sendData(SteamID target, int sendFlags) {
        sharedBuffer.flip();
        // Giả sử bạn sử dụng SteamNetworkingMessages
        // messages.sendMessageToUser(target, sharedBuffer, sendFlags, 0);
    }

    /**
     * Mỗi khi một Module thay đổi, hàm này sẽ được gọi để đồng bộ và lưu trữ.
     */
    public abstract void syncModuleState(String moduleName, boolean state);

    public abstract void update();
    
    public abstract void shutdown();
}