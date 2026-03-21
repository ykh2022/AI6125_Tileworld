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

## 建议实现模块

- Planning module（必做）：根据当前感知 + 记忆规划动作
- Memory module（可选）：可扩展 `TWAgentWorkingMemory`
- Communication module（可选）：可扩展 `Message`，并在规划时读取 `TWEnvironment.getMessages()`

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

课程文档要求：
- JDK `1.8`
- Java3D `1.5`
- `MASON_14.jar`（不要使用 `MASON_20.jar`，与给定代码不兼容）

当前项目 `.classpath` 已引用：
- `E:/Kun/Study/NTU/6125_Multi Agent/project/MASON_14.jar`

## 代码结构

- `src/tileworld/`：入口、参数、GUI
- `src/tileworld/agent/`：agent 与感知/记忆/消息
- `src/tileworld/planners/`：规划与路径
- `src/tileworld/environment/`：环境与对象（课程限制：不应修改）
- `src/tileworld/exceptions/`：异常定义
- `bin/`：编译输出

## 运行方式

- GUI：运行 `tileworld.TWGUI`
- Headless：运行 `tileworld.TileworldMain`
