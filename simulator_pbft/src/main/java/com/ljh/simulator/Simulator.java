package com.ljh.simulator;

import com.ljh.simulator.message.Message;
import com.ljh.simulator.replica.Replica;
import com.ljh.simulator.utils.Utils;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

public class Simulator {

    public static long max_occupy_bandwidth = 0;

    public static final int RN = 40;  						//replicas节点的数量(rn)

    public static final int FN = 2;							//恶意节点的数量

    public static final int CN = 1;						    //客户端数量

    public static final int INFLIGHT = 2000; 			    //最多同时处理多少请求

    public static final int REQNUM = 5000;					//请求消息总数量

    public static final int TIMEOUT = 500;					//节点超时设定(毫秒)

    public static final int CLITIMEOUT = 800;				//客户端超时设定(毫秒)

    public static final int BASEDLYBTWRP = 12;				//节点之间的基础网络时延

    public static final int DLYRNGBTWRP = 2;				//节点间的网络时延扰动范围

    public static final int BASEDLYBTWRPANDCLI = 30;		//节点与客户端之间的基础网络时延

    public static final int DLYRNGBTWRPANDCLI = 15;			//节点与客户端之间的网络时延扰动范围

    public static final int BANDWIDTH = 300000000;			//节点间网络的额定带宽(bytes)(超过后时延呈指数级上升)

    public static final double FACTOR = 1.005;				//超出额定负载后的指数基数

    public static final int COLLAPSEDELAY = 10000;			//视为系统崩溃的网络时延

    public static final boolean SHOWDETAILINFO = false;		//是否显示完整的消息交互过程

    //消息优先队列（按消息计划被处理的时间戳排序）
    public static Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);
    //正在网络中传播的消息的总大小
    public static long inFlyMsgLen = 0;

    //初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
    public static int[][] netDlys = Utils.netDlyBtwRpInit(RN);

    public static int[][] netDlysToClis = Utils.netDlyBtwRpAndCliInit(RN, CN);

    public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);

    public static final int THR_AGG_DELAY = 10; // 10-30ms 聚签

    public static final int THR_SIG_DELAY = 1;  // 0.3-0.7ms 签名

    public static final int THR_VER_DELAY = 6;  // 5-10ms 验签

    public static final int RSA_SIG_DELAY = 2;  // 平均1ms

    public static final int RSA_VER_DELAY = 2;  // 实际0.12


    public static void main(String[] args) {
        //初始化包含FN个拜占庭意节点的RN个replicas
//		boolean[] byzts = byztDistriInit(RN, FN);
//        boolean[] byzts = {true, false, false, false, false, false, true};
        Replica[] reps = new Replica[RN];
        for(int i = 0; i < RN; i++) {
//            if(byzts[i]) {
//                reps[i] = new Replica(i, netDlys[i], netDlysToClis[i]);
//            }else {
//                reps[i] = new Replica(i, netDlys[i], netDlysToClis[i]);
//            }
            reps[i] = new Replica(i, netDlys[i], netDlysToClis[i]);
        }

        //初始化CN个客户端
        Client[] clis = new Client[CN];
        for(int i = 0; i < CN; i++) {
            //客户端的编号设置为负数
            clis[i] = new Client(Client.getCliId(i), netDlysToNodes[i]);
        }

        // 随机选择客户端发送请求
        Random rand = new Random(555);
        for(int i = 0; i < 500; i++) {
            clis[rand.nextInt(CN)].sendRequest(0);
        }

        //消息处理
        long timestamp = 0;
        while(!msgQue.isEmpty()) {
            Message msg = msgQue.poll();
            switch(msg.type) {
                case Message.REPLY:
                    clis[Client.getCliArrayIndex(msg.rcvId)].msgProcess(msg);
                    break;
                default:
                    reps[msg.rcvId].msgProcess(msg);
            }

            inFlyMsgLen -= msg.len;
            timestamp = msg.rcvtime;
            if(getNetDelay(inFlyMsgLen, 0) > COLLAPSEDELAY ) {
                System.out.println("【Error】网络消息总负载"+inFlyMsgLen
                        +"B,网络传播时延超过"+COLLAPSEDELAY/1000
                        +"秒，系统已严重拥堵，不可用！");
                break;
            }
        }

        long totalTime = 0;
        long totalStableMsg = 0;
        for(int i = 0; i < CN; i++) {
            totalTime += clis[i].accTime;
            totalStableMsg += clis[i].stableMsgNum();
        }
        double tps = getStableRequestNum(clis)/((double) timestamp/1000);
        System.out.println("【The end】消息平均确认时间为:"+totalTime/totalStableMsg
                +"毫秒;消息吞吐量为:"+tps+"tps");
        System.out.println("最大占用带宽:" + max_occupy_bandwidth + "B");

    }

    public static int getStableRequestNum(Client[] clis) {
        int num = 0;
        for(int i = 0; i < clis.length; i++) {
            num += clis[i].stableMsgNum();
        }
        return num;
    }


    public static void sendMsg(Message msg, String tag) {
        msg.print(tag);
        msgQue.add(msg);
        inFlyMsgLen += msg.len;
    }

    public static int getNetDelay(long inFlyMsgLen, int basedelay) {
        if(inFlyMsgLen < BANDWIDTH) {
            return basedelay;
        }else {
            return (int)Math.pow(FACTOR, inFlyMsgLen - BANDWIDTH) + basedelay;
        }
    }

    public static void sendMsgToOthers(Message msg, int id, String tag) {
        for(int i = 0; i < RN; i++) {
            if(i != id) {
                Message m = msg.copy(i, msg.rcvtime + netDlys[id][i]);
                sendMsg(m, tag);
            }
        }
    }
}
