package vn.pmgteam.yanase.ai;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * NPCBrain — Bộ não AI cho NPC.
 * Kế thừa AISystem, thêm:
 * - Memory: nhớ lịch sử hội thoại
 * - Personality: tính cách NPC
 * - Decision: tự quyết định hành động
 */
public class NPCBrain extends AISystem {

    // ----------------------------------------------------------------
    // NPC Action enum
    // ----------------------------------------------------------------
    public enum Action {
        IDLE, PATROL, CHASE, FLEE, ATTACK, TALK, SLEEP, INVESTIGATE;

        public static Action fromString(String s) {
            if (s == null) return IDLE;
            return switch (s.toUpperCase().trim()) {
                case "PATROL"      -> PATROL;
                case "CHASE"       -> CHASE;
                case "FLEE"        -> FLEE;
                case "ATTACK"      -> ATTACK;
                case "TALK"        -> TALK;
                case "SLEEP"       -> SLEEP;
                case "INVESTIGATE" -> INVESTIGATE;
                default            -> IDLE;
            };
        }
    }

    // ----------------------------------------------------------------
    // Fields
    // ----------------------------------------------------------------

    private final String npcName;
    private final String personality;   // "friendly guard", "aggressive bandit", v.v.
    private final int    maxMemory;     // Số lượt hội thoại tối đa nhớ được

    // Lịch sử hội thoại — format: ["user: ...", "npc: ..."]
    private final LinkedList<String> conversationHistory = new LinkedList<>();

    // Trạng thái hiện tại
    private Action currentAction = Action.IDLE;
    private float  health        = 100f;
    private float  alertLevel    = 0f;   // 0.0 = bình thường, 1.0 = báo động tối đa
    private String lastDialogue  = "";

    // Listeners
    private ActionListener  actionListener;
    private DialogueListener dialogueListener;

    public interface ActionListener  { void onAction(Action action, String reason); }
    public interface DialogueListener { void onDialogue(String npcName, String text); }

    // ----------------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------------

    public NPCBrain(String npcName, String personality) {
        this(npcName, personality, 10);
    }

    public NPCBrain(String npcName, String personality, int maxMemory) {
        this.npcName     = npcName;
        this.personality = personality;
        this.maxMemory   = maxMemory;
    }

    // ----------------------------------------------------------------
    // Dialogue — NPC trả lời hội thoại
    // ----------------------------------------------------------------

    /**
     * Player nói chuyện với NPC.
     * @param playerInput Câu nói của player
     * @return CompletableFuture<String> câu trả lời của NPC
     */
    public CompletableFuture<String> talk(String playerInput) {
        // Thêm input của player vào memory
        addMemory("Player: " + playerInput);

        String prompt = buildDialoguePrompt(playerInput);

        return askGlobalAI(prompt).thenApply(response -> {
            // Lọc response
            String clean = cleanResponse(response);
            addMemory(npcName + ": " + clean);
            lastDialogue = clean;

            if (dialogueListener != null)
                dialogueListener.onDialogue(npcName, clean);

            return clean;
        });
    }

    /**
     * NPC tự quyết định hành động dựa vào context.
     * @param worldContext Mô tả tình huống hiện tại (vd: "Player đang tiến lại gần với vũ khí")
     * @return CompletableFuture<Action>
     */
    public CompletableFuture<Action> decideAction(String worldContext) {
        String prompt = buildDecisionPrompt(worldContext);

        return askGlobalAI(prompt).thenApply(response -> {
            Action action = parseAction(response);
            String reason = extractReason(response);

            currentAction = action;

            if (actionListener != null)
                actionListener.onAction(action, reason);

            System.out.println("[" + npcName + "] Decided: " + action + " — " + reason);
            return action;
        });
    }

    /**
     * Cập nhật trạng thái NPC và tự quyết định hành động mới nếu cần.
     * Gọi trong game loop khi có sự kiện quan trọng.
     */
    public CompletableFuture<Action> update(String worldContext) {
        // Cập nhật alert level dựa vào context
        if (worldContext.contains("enemy") || worldContext.contains("attack")
                || worldContext.contains("danger")) {
            alertLevel = Math.min(1.0f, alertLevel + 0.3f);
        } else {
            alertLevel = Math.max(0.0f, alertLevel - 0.05f);
        }

        return decideAction(worldContext);
    }

    // ----------------------------------------------------------------
    // Prompt builders
    // ----------------------------------------------------------------

    private String buildDialoguePrompt(String playerInput) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(npcName)
          .append(", a ").append(personality).append(" in a video game.\n");
        sb.append("Respond naturally in character. Keep response under 2 sentences.\n");
        sb.append("Current health: ").append((int)health).append("/100\n");
        sb.append("Alert level: ").append(String.format("%.0f", alertLevel * 100)).append("%\n\n");

        // Thêm lịch sử hội thoại
        if (!conversationHistory.isEmpty()) {
            sb.append("Previous conversation:\n");
            conversationHistory.forEach(line -> sb.append(line).append("\n"));
            sb.append("\n");
        }

        sb.append("Player says: \"").append(playerInput).append("\"\n");
        sb.append(npcName).append(" responds:");
        return sb.toString();
    }

    private String buildDecisionPrompt(String worldContext) {
        return """
            You are %s, a %s in a video game.
            Current state: health=%d/100, alert=%.0f%%
            Situation: %s
            
            Choose ONE action from: IDLE, PATROL, CHASE, FLEE, ATTACK, TALK, SLEEP, INVESTIGATE
            
            Respond in this exact format:
            ACTION: <action>
            REASON: <one short sentence>
            """.formatted(
                npcName, personality,
                (int)health, alertLevel * 100,
                worldContext
            );
    }

    // ----------------------------------------------------------------
    // Response parsing
    // ----------------------------------------------------------------

    @Override
    protected String parseAiResponse(String jsonRaw) {
        // Parse OpenAI response format
        try {
            // Tìm "content": "..." trong JSON
            int idx = jsonRaw.indexOf("\"content\":");
            if (idx == -1) return "[No response]";

            int start = jsonRaw.indexOf("\"", idx + 10) + 1;
            int end   = jsonRaw.indexOf("\"", start);

            // Handle escaped quotes
            while (end > 0 && jsonRaw.charAt(end - 1) == '\\') {
                end = jsonRaw.indexOf("\"", end + 1);
            }

            if (start <= 0 || end <= start) return "[Parse error]";

            return jsonRaw.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        } catch (Exception e) {
            return "[Error parsing response]";
        }
    }

    private Action parseAction(String response) {
        // Tìm "ACTION: CHASE" pattern
        String upper = response.toUpperCase();
        int idx = upper.indexOf("ACTION:");
        if (idx >= 0) {
            String after = upper.substring(idx + 7).trim();
            String word  = after.split("\\s+")[0].replaceAll("[^A-Z]", "");
            return Action.fromString(word);
        }
        // Fallback: tìm keyword bất kỳ trong response
        for (Action a : Action.values()) {
            if (upper.contains(a.name())) return a;
        }
        return Action.IDLE;
    }

    private String extractReason(String response) {
        int idx = response.toUpperCase().indexOf("REASON:");
        if (idx >= 0) {
            String after = response.substring(idx + 7).trim();
            int nl = after.indexOf('\n');
            return nl > 0 ? after.substring(0, nl).trim() : after.trim();
        }
        return "";
    }

    private String cleanResponse(String raw) {
        if (raw.startsWith("[Error") || raw.startsWith("[Network")) return raw;
        // Bỏ prefix "NpcName:" nếu AI tự thêm vào
        String clean = raw.trim();
        if (clean.toLowerCase().startsWith(npcName.toLowerCase() + ":")) {
            clean = clean.substring(npcName.length() + 1).trim();
        }
        return clean;
    }

    // ----------------------------------------------------------------
    // Memory management
    // ----------------------------------------------------------------

    private void addMemory(String entry) {
        conversationHistory.addLast(entry);
        // Giới hạn memory
        while (conversationHistory.size() > maxMemory * 2) {
            conversationHistory.removeFirst();
        }
    }

    public void clearMemory() {
        conversationHistory.clear();
        System.out.println("[" + npcName + "] Memory cleared.");
    }

    public List<String> getMemory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    // ----------------------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------------------

    public String  getName()          { return npcName; }
    public Action  getCurrentAction() { return currentAction; }
    public float   getHealth()        { return health; }
    public float   getAlertLevel()    { return alertLevel; }
    public String  getLastDialogue()  { return lastDialogue; }

    public void setHealth(float h)      { this.health = Math.max(0, Math.min(100, h)); }
    public void setAlertLevel(float a)  { this.alertLevel = Math.max(0, Math.min(1, a)); }

    public void setActionListener(ActionListener l)    { this.actionListener = l; }
    public void setDialogueListener(DialogueListener l) { this.dialogueListener = l; }
}