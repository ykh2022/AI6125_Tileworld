# Tileworld Group Project (AI6125)

本项目是 AI6125《Multi-agent System》的课程小组项目实现，基于 MASON 的 Tileworld 仿真环境。

## 项目背景

Tileworld 是一个网格环境，包含：
- Agent
- Tile
- Hole
- Obstacle
- Fuel Station

核心目标是让 agent 在动态环境中高效搬运 tile 填补 hole，以获得更高 reward。

## 课程要求中的 Agent 规格

每个学生设计 1 个 agent，且必须满足：
- 感知范围：`max(abs(x-X), abs(y-Y)) <= 3`
- 动作集合：`{WAIT, MOVE_UP, MOVE_DOWN, MOVE_LEFT, MOVE_RIGHT, PICK_UP, DROP, REFUEL}`
- 最多携带 `3` 个 tile
- 每移动一步消耗 `1` fuel；fuel 为 `0` 时无法移动
- 每个 time step 执行：Sense -> Communicate(可选) -> Plan -> Act
- 自定义 agent 必须继承 `TWAgent`

## 必须遵守的限制（重点）

根据 `group-project.pdf`，以下条款必须严格遵守：

- 只能在自定义 agent 类中重写：
  - `communicate()`
  - `think()`
  - `act()`
- 不得重写 `TWAgent` 的其他方法（例如 `sense()`、`putTileInHole()` 等）
- 不得修改模拟器的 `environment` package
- `TWEnvironment.increaseReward()` 只能在 `TWAgent.putTileInHole()` 内被调用
- 在 `communicate()` 中需要调用 `TWEnvironment.receiveMessage()`，否则消息不会进入环境广播列表

违反上述限制会导致课程评分扣分。

## Agent 实现

### SimpleTWAgent（基线策略）

基于规则的多智能体协作策略：
- Zone-based exploration：按 agent 编号分配搜索区域
- 消息驱动的目标协调：TARGET_CLAIM / TARGET_RELEASE / TARGET_INVALID
- 发现广播：TILE_FOUND / HOLE_FOUND / OBSTACLE_FOUND / FUEL_STATION_FOUND
- 状态广播：LOW_FUEL / STUCK
- TTL 机制清理过期信息和声明

### RLTWAgent（强化学习策略）

在 SimpleTWAgent 基础上引入强化学习优化的 agent：
- 继承 SimpleTWAgent 的通信协调框架
- 扩展的参数调优（更大的 INFO_TTL、更小的 zone penalty 等）
- 与 SimpleTWAgent 共享同一套消息协议，可混合部署

### 通信模块

- `AgentMessageType`：定义 9 种消息类型（发现、声明、状态）
- `AgentMessage`：结构化消息载体，包含坐标、时间戳、优先级、TTL
- 消息通过 `TWEnvironment.receiveMessage()` 广播

## 实验配置（课程说明）

比赛评测使用 3 套配置：

- Configuration 1
  - Grid: `50 x 50`
  - Object creation rate: `Normal(mu=0.2, sigma=0.05)`
  - Lifetime: `100`
- Configuration 2
  - Grid: `80 x 80`
  - Object creation rate: `Normal(mu=2, sigma=0.5)`
  - Lifetime: `30`
- Configuration 3
  - 未公开（大小、分布、生命周期未知）

固定参数：
- Total time steps: `5000`
- Initial fuel: `500`
- 每个配置运行 `10` 次（不同随机种子）

## 依赖与运行要求

- JDK `1.8`
- Java3D `1.5`
- `MASON_14.jar`（不要使用 `MASON_20.jar`，与给定代码不兼容）

## 代码结构

- `src/tileworld/`：入口、参数、GUI（TWGUI / CatGUI）
- `src/tileworld/agent/`：agent 实现（SimpleTWAgent / RLTWAgent）与通信模块
- `src/tileworld/planners/`：A* 路径规划
- `src/tileworld/environment/`：环境与对象（课程限制：不应修改）
- `src/tileworld/exceptions/`：异常定义
- `bin/`：编译输出

## 运行方式

```bash
# 编译
build.bat

# GUI 模式（标准界面）
run_gui.bat

# GUI 模式（CatGUI 猫猫主题界面）
run_gui_cat.bat

# 无头模式
run_headless.bat

# 批量评测
run_eval.ps1
```
