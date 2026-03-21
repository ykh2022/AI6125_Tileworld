package tileworld.agent;

/**
 * Author: YKH
 * Time: 2026-03-20
 * Function: Encapsulates structured communication payloads exchanged between agents.
 */
public class AgentMessage extends Message {
    private final AgentMessageType type;
    private final int x;
    private final int y;
    private final int timeStep;
    private final int priority;
    private final int ttl;
    private final String messageId;

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Builds a structured message with event type, target position and lifecycle metadata.
     */
    public AgentMessage(String from,
                        String to,
                        AgentMessageType type,
                        int x,
                        int y,
                        int timeStep,
                        int priority,
                        int ttl,
                        String messageId) {
        super(from, to, type.name() + "@" + x + "," + y + "#" + timeStep);
        this.type = type;
        this.x = x;
        this.y = y;
        this.timeStep = timeStep;
        this.priority = priority;
        this.ttl = ttl;
        this.messageId = messageId;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns semantic event type of this message.
     */
    public AgentMessageType getType() {
        return type;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns x-coordinate carried by this message.
     */
    public int getX() {
        return x;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns y-coordinate carried by this message.
     */
    public int getY() {
        return y;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns simulation step when this message was created.
     */
    public int getTimeStep() {
        return timeStep;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns priority level used by message consumer logic.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns time-to-live (TTL) in simulation steps.
     */
    public int getTtl() {
        return ttl;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns globally unique message identifier for deduplication.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Checks whether this message should be considered expired at current step.
     */
    public boolean isExpired(int currentStep) {
        return currentStep > (timeStep + ttl);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns normalized target coordinate key in "x:y" format.
     */
    public String targetKey() {
        return x + ":" + y;
    }
}
