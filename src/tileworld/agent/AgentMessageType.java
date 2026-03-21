package tileworld.agent;

/**
 * Author: YKH
 * Time: 2026-03-20
 * Function: Defines all message event types for team communication and coordination.
 */
public enum AgentMessageType {
    TILE_FOUND,
    HOLE_FOUND,
    OBSTACLE_FOUND,
    TARGET_CLAIM,
    TARGET_RELEASE,
    TARGET_INVALID,
    LOW_FUEL,
    STUCK
}
