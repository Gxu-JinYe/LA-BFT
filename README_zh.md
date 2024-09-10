# 论文引用
- 论文正文与完整代码已上传，如利用代码进行后续研究，请引用我们的论文，论文引用格式如下：
- Ye J, Hu H, Liang J, et al. Lightweight adaptive Byzantine fault tolerant consensus algorithm for distributed energy trading[J]. Computer Networks, 2024, 251: 110635.
## 代码主要功能（pbftSimulator）
- 实现了PBFT（即LA-BFT）的主要功能，包括Request、Preprepare、Prepare、Commit、CheckPoint、ViewChange、NewView等各阶段
- 采用消息优先队列用单线程模拟多个节点之间的共识交互过程，考虑了节点之间的网络延迟（网络延迟随消息的数量增长而指数增加）
- 可以模拟包含诚实节点与拜占庭节点/掉线节点的共识交互的过程
- 可以方便地调整系统参数（节点数量，恶意节点数量等）、网络参数（网络延迟策略）以测评不同参数下PBFT的性能变化
- 可以方便地实现各种拜占庭行为，并模拟该行为对共识过程的影响