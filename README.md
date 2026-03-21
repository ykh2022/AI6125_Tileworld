# Tileworld (Multi-Agent Project)

这是一个基于 **MASON** 框架的 Tileworld 多智能体仿真项目。  
项目包含环境建模、对象生成与消失、智能体感知/记忆/决策/执行、以及可视化 GUI。

## 项目目标

- 在二维网格环境中运行多个智能体
- 智能体通过感知与动作完成搬运任务（tile -> hole）
- 支持燃料约束、记忆机制和消息广播机制
- 统计整体 reward 作为性能指标

## 项目结构

- `src/tileworld/`：主入口、参数和 GUI
- `src/tileworld/agent/`：智能体、动作、感知、记忆、消息
- `src/tileworld/environment/`：环境、对象、网格逻辑
- `src/tileworld/planners/`：路径与规划相关代码
- `src/tileworld/exceptions/`：异常定义
- `bin/`：已编译 class 文件

## 运行方式

### 1) 图形界面运行

运行主类：

- `tileworld.TWGUI`

### 2) 无界面批量运行

运行主类：

- `tileworld.TileworldMain`

## 依赖

- Java（建议 JDK 8+）
- MASON 库（项目当前配置使用 `MASON_14.jar`）

当前 `.classpath` 中引用路径为：

- `E:/Kun/Study/NTU/6125_Multi Agent/project/MASON_14.jar`

如果你在其他机器运行，需要把该 jar 路径改为本机可用路径（或改成项目内相对路径）。

## 可扩展点

- 自定义 Agent：继承 `TWAgent`
- 自定义 Planner：实现 `tileworld.planners` 下规划逻辑
- 自定义 Memory：扩展 `TWAgentWorkingMemory`
- 自定义 Message：扩展 `Message`，实现团队通信策略

## 当前状态

- 代码可用于课程项目基础仿真与二次开发
- 默认 `SimpleTWAgent` 为示例策略（随机移动）
- 更高 reward 需要实现更强的规划/记忆/通信策略
