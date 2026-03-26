package vn.pmgteam.yanase.node.subnodes;

import vn.pmgteam.yanase.ai.NPCBrain;
import vn.pmgteam.yanase.ai.NPCBrain.Action;
import vn.pmgteam.yanase.node.Object3D;
import org.w3c.dom.*;

import java.util.concurrent.CompletableFuture;

/**
 * NPCNode — Object3D tích hợp NPCBrain.
 * Dùng trong scene như Box3D nhưng có AI.
 *
 * Ví dụ dùng trong GameScene:
 * <pre>
 *   NPCNode guard = new NPCNode("Guard", "hostile city guard", "sk_guard.png");
 *   guard.getBrain().setApiKey("your-key");
 *   guard.position.set(5, 0, 0);
 *   rootNode.appendChild(guard);
 *
 *   // Khi player nói chuyện:
 *   guard.talk("Hello guard!").thenAccept(reply -> {
 *       System.out.println("Guard: " + reply);
 *   });
 * </pre>
 */
public class NPCNode extends Box3D {

    private final NPCBrain brain;
    private float  talkRange      = 3.0f;   // Khoảng cách có thể nói chuyện
    private float  detectionRange = 8.0f;   // Khoảng cách phát hiện
    private boolean isThinking    = false;  // Đang chờ AI response
    private float   updateTimer   = 0f;
    private float   updateInterval = 3.0f;  // Giây giữa mỗi lần AI cập nhật hành động

    // Patrol path
    private org.joml.Vector3f[] patrolPoints;
    private int   patrolIndex   = 0;
    private float patrolSpeed   = 0.02f;

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public NPCNode(String name, String personality) {
        super(name);
        this.brain = new NPCBrain(name, personality);
        setupDefaultListeners();
    }

    public NPCNode(String name, String personality, int memory) {
        super(name);
        this.brain = new NPCBrain(name, personality, memory);
        setupDefaultListeners();
    }

    // ----------------------------------------------------------------
    // AI shortcuts
    // ----------------------------------------------------------------

    public CompletableFuture<String> talk(String playerInput) {
        return brain.talk(playerInput);
    }

    public CompletableFuture<Action> think(String context) {
        if (isThinking) return CompletableFuture.completedFuture(brain.getCurrentAction());
        isThinking = true;
        return brain.decideAction(context).whenComplete((a, e) -> isThinking = false);
    }

    // ----------------------------------------------------------------
    // Update — gọi trong GameScene.update()
    // ----------------------------------------------------------------

    public void update(float deltaTime, org.joml.Vector3f playerPos) {
        updateTimer += deltaTime;

        float distToPlayer = position.distance(playerPos);

        // Cập nhật AI mỗi updateInterval giây
        if (updateTimer >= updateInterval && !isThinking) {
            updateTimer = 0;
            String context = buildContext(distToPlayer, playerPos);
            think(context);
        }

        // Thực thi hành động hiện tại
        executeAction(deltaTime, playerPos, distToPlayer);
    }

    private void executeAction(float dt, org.joml.Vector3f playerPos, float dist) {
        switch (brain.getCurrentAction()) {
            case PATROL -> patrol(dt);
            case CHASE  -> moveToward(playerPos, dt, 0.04f);
            case FLEE   -> moveAway(playerPos, dt, 0.05f);
            case IDLE, TALK, SLEEP -> { /* Đứng yên */ }
            default -> { /* Giữ vị trí */ }
        }
    }

    // ----------------------------------------------------------------
    // Movement helpers
    // ----------------------------------------------------------------

    private void patrol(float dt) {
        if (patrolPoints == null || patrolPoints.length == 0) return;

        org.joml.Vector3f target = patrolPoints[patrolIndex];
        float dist = position.distance(target);

        if (dist < 0.2f) {
            patrolIndex = (patrolIndex + 1) % patrolPoints.length;
        } else {
            moveToward(target, dt, patrolSpeed);
        }
    }

    private void moveToward(org.joml.Vector3f target, float dt, float speed) {
        float dx = target.x - position.x;
        float dz = target.z - position.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01f) return;

        position.x += (dx / len) * speed * dt * 60;
        position.z += (dz / len) * speed * dt * 60;

        // Xoay NPC về hướng di chuyển
        rotation.y = (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    private void moveAway(org.joml.Vector3f from, float dt, float speed) {
        float dx = position.x - from.x;
        float dz = position.z - from.z;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01f) return;

        position.x += (dx / len) * speed * dt * 60;
        position.z += (dz / len) * speed * dt * 60;
    }

    // ----------------------------------------------------------------
    // Context builder
    // ----------------------------------------------------------------

    private String buildContext(float distToPlayer, org.joml.Vector3f playerPos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player is ").append(String.format("%.1f", distToPlayer)).append(" units away. ");

        if (distToPlayer < talkRange) {
            sb.append("Player is very close (in talk range). ");
        } else if (distToPlayer < detectionRange) {
            sb.append("Player is in detection range. ");
        } else {
            sb.append("No player nearby. ");
        }

        sb.append("My health: ").append((int)brain.getHealth()).append("/100. ");
        sb.append("Alert level: ").append(String.format("%.0f", brain.getAlertLevel() * 100)).append("%. ");
        sb.append("Current action: ").append(brain.getCurrentAction()).append(".");

        return sb.toString();
    }

    // ----------------------------------------------------------------
    // Default listeners
    // ----------------------------------------------------------------

    private void setupDefaultListeners() {
        brain.setActionListener((action, reason) -> {
            System.out.println("[" + getNodeName() + "] → " + action + ": " + reason);
        });
        brain.setDialogueListener((name, text) -> {
            System.out.println("[" + name + "]: " + text);
        });
    }

    // ----------------------------------------------------------------
    // Config
    // ----------------------------------------------------------------

    public void setPatrolPoints(org.joml.Vector3f... points) {
        this.patrolPoints = points;
    }

    public void setTalkRange(float r)       { this.talkRange = r; }
    public void setDetectionRange(float r)  { this.detectionRange = r; }
    public void setUpdateInterval(float s)  { this.updateInterval = s; }
    public boolean isInTalkRange(org.joml.Vector3f playerPos) {
        return position.distance(playerPos) <= talkRange;
    }

    public NPCBrain getBrain() { return brain; }

    // ----------------------------------------------------------------
    // W3C DOM stubs
    // ----------------------------------------------------------------

    @Override public void setNodeValue(String v) throws DOMException {}
    @Override public Node getPreviousSibling() { return null; }
    @Override public Node insertBefore(Node n, Node r) throws DOMException { return null; }
    @Override public Node replaceChild(Node n, Node o) throws DOMException { return null; }
    @Override public Node removeChild(Node o) throws DOMException { return null; }
    @Override public Node cloneNode(boolean d) { return null; }
    @Override public void normalize() {}
    @Override public boolean isSupported(String f, String v) { return false; }
    @Override public String getNamespaceURI() { return null; }
    @Override public String getPrefix() { return null; }
    @Override public void setPrefix(String p) throws DOMException {}
    @Override public String getLocalName() { return null; }
    @Override public boolean hasAttributes() { return false; }
    @Override public String getBaseURI() { return null; }
    @Override public short compareDocumentPosition(Node o) throws DOMException { return 0; }
    @Override public String getTextContent() throws DOMException { return null; }
    @Override public void setTextContent(String t) throws DOMException {}
    @Override public boolean isSameNode(Node o) { return false; }
    @Override public String lookupPrefix(String ns) { return null; }
    @Override public boolean isDefaultNamespace(String ns) { return false; }
    @Override public String lookupNamespaceURI(String p) { return null; }
    @Override public boolean isEqualNode(Node o) { return false; }
    @Override public Object getFeature(String f, String v) { return null; }
    @Override public Object setUserData(String k, Object d, UserDataHandler h) { return null; }
    @Override public Object getUserData(String k) { return null; }
}