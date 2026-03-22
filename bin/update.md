# Update Log

## 2026-03-20 - Repository Initialization

### Changes
- Initialized Git repository and created first project commit.
- Added project documentation files: `README.md`, `update.md`
- Imported full Tileworld codebase (`src/tileworld/**`, `bin/tileworld/**`)

---

## 2026-03-20 - Communication Module Implementation

### New Files
- `src/tileworld/agent/AgentMessageType.java`
  - 定义 9 种消息类型：TILE_FOUND, HOLE_FOUND, OBSTACLE_FOUND, TARGET_CLAIM, TARGET_RELEASE, TARGET_INVALID, LOW_FUEL, STUCK, FUEL_STATION_FOUND
- `src/tileworld/agent/AgentMessage.java`
  - 结构化消息载体：坐标、时间戳、优先级、TTL、过期检查

### Updated Files
- `src/tileworld/agent/SimpleTWAgent.java`
  - `communicate()`：广播发现信息（带冷却）、广播状态
  - `think()`：处理团队消息、目标声明/释放/失效、TTL 清理
  - `act()`：拾取/放下时广播 TARGET_INVALID、被阻挡时尝试回退

---

## 2026-03-20 - Multi-Agent Strategy (Message-Driven)

### Strategy Details
- Zone-based exploration：按 agent 编号分配搜索区域
- Cooperative target allocation：距离 + zone penalty 评分，避免重复声明
- Communication-driven coordination：发现广播、声明生命周期、状态广播
- Action robustness：被阻挡时广播 STUCK 并尝试回退移动

### Compliance
- 未修改 `environment` package 的核心逻辑
- 未修改 `TWAgent` 受限方法
- 所有自定义行为通过 agent 侧逻辑和消息模块实现

---

## 2026-03-21 - CatGUI and Build Scripts

### New Files
- `src/tileworld/CatGUI.java`：猫猫主题 GUI，支持精灵图渲染、agent 状态面板、记忆层可视化
- `build.bat`：JDK 8 编译脚本
- `run_gui.bat`：标准 GUI 启动脚本
- `run_gui_cat.bat`：CatGUI 启动脚本
- `run_headless.bat`：无头模式启动脚本
- `run_eval.ps1`：批量评测 PowerShell 脚本

### Updated Files
- `src/tileworld/Parameters.java`：调整默认参数
- `src/tileworld/agent/TWAgentWorkingMemory.java`：增强记忆模块
- `src/tileworld/environment/TWEnvironment.java`：适配 RLTWAgent 创建

---

## 2026-03-21 - RLTWAgent Implementation

### New Files
- `src/tileworld/agent/RLTWAgent.java`
  - 基于强化学习的 agent 实现
  - 继承通信协调框架（与 SimpleTWAgent 共享消息协议）
  - 扩展参数调优：更大的 INFO_TTL (50)、更小的 zone penalty (1.5)

### Notes
- RLTWAgent 与 SimpleTWAgent 可混合部署，共享同一套 AgentMessageType 协议

---

## 2026-03-22 - Documentation Update

### Changes
- 更新 `README.md`：新增 Agent 实现说明（SimpleTWAgent / RLTWAgent）、通信模块描述、运行命令
- 更新 `update.md`：补全所有变更记录
- 清理 `sources.txt` 和 `updata.md`（已删除）
