/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tileworld.agent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import tileworld.Parameters;
import sim.util.Bag;
import tileworld.environment.TWDirection;
import tileworld.environment.TWEntity;
import tileworld.environment.TWEnvironment;
import tileworld.environment.TWHole;
import tileworld.environment.TWObstacle;
import tileworld.environment.TWTile;
import tileworld.exceptions.CellBlockedException;

/**
 * TWContextBuilder
 *
 * @author michaellees
 * Created: Feb 6, 2011
 *
 * Copyright michaellees Expression year is undefined on line 16, column 24 in Templates/Classes/Class.java.
 *
 *
 * Description:
 *
 * Function: Implements a communication-aware multi-agent strategy with
 * zone-based exploration, target claiming and invalidation synchronization.
 */
public class SimpleTWAgent extends TWAgent {
    private static final int LOW_FUEL_THRESHOLD = 60;
    private static final int CLAIM_TTL = 8;
    private static final int CLAIM_RENEW_INTERVAL = 3;
    private static final int INFO_TTL = 10;
    private static final int DISCOVERY_COOLDOWN = 4;
    private static final int STATUS_COOLDOWN = 8;
    private static final int STUCK_STEPS = 4;
    private static final int ASSIGNMENT_SLOTS = 6;
    private static final double OUTSIDE_ZONE_PENALTY = 7.0;

    private final String name;
    private final LinkedHashMap<String, AgentMessage> outboundMessages;
    private final HashMap<String, Integer> discoveryLastBroadcastStep;
    private final HashMap<String, Integer> teammateClaimExpiry;
    private final HashMap<String, TargetInfo> sharedTileTargets;
    private final HashMap<String, TargetInfo> sharedHoleTargets;

    private final int slotIndex;
    private final int assignedMinX;
    private final int assignedMaxX;

    private String ownedClaimKey;
    private int lastClaimBroadcastStep;
    private int lastX;
    private int lastY;
    private int stationarySteps;
    private int lastLowFuelBroadcastStep;
    private int lastStuckBroadcastStep;
    private TWDirection explorationVerticalDirection;

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Stores a candidate target with freshness window for planning.
     */
    private static class TargetInfo {
        private final int x;
        private final int y;
        private int observedAt;
        private int ttl;

        /**
         * Author: YKH
         * Time: 2026-03-20
         * Function: Creates a target record with coordinate and freshness metadata.
         */
        TargetInfo(int x, int y, int observedAt, int ttl) {
            this.x = x;
            this.y = y;
            this.observedAt = observedAt;
            this.ttl = ttl;
        }

        /**
         * Author: YKH
         * Time: 2026-03-20
         * Function: Checks whether this target info is stale at current step.
         */
        boolean isExpired(int step) {
            return step > observedAt + ttl;
        }

        /**
         * Author: YKH
         * Time: 2026-03-20
         * Function: Refreshes freshness metadata for an already-known target.
         */
        void refresh(int observedAt, int ttl) {
            this.observedAt = observedAt;
            this.ttl = ttl;
        }
    }

    public SimpleTWAgent(String name, int xpos, int ypos, TWEnvironment env, double fuelLevel) {
        super(xpos, ypos, env, fuelLevel);
        this.name = name;
        this.outboundMessages = new LinkedHashMap<String, AgentMessage>();
        this.discoveryLastBroadcastStep = new HashMap<String, Integer>();
        this.teammateClaimExpiry = new HashMap<String, Integer>();
        this.sharedTileTargets = new HashMap<String, TargetInfo>();
        this.sharedHoleTargets = new HashMap<String, TargetInfo>();

        this.slotIndex = computeSlotIndex(name, ASSIGNMENT_SLOTS);
        int[] band = computeAssignedBand(env.getxDimension(), this.slotIndex, ASSIGNMENT_SLOTS);
        this.assignedMinX = band[0];
        this.assignedMaxX = band[1];

        this.ownedClaimKey = null;
        this.lastClaimBroadcastStep = -CLAIM_RENEW_INTERVAL;
        this.lastX = xpos;
        this.lastY = ypos;
        this.stationarySteps = 0;
        this.lastLowFuelBroadcastStep = -STATUS_COOLDOWN;
        this.lastStuckBroadcastStep = -STATUS_COOLDOWN;
        this.explorationVerticalDirection = (slotIndex % 2 == 0) ? TWDirection.S : TWDirection.N;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Broadcasts discovery/status updates and renews current claim periodically.
     */
    @Override
    public void communicate() {
        int step = getCurrentStep();

        broadcastDiscoveryIfNeeded(AgentMessageType.TILE_FOUND, getMemory().getClosestObjectInSensorRange(TWTile.class), step);
        broadcastDiscoveryIfNeeded(AgentMessageType.HOLE_FOUND, getMemory().getClosestObjectInSensorRange(TWHole.class), step);
        broadcastDiscoveryIfNeeded(AgentMessageType.OBSTACLE_FOUND, getMemory().getClosestObjectInSensorRange(TWObstacle.class), step);

        renewOwnedClaimIfNeeded(step);
        updateAndBroadcastStatus(step);
        flushOutboundMessages();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Consumes shared messages, chooses cooperative target, then returns current-step action.
     */
    @Override
    protected TWThought think() {
        int step = getCurrentStep();
        processIncomingMessages(step);
        cleanupExpiredState(step);

        TWEntity currentCellObject = getCurrentCellObject();

        if (currentCellObject instanceof TWTile && carriedTiles.size() < 3) {
            return new TWThought(TWAction.PICKUP, TWDirection.Z);
        }

        if (currentCellObject instanceof TWHole && this.hasTile()) {
            return new TWThought(TWAction.PUTDOWN, TWDirection.Z);
        }

        if (this.getEnvironment().inFuelStation(this) && this.getFuelLevel() < Parameters.defaultFuelLevel) {
            return new TWThought(TWAction.REFUEL, TWDirection.Z);
        }

        TargetInfo target = this.hasTile() ? chooseHoleTarget(step) : chooseTileTarget(step);
        if (target != null) {
            claimTargetIfNeeded(target, step);
            return new TWThought(TWAction.MOVE, directionToward(target.x, target.y));
        }

        releaseOwnedClaimIfAny(step);

        if (this.getFuelLevel() <= LOW_FUEL_THRESHOLD) {
            return new TWThought(TWAction.MOVE, TWDirection.Z);
        }

        return new TWThought(TWAction.MOVE, explorationDirection());
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Executes chosen action and emits invalidation/stuck signals on state changes.
     */
    @Override
    protected void act(TWThought thought) {
        int step = getCurrentStep();

        if (thought.getAction() == TWAction.PICKUP) {
            TWEntity cell = getCurrentCellObject();
            if (cell instanceof TWTile && carriedTiles.size() < 3) {
                TWTile tile = (TWTile) cell;
                pickUpTile(tile);
                getMemory().removeObject(tile);
                enqueueMessage(AgentMessageType.TARGET_INVALID, tile.getX(), tile.getY(), step, 10, INFO_TTL, "");
                clearOwnedClaimIfMatches(tile.getX(), tile.getY());
            }
            return;
        }

        if (thought.getAction() == TWAction.PUTDOWN) {
            TWEntity cell = getCurrentCellObject();
            if (cell instanceof TWHole && this.hasTile()) {
                TWHole hole = (TWHole) cell;
                putTileInHole(hole);
                getMemory().removeObject(hole);
                enqueueMessage(AgentMessageType.TARGET_INVALID, hole.getX(), hole.getY(), step, 10, INFO_TTL, "");
                clearOwnedClaimIfMatches(hole.getX(), hole.getY());
            }
            return;
        }

        if (thought.getAction() == TWAction.REFUEL) {
            refuel();
            return;
        }

        if (thought.getAction() == TWAction.MOVE && this.getFuelLevel() <= 0) {
            return;
        }

        try {
            this.move(thought.getDirection());
        } catch (CellBlockedException ex) {
            enqueueMessage(AgentMessageType.STUCK, this.getX(), this.getY(), step, 9, INFO_TTL, "");
            releaseOwnedClaimIfAny(step);
            try {
                this.move(getRandomDirection());
            } catch (CellBlockedException ignored) {
                // No-op: if both directions are blocked the agent remains in place.
            }
        }
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Ingests team messages and updates shared target/claim states.
     */
    private void processIncomingMessages(int step) {
        Bag messages = new Bag();
        messages.addAll(this.getEnvironment().getMessages());

        for (int i = 0; i < messages.size(); i++) {
            Message raw = (Message) messages.get(i);
            if (!(raw instanceof AgentMessage)) {
                continue;
            }

            AgentMessage message = (AgentMessage) raw;
            if (this.getName().equals(message.getFrom())) {
                continue;
            }
            if (!isMessageForMe(message)) {
                continue;
            }
            if (message.isExpired(step)) {
                continue;
            }

            String key = targetKey(message.getX(), message.getY());
            switch (message.getType()) {
                case TARGET_CLAIM:
                    teammateClaimExpiry.put(key, message.getTimeStep() + message.getTtl());
                    break;
                case TARGET_RELEASE:
                case TARGET_INVALID:
                    teammateClaimExpiry.remove(key);
                    sharedTileTargets.remove(key);
                    sharedHoleTargets.remove(key);
                    if (key.equals(ownedClaimKey)) {
                        ownedClaimKey = null;
                    }
                    break;
                case TILE_FOUND:
                    upsertSharedTarget(sharedTileTargets, message, step);
                    break;
                case HOLE_FOUND:
                    upsertSharedTarget(sharedHoleTargets, message, step);
                    break;
                default:
                    // LOW_FUEL/STUCK/OBSTACLE_FOUND are informative for now.
                    break;
            }
        }
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Removes expired claim and stale shared target entries.
     */
    private void cleanupExpiredState(int step) {
        Iterator<Map.Entry<String, Integer>> claimIt = teammateClaimExpiry.entrySet().iterator();
        while (claimIt.hasNext()) {
            Map.Entry<String, Integer> claim = claimIt.next();
            if (claim.getValue().intValue() < step) {
                claimIt.remove();
            }
        }

        pruneSharedTargets(sharedTileTargets, step);
        pruneSharedTargets(sharedHoleTargets, step);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Drops stale entries in a shared target map.
     */
    private void pruneSharedTargets(HashMap<String, TargetInfo> targetMap, int step) {
        Iterator<Map.Entry<String, TargetInfo>> it = targetMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TargetInfo> entry = it.next();
            if (entry.getValue().isExpired(step)) {
                it.remove();
            }
        }
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Inserts or refreshes target info received from teammates.
     */
    private void upsertSharedTarget(HashMap<String, TargetInfo> targetMap, AgentMessage message, int step) {
        String key = targetKey(message.getX(), message.getY());
        TargetInfo target = targetMap.get(key);
        if (target == null) {
            targetMap.put(key, new TargetInfo(message.getX(), message.getY(), step, message.getTtl()));
        } else {
            target.refresh(step, message.getTtl());
        }
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Chooses a tile target by combining sensed, remembered and shared info.
     */
    private TargetInfo chooseTileTarget(int step) {
        TargetInfo best = null;

        TWEntity sensed = getMemory().getClosestObjectInSensorRange(TWTile.class);
        best = pickBetterTarget(best, toTargetInfo(sensed, step), step);

        TWTile remembered = getMemory().getNearbyTile(this.getX(), this.getY(), 6);
        if (remembered != null) {
            best = pickBetterTarget(best, new TargetInfo(remembered.getX(), remembered.getY(), step, INFO_TTL), step);
        }

        best = pickBetterTarget(best, chooseSharedTarget(sharedTileTargets, step), step);
        return best;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Chooses a hole target by combining sensed, remembered and shared info.
     */
    private TargetInfo chooseHoleTarget(int step) {
        TargetInfo best = null;

        TWEntity sensed = getMemory().getClosestObjectInSensorRange(TWHole.class);
        best = pickBetterTarget(best, toTargetInfo(sensed, step), step);

        TWHole remembered = getMemory().getNearbyHole(this.getX(), this.getY(), 6);
        if (remembered != null) {
            best = pickBetterTarget(best, new TargetInfo(remembered.getX(), remembered.getY(), step, INFO_TTL), step);
        }

        best = pickBetterTarget(best, chooseSharedTarget(sharedHoleTargets, step), step);
        return best;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Selects a valid shared target with minimal cooperative score.
     */
    private TargetInfo chooseSharedTarget(HashMap<String, TargetInfo> targetMap, int step) {
        TargetInfo best = null;

        for (Map.Entry<String, TargetInfo> entry : targetMap.entrySet()) {
            String key = entry.getKey();
            TargetInfo candidate = entry.getValue();

            if (candidate.isExpired(step)) {
                continue;
            }
            if (isClaimedByTeammate(key, step)) {
                continue;
            }

            best = pickBetterTarget(best, candidate, step);
        }

        return best;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns the better target based on claim availability and score.
     */
    private TargetInfo pickBetterTarget(TargetInfo currentBest, TargetInfo candidate, int step) {
        if (candidate == null) {
            return currentBest;
        }

        String key = targetKey(candidate.x, candidate.y);
        if (isClaimedByTeammate(key, step)) {
            return currentBest;
        }

        if (currentBest == null) {
            return candidate;
        }

        double candidateScore = scoreTarget(candidate);
        double bestScore = scoreTarget(currentBest);
        return (candidateScore < bestScore) ? candidate : currentBest;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Computes target score with distance and zone preference penalty.
     */
    private double scoreTarget(TargetInfo target) {
        double score = this.getDistanceTo(target.x, target.y);
        if (!isInAssignedBand(target.x)) {
            score += OUTSIDE_ZONE_PENALTY;
        }
        if (ownedClaimKey != null && ownedClaimKey.equals(targetKey(target.x, target.y))) {
            score -= 1.5;
        }
        return score;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Claims a target if changed; otherwise renews claim with a fixed interval.
     */
    private void claimTargetIfNeeded(TargetInfo target, int step) {
        String key = targetKey(target.x, target.y);

        if (!key.equals(ownedClaimKey)) {
            releaseOwnedClaimIfAny(step);
            ownedClaimKey = key;
            enqueueMessage(AgentMessageType.TARGET_CLAIM, target.x, target.y, step, 8, CLAIM_TTL, "");
            lastClaimBroadcastStep = step;
            return;
        }

        renewOwnedClaimIfNeeded(step);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Refreshes own claim broadcast so teammates keep respecting reservation.
     */
    private void renewOwnedClaimIfNeeded(int step) {
        if (ownedClaimKey == null) {
            return;
        }
        if (step - lastClaimBroadcastStep < CLAIM_RENEW_INTERVAL) {
            return;
        }

        int separator = ownedClaimKey.indexOf(':');
        int x = Integer.parseInt(ownedClaimKey.substring(0, separator));
        int y = Integer.parseInt(ownedClaimKey.substring(separator + 1));

        enqueueMessage(AgentMessageType.TARGET_CLAIM, x, y, step, 8, CLAIM_TTL, "");
        lastClaimBroadcastStep = step;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Releases current target claim and informs teammates.
     */
    private void releaseOwnedClaimIfAny(int step) {
        if (ownedClaimKey == null) {
            return;
        }

        int separator = ownedClaimKey.indexOf(':');
        int x = Integer.parseInt(ownedClaimKey.substring(0, separator));
        int y = Integer.parseInt(ownedClaimKey.substring(separator + 1));
        enqueueMessage(AgentMessageType.TARGET_RELEASE, x, y, step, 8, CLAIM_TTL, "");
        ownedClaimKey = null;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Clears own claim marker when claimed target is consumed.
     */
    private void clearOwnedClaimIfMatches(int x, int y) {
        String key = targetKey(x, y);
        if (key.equals(ownedClaimKey)) {
            ownedClaimKey = null;
        }
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Checks if target is currently reserved by teammate claim.
     */
    private boolean isClaimedByTeammate(String key, int step) {
        Integer expiry = teammateClaimExpiry.get(key);
        return expiry != null && expiry.intValue() >= step;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Broadcasts low-fuel and stuck signals for teammate awareness.
     */
    private void updateAndBroadcastStatus(int step) {
        if (this.getFuelLevel() <= LOW_FUEL_THRESHOLD && step - lastLowFuelBroadcastStep >= STATUS_COOLDOWN) {
            enqueueMessage(AgentMessageType.LOW_FUEL, this.getX(), this.getY(), step, 9, INFO_TTL, "");
            lastLowFuelBroadcastStep = step;
        }

        if (this.getX() == lastX && this.getY() == lastY) {
            stationarySteps++;
        } else {
            stationarySteps = 0;
        }

        if (stationarySteps >= STUCK_STEPS && step - lastStuckBroadcastStep >= STATUS_COOLDOWN) {
            enqueueMessage(AgentMessageType.STUCK, this.getX(), this.getY(), step, 9, INFO_TTL, "");
            releaseOwnedClaimIfAny(step);
            lastStuckBroadcastStep = step;
        }

        lastX = this.getX();
        lastY = this.getY();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Broadcasts sensed entities with cooldown to avoid message flooding.
     */
    private void broadcastDiscoveryIfNeeded(AgentMessageType type, TWEntity entity, int step) {
        if (entity == null) {
            return;
        }

        String discoveryKey = type.name() + ":" + entity.getX() + ":" + entity.getY();
        Integer lastBroadcast = discoveryLastBroadcastStep.get(discoveryKey);
        if (lastBroadcast != null && step - lastBroadcast.intValue() < DISCOVERY_COOLDOWN) {
            return;
        }

        enqueueMessage(type, entity.getX(), entity.getY(), step, 6, INFO_TTL, "");
        discoveryLastBroadcastStep.put(discoveryKey, step);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Queues a deduplicated structured message for current time step.
     */
    private void enqueueMessage(AgentMessageType type, int x, int y, int step, int priority, int ttl, String to) {
        String recipient = to == null ? "" : to;
        String messageId = this.getName() + "#" + step + "#" + type.name() + "#" + x + "#" + y + "#" + recipient;
        if (outboundMessages.containsKey(messageId)) {
            return;
        }

        AgentMessage message = new AgentMessage(this.getName(), recipient, type, x, y, step, priority, ttl, messageId);
        outboundMessages.put(messageId, message);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Sends queued messages to environment broadcast channel.
     */
    private void flushOutboundMessages() {
        for (AgentMessage message : outboundMessages.values()) {
            this.getEnvironment().receiveMessage(message);
        }
        outboundMessages.clear();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Checks whether message target is broadcast or this agent.
     */
    private boolean isMessageForMe(AgentMessage message) {
        String to = message.getTo();
        return to == null || to.isEmpty() || "ALL".equalsIgnoreCase(to) || this.getName().equals(to);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns current simulation step as integer.
     */
    private int getCurrentStep() {
        return (int) this.getEnvironment().schedule.getSteps();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Returns object currently under agent position.
     */
    private TWEntity getCurrentCellObject() {
        return (TWEntity) this.getEnvironment().getObjectGrid().get(this.getX(), this.getY());
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Builds normalized coordinate key used by claim and cache maps.
     */
    private String targetKey(int x, int y) {
        return x + ":" + y;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Converts sensed entity into short-lived target candidate.
     */
    private TargetInfo toTargetInfo(TWEntity entity, int step) {
        if (entity == null) {
            return null;
        }
        return new TargetInfo(entity.getX(), entity.getY(), step, INFO_TTL);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Picks movement direction toward target with obstacle fallback.
     */
    private TWDirection directionToward(int tx, int ty) {
        TWDirection primaryX = tx > this.getX() ? TWDirection.E : TWDirection.W;
        TWDirection primaryY = ty > this.getY() ? TWDirection.S : TWDirection.N;

        if (tx == this.getX()) {
            if (isValidMoveDirection(primaryY)) {
                return primaryY;
            }
        } else if (ty == this.getY()) {
            if (isValidMoveDirection(primaryX)) {
                return primaryX;
            }
        } else {
            if (this.getEnvironment().random.nextBoolean()) {
                if (isValidMoveDirection(primaryX)) {
                    return primaryX;
                }
                if (isValidMoveDirection(primaryY)) {
                    return primaryY;
                }
            } else {
                if (isValidMoveDirection(primaryY)) {
                    return primaryY;
                }
                if (isValidMoveDirection(primaryX)) {
                    return primaryX;
                }
            }
        }

        return getRandomDirection();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Generates exploration direction constrained by assigned x-band.
     */
    private TWDirection explorationDirection() {
        if (this.getX() < assignedMinX && isValidMoveDirection(TWDirection.E)) {
            return TWDirection.E;
        }
        if (this.getX() > assignedMaxX && isValidMoveDirection(TWDirection.W)) {
            return TWDirection.W;
        }

        if (this.getY() <= 0) {
            explorationVerticalDirection = TWDirection.S;
        } else if (this.getY() >= this.getEnvironment().getyDimension() - 1) {
            explorationVerticalDirection = TWDirection.N;
        }

        if (isValidMoveDirection(explorationVerticalDirection)) {
            return explorationVerticalDirection;
        }

        TWDirection horizontal = this.getEnvironment().random.nextBoolean() ? TWDirection.E : TWDirection.W;
        if (isValidMoveDirection(horizontal)) {
            return horizontal;
        }

        TWDirection altHorizontal = (horizontal == TWDirection.E) ? TWDirection.W : TWDirection.E;
        if (isValidMoveDirection(altHorizontal)) {
            return altHorizontal;
        }

        return getRandomDirection();
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Checks move validity against boundary and known hard obstacles.
     */
    private boolean isValidMoveDirection(TWDirection direction) {
        if (direction == TWDirection.Z) {
            return true;
        }

        int nx = this.getX() + direction.dx;
        int ny = this.getY() + direction.dy;
        return this.getEnvironment().isInBounds(nx, ny) && !this.getEnvironment().isCellBlocked(nx, ny);
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Samples fallback random direction among valid cardinal moves.
     */
    private TWDirection getRandomDirection() {
        TWDirection[] candidates = new TWDirection[]{TWDirection.E, TWDirection.W, TWDirection.N, TWDirection.S};

        for (int i = 0; i < candidates.length; i++) {
            TWDirection dir = candidates[this.getEnvironment().random.nextInt(candidates.length)];
            if (isValidMoveDirection(dir)) {
                return dir;
            }
        }

        return TWDirection.Z;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Checks if x-coordinate lies inside this agent's assigned search band.
     */
    private boolean isInAssignedBand(int x) {
        return x >= assignedMinX && x <= assignedMaxX;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Computes a stable slot index from agent name suffix/hash.
     */
    private static int computeSlotIndex(String agentName, int slots) {
        int parsedNumber = -1;
        int i = agentName.length() - 1;
        while (i >= 0 && Character.isDigit(agentName.charAt(i))) {
            i--;
        }
        if (i < agentName.length() - 1) {
            try {
                parsedNumber = Integer.parseInt(agentName.substring(i + 1));
            } catch (NumberFormatException ignored) {
                parsedNumber = -1;
            }
        }

        if (parsedNumber > 0) {
            return (parsedNumber - 1) % slots;
        }
        return Math.abs(agentName.hashCode()) % slots;
    }

    /**
     * Author: YKH
     * Time: 2026-03-20
     * Function: Splits world x-axis into slot bands and returns [minX, maxX].
     */
    private static int[] computeAssignedBand(int width, int slotIndex, int slots) {
        int[] band = new int[2];

        int effectiveSlots = Math.min(slots, Math.max(1, width));
        int normalizedSlot = slotIndex % effectiveSlots;
        int base = width / effectiveSlots;
        int remainder = width % effectiveSlots;

        int start = 0;
        for (int i = 0; i < normalizedSlot; i++) {
            start += base + (i < remainder ? 1 : 0);
        }

        int size = base + (normalizedSlot < remainder ? 1 : 0);
        int end = start + Math.max(1, size) - 1;

        band[0] = Math.max(0, start);
        band[1] = Math.min(width - 1, end);
        return band;
    }

    @Override
    public String getName() {
        return name;
    }
}
