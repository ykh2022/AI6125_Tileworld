# Update Log

## 2026-03-20 - Repository Initialization

### Changes
- Initialized Git repository content and created first project commit.
- Added project documentation files:
  - `README.md`
  - `update.md`

### Scope
- Imported full Tileworld codebase under:
  - `src/tileworld/**`
  - `bin/tileworld/**`
- Set commit metadata for local repository and created baseline version.

### Output
- Baseline commit created: `331bac9`

---

## 2026-03-20 - README Update Based on group-project.pdf

### Changes
- Rewrote `README.md` to align with assignment requirements in `group-project.pdf`.
- Added explicit "must-follow" constraints to avoid score penalties.

### Key Additions
- Agent specification section:
  - sensing range formula `max(abs(x-X), abs(y-Y)) <= 3`
  - action set requirement
  - tile carrying and fuel constraints
  - Sense-Communicate-Plan-Act loop
- Restriction section:
  - only override `communicate()`, `think()`, `act()`
  - do not modify `environment` package
  - `increaseReward()` call restriction
  - `receiveMessage()` requirement inside `communicate()`
- Experiment setup section:
  - known/unknown competition configurations
  - fixed steps/fuel and repeated-run settings

### Files
- `README.md`

---

## 2026-03-20 - Communication Module Implementation

### Changes
- Implemented structured team communication and coordination features.

### New Files
- `src/tileworld/agent/AgentMessageType.java`
  - Added communication event enum:
    - `TILE_FOUND`, `HOLE_FOUND`, `OBSTACLE_FOUND`
    - `TARGET_CLAIM`, `TARGET_RELEASE`, `TARGET_INVALID`
    - `LOW_FUEL`, `STUCK`
- `src/tileworld/agent/AgentMessage.java`
  - Added rich message payload fields:
    - sender/receiver (via parent `Message`)
    - `type`, `x`, `y`, `timeStep`, `priority`, `ttl`, `messageId`
  - Added helper methods:
    - expiry check (`isExpired`)
    - target key generation (`targetKey`)

### Updated File
- `src/tileworld/agent/SimpleTWAgent.java`

### Implemented Behaviors
- In `communicate()`:
  - broadcast nearby discovery info with cooldown
  - broadcast status (`LOW_FUEL`, `STUCK`)
  - flush queued structured messages via `TWEnvironment.receiveMessage()`
- In `think()`:
  - read and process team broadcast messages
  - handle target claims/releases/invalidations
  - maintain shared target caches with TTL cleanup
  - avoid teammate-claimed targets during planning
  - claim selected target before movement
- In `act()`:
  - on `PICKUP` / `PUTDOWN`, broadcast `TARGET_INVALID`
  - on blocked movement, broadcast `STUCK` and fallback move attempt

### Notes
- Existing environment package was not modified in this round.
- `javac` is unavailable in current runtime environment, so compile check was not executed here.

---

## 2026-03-20 - Comment Standardization

### Changes
- Updated annotations in new/extended communication code:
  - `Author` unified to `YKH`
  - added `Function` explanation in class/method comments

### Files
- `src/tileworld/agent/AgentMessageType.java`
- `src/tileworld/agent/AgentMessage.java`
- `src/tileworld/agent/SimpleTWAgent.java`

---

## 2026-03-20 - Multi-Agent Strategy (Message-Driven)

### Changes
- Implemented a message-driven multi-agent strategy in `SimpleTWAgent` without modifying restricted files.

### Strategy Details
- Zone-based exploration:
  - each agent is assigned a stable x-axis search band from its name/index
  - default behavior prioritizes local exploration within own band
- Cooperative target allocation:
  - target scoring combines distance + out-of-zone penalty
  - agents avoid targets claimed by teammates
  - active target claim is renewed periodically to prevent stale reservations
- Communication-driven coordination:
  - discovery broadcasts (`TILE_FOUND`, `HOLE_FOUND`, `OBSTACLE_FOUND`)
  - reservation lifecycle (`TARGET_CLAIM`, `TARGET_RELEASE`, `TARGET_INVALID`)
  - status broadcasts (`LOW_FUEL`, `STUCK`)
  - TTL cleanup for stale shared data and claims
- Action robustness:
  - on blocked move, broadcast `STUCK`, release claim, attempt fallback move
  - if fuel is `0`, skip move invocation to avoid repeated invalid move attempts

### Compliance
- No changes to `environment` package.
- No changes to `TWAgent` core restricted methods.
- Custom behavior implemented through agent-side logic and message module.

### Files
- `src/tileworld/agent/SimpleTWAgent.java`
- `src/tileworld/agent/AgentMessage.java`
- `src/tileworld/agent/AgentMessageType.java`

### Verification
- Recompiled full project into `bin` using JDK 8 (`javac 1.8.0_482`).
- Smoke test executed for 100 steps using `TWEnvironment` startup/step cycle.
- Result: `SMOKE_OK reward=1 steps=100`.
