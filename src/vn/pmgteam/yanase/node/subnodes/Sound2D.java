package vn.pmgteam.yanase.node.subnodes;

import javax.sound.sampled.*;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.node.Object2D;
import vn.pmgteam.yanase.render.RenderSystem;

public class Sound2D extends Object2D {
    private TargetDataLine microphone;
    private byte[] buffer;
    private float lastAmplitude = 0;
    private boolean isListening = false;

    public Sound2D(String name) {
        super(name);
        initMicrophone();
    }

    private void initMicrophone() {
        try {
            // Thiết lập định dạng âm thanh (Cực nhẹ: Mono, 16bit, 44100Hz)
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("[Sound2D] Card rời không hỗ trợ định dạng này!");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            
            // Buffer nhỏ để giảm lag và tiết kiệm RAM
            buffer = new byte[1024]; 
            isListening = true;
            System.out.println("[Sound2D] Đang nhận tín hiệu từ STM32 qua Card rời...");
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render2D() {
        if (!isListening) return;

        // Đọc dữ liệu thô từ ngõ Mic (Card rời)
        int numBytesRead = microphone.read(buffer, 0, buffer.length);
        
        if (numBytesRead > 0) {
            // Tính toán biên độ (Amplitude) đơn giản để làm Visualizer
            calculateAmplitude();
        }

        // Test render: Vẽ một thanh bar nhảy theo giọng nói từ STM32
        renderVisualizer();
    }

    private void calculateAmplitude() {
        int max = 0;
        for (int i = 0; i < buffer.length; i += 2) {
            // Gộp 2 byte thành 1 sample 16-bit
            short value = (short) ((buffer[i] << 8) | (buffer[i + 1] & 0xFF));
            max = Math.max(max, Math.abs(value));
        }
        lastAmplitude = max / 32768.0f; // Chuẩn hóa về khoảng 0.0 - 1.0
    }

    private void renderVisualizer() {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        
        // Vẽ một hình vuông đại diện cho cường độ âm thanh
        float size = 50 + (lastAmplitude * 100); 
        RenderSystem.color4f(0.2f, 0.8f, 1.0f, 0.8f);
        
        // Giả sử bạn có hàm vẽ Rect đơn giản
        // drawRect(position.x, position.y, size, size);
    }

    public float getAmplitude() {
        return lastAmplitude;
    }

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Node cloneNode(boolean deep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void normalize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSupported(String feature, String version) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNamespaceURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrefix() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getLocalName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttributes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getBaseURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getTextContent() throws DOMException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isSameNode(Node other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupPrefix(String namespaceURI) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String lookupNamespaceURI(String prefix) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEqualNode(Node arg) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getFeature(String feature, String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getUserData(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}