package com.ljh.simulator;

import com.ljh.simulator.entity.Client;
import com.ljh.simulator.entity.Node;
import com.ljh.simulator.utils.Utils;
import com.ljh.simulator.vo.Message;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

public class Simulator {

    public static final int RN = 7;  						//replicas节点的数量(rn)

    public static final int FN = 2;							//恶意节点的数量

    public static final int CN = 3;						    //客户端数量

    public static final int INFLIGHT = 2000; 			    //最多同时处理多少请求

    public static final int REQNUM = 5000;					//请求消息总数量

    public static final int TIMEOUT = 500;					//节点超时设定(毫秒)

    public static final int CLITIMEOUT = 800;				//客户端超时设定(毫秒)

    public static final int BASEDLYBTWRP = 2;				//节点之间的基础网络时延

    public static final int DLYRNGBTWRP = 1;				//节点间的网络时延扰动范围

    public static final int BASEDLYBTWRPANDCLI = 10;		//节点与客户端之间的基础网络时延

    public static final int DLYRNGBTWRPANDCLI = 15;			//节点与客户端之间的网络时延扰动范围

    public static final int BANDWIDTH = 300000;			    //节点间网络的额定带宽(bytes)(超过后时延呈指数级上升)

    public static final double FACTOR = 1.005;				//超出额定负载后的指数基数

    public static final int COLLAPSEDELAY = 10000;			//视为系统崩溃的网络时延

    public static final boolean SHOWDETAILINFO = false;		//是否显示完整的消息交互过程

    //消息优先队列（按消息计划被处理的时间戳排序）
    public static Queue<Message> msgQue = new PriorityQueue<>(Message.cmp);
    //正在网络中传播的消息的总大小
    public static long inFlyMsgLen = 0;

    //初始化节点之间的基础网络时延以及节点与客户端之间的基础网络时延
    public static int[][] netDlys = netDlyBtwRpInit(RN);

    public static int[][] netDlysToClis = netDlyBtwRpAndCliInit(RN, CN);

    public static int[][] netDlysToNodes = Utils.flipMatrix(netDlysToClis);

    public static final int THR_AGG_DELAY = 30;

    public static final int THR_SIG_DELAY = 1;  // 实际0.3

    public static final int THR_VER_DELAY = 6;

    public static final int RSA_SIG_DELAY = 1;

    public static final int RSA_VER_DELAY = 1;  // 实际0.12

    public static void main(String[] args) {
        // 初始化RN个节点信息
        Node[] nodes = new Node[RN];
        for(int i = 0; i < RN; i++) {
            nodes[i] = new Node(i, netDlys[i], netDlysToClis[i]);
        }

        //初始化CN个客户端
        Client[] clis = new Client[CN];
        for(int i = 0; i < CN; i++) {
            //客户端的编号设置为负数
            clis[i] = new Client(Client.getCliId(i), netDlysToNodes[i]);
        }

        Random rand = new Random(555);
        int requestNums = 0;
        for(int i = 0; i < 2; i++) {
            // 随机选客户端发送消息
            clis[rand.nextInt(CN)].sendRequest(0);
            requestNums++;
        }

        long timestamp = 0;
        while(!msgQue.isEmpty()) {
            Message msg = msgQue.poll();
            switch (msg.type){
                case Message.REPLY:
                    clis[msg.rcvId].msgProcess(msg);
                    break;
                default:
                    nodes[msg.rcvId].msgProcess(msg);
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

    }

    /**
     * 随机初始化replicas节点之间的基础网络传输延迟
     * @param n 表示节点总数
     * @return	返回节点之间的基础网络传输延迟数组
     */
    public static int[][] netDlyBtwRpInit(int n){
        int[][] ltcs = new int[n][n];
        Random rand = new Random(999);
        for(int i = 0; i < n; ++i)
            for(int j = 0; j < n; ++j)
                if(i < j && ltcs[i][j] == 0) {
                    ltcs[i][j] = BASEDLYBTWRP + rand.nextInt(DLYRNGBTWRP);
                    ltcs[j][i] = ltcs[i][j];
                }
        return ltcs;
    }

    /**
     * 随机初始化客户端与各节点之间的基础网络传输延迟
     * @param n 表示节点数量
     * @param m 表示客户端数量
     * @return 返回客户端与各节点之间的基础网络传输延迟
     */
    public static int[][] netDlyBtwRpAndCliInit(int n, int m){
        int[][] ltcs = new int[n][m];
        Random rand = new Random(666);
        for(int i = 0; i < n; i++)
            for(int j = 0; j < m; j++)
                ltcs[i][j] = BASEDLYBTWRPANDCLI + rand.nextInt(DLYRNGBTWRPANDCLI);
        return ltcs;
    }

    /**
     * 随机初始化replicas节点的拜占庭标签
     * @param n	节点数量
     * @param f	拜占庭节点数量
     * @return	返回拜占庭标签数组（true为拜占庭节点，false为诚实节点）
     */
    public static boolean[] byztDistriInit(int n, int f) {
        boolean[] byzt = new boolean[n];
        Random rand = new Random(111);
        while(f > 0) {
            int i = rand.nextInt(n);
            if(!byzt[i]) {
                byzt[i] = true;
                --f;
            }
        }
        return byzt;
    }

    public static void sendMsg(Message msg, String tag) {
        msg.print(tag);
        msgQue.add(msg);
        inFlyMsgLen += msg.len;
    }

    public static void sendMsg(Message msg, String tag, int[] netDlys) {
        msg.print(tag);
        msg.rcvtime += netDlys[msg.rcvId];
        msgQue.add(msg);
        inFlyMsgLen += msg.len;
    }

    public static void sendMsgToOthers(Message msg, int id, String tag) {
        for(int i = 0; i < RN; i++) {
            if(i != id) {
                Message m = msg.copy(i, msg.rcvtime + netDlys[id][i]);
                sendMsg(m, tag);
            }
        }
    }

    public static void sendMsgToOthers(Set<Message> msgSet, int id, String tag) {
        if(msgSet == null) {
            return;
        }
        for(Message msg : msgSet) {
            sendMsgToOthers(msg, id, tag);
        }
    }

    public static int getNetDelay(long inFlyMsgLen, int basedelay) {
        if(inFlyMsgLen < BANDWIDTH) {
            return basedelay;
        }else {
            return (int)Math.pow(FACTOR, inFlyMsgLen - BANDWIDTH) + basedelay;
        }
    }

    public static int getStableRequestNum(Client[] clis) {
        int num = 0;
        for(int i = 0; i < clis.length; i++) {
            num += clis[i].stableMsgNum();
        }
        return num;
    }

}
