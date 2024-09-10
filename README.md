# Paper citations
- The paper and the complete code have been uploaded. If you use the code for subsequent research, please cite our paper. The paper citation format is as follows:
- Ye J, Hu H, Liang J,  et al. Lightweight adaptive Byzantine fault tolerant consensus algorithm for distributed energy trading[J]. Computer  Networks, 2024, 251: 110635.
## Key Features of the Code (pbftSimulator)
- Implemented the main functions of PBFT (namely LA-BFT), including Request, Preprepare, Prepare, Commit, CheckPoint, ViewChange, NewView, and other stages
- A message priority queue is used to simulate the consensus interaction process between multiple nodes in a single thread, taking into account the network delay between nodes (network delay increases exponentially with the number of messages)
- It is possible to simulate the process of consensus interaction involving honest nodes with Byzantine/straggling nodes
- You can easily adjust system parameters (number of nodes, number of malicious nodes, etc.) and network parameters (network delay policy) to evaluate the performance of PBFT under different parameters
- It is convenient to implement various Byzantine behaviors and simulate the effect of that behavior on the consensus process