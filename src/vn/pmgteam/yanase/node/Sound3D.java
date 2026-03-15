package vn.pmgteam.yanase.node;

import org.joml.Vector3f;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

import vn.pmgteam.yanase.base.Engine;
import javax.sound.sampled.*;

public class Sound3D extends Object3D {
    private FloatControl balanceControl;
    private FloatControl gainControl;
    private SourceDataLine speaker;
    private float maxDistance = 20.0f; // Khoảng cách tối đa nghe thấy âm thanh

    public Sound3D(String name) {
        super(name);
        initAudioOutput();
    }

    private void initAudioOutput() {
        try {
            // Định dạng đầu ra (Cực nhẹ cho máy 4GB)
            AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            speaker = (SourceDataLine) AudioSystem.getLine(info);
            speaker.open(format);
            speaker.start();

            // Lấy bộ điều khiển âm lượng và cân bằng loa
            if (speaker.isControlSupported(FloatControl.Type.BALANCE)) {
                balanceControl = (FloatControl) speaker.getControl(FloatControl.Type.BALANCE);
            }
            gainControl = (FloatControl) speaker.getControl(FloatControl.Type.MASTER_GAIN);
        } catch (Exception e) {
            System.err.println("[Sound3D] Không thể khởi tạo output!");
        }
    }

    @Override
    public void render() {
        var camera = Engine.getEngine().getMainCamera();
        if (camera == null || speaker == null) return;

        // 1. Tính toán khoảng cách từ Camera tới Node âm thanh
        float distance = camera.getPosition().distance(this.position);

        // 2. Tính toán Độ lớn (Gain) - Càng xa càng nhỏ
        float volume = 1.0f - Math.min(distance / maxDistance, 1.0f);
        float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20);
        gainControl.setValue(Math.max(dB, gainControl.getMinimum()));

        // 3. Tính toán Hướng âm (Panning)
        // Dựa vào tích vô hướng giữa hướng Camera và vector tới nguồn âm
        Vector3f toSource = new Vector3f(this.position).sub(camera.getPosition()).normalize();
        Vector3f camRight = camera.getRightVector(); // Bạn cần viết hàm này trong Camera
        float pan = toSource.dot(camRight); 
        
        if (balanceControl != null) {
            balanceControl.setValue(Math.max(-1.0f, Math.min(1.0f, pan)));
        }

        // Vẽ một icon loa nhỏ trong Editor để dễ quản lý
        renderDebugIcon();
    }

    private void renderDebugIcon() {
        // Vẽ một khối Cube nhỏ hoặc ký hiệu Loa tại vị trí this.position
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