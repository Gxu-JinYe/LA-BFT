package com.ljh.simulator.replica;

import com.ljh.simulator.Client;
import com.ljh.simulator.Simulator;
import com.ljh.simulator.message.*;
import com.ljh.simulator.utils.Utils;

import java.util.*;

public class Replica {

    public long freeTime = 0;

    public static final int K = 10;						//发送checkpoint消息的周期

    public static final int L = 30;						//L = 高水位 - 低水位		(一般取L>=K*2)

    public static final int PROCESSING = 0;		//没有收到f+1个reply

    public static final int STABLE = 1;			//已经收到了f+1个reply

    public String receiveTag = "Receive";

    public String sendTag = "Send";

    public int id; 										//当前节点的id

    public int v;										//视图编号

    public int n;										//消息处理序列号

    public int lastRepNum;								//最新回复的消息处理序列号

    public int h;										//低水位 = 稳定状态checkpoint的n

    public int netDlys[];								//与其他节点的网络延迟

    public int netDlyToClis[];							//与客户端的网络延迟

    public boolean isTimeOut;							//当前正在处理的请求是否超时（如果超时了不会再发送任何消息）

    //消息缓存<type, <msg>>:type消息类型;
    public Map<Integer, Set<Message>> msgCache;

    //最新reply的状态集合<c, <c, t, r>>:c客户端编号;t请求消息时间戳;r返回结果
    public Map<Integer, LastReply> lastReplyMap;

    //checkpoints集合<n, <c, <c, t, r>>>:n消息处理序列号
    public Map<Integer, Map<Integer, LastReply>> checkPoints;

    public Map<Message, Integer> reqStats;			//request请求状态

    public static Comparator<PrePrepareMsg> nCmp = new Comparator<PrePrepareMsg>(){
        @Override
        public int compare(PrePrepareMsg c1, PrePrepareMsg c2) {
            return (int) (c1.n - c2.n);
        }
    };

    public Replica(int id, int[] netDlys, int[] netDlyToClis) {
        this.id = id;
        this.netDlys = netDlys;
        this.netDlyToClis = netDlyToClis;
        msgCache = new HashMap<>();
        lastReplyMap = new HashMap<>();
        checkPoints = new HashMap<>();
        reqStats = new HashMap<>();
        checkPoints.put(0, lastReplyMap);
        //初始时启动Timer
    }

    public void msgProcess(Message msg) {
        msg.print(receiveTag);
        switch(msg.type) {
            case Message.REQUEST:
                receiveRequest(msg);
                break;
            case Message.PREPREPARE:
                receivePreprepare(msg);
                break;
            case Message.PREPARE:
                receivePrepare(msg);
                break;
            case Message.COMMIT:
                receiveCommit(msg);
                break;
            default:
                System.out.println("【Error】消息类型错误！");
                return;
        }
    }

    public void receiveRequest(Message msg) {
        if(msg == null) return;
        RequestMsg reqlyMsg = (RequestMsg)msg;

        int c = reqlyMsg.c;
        long t = reqlyMsg.t;
        long recTime = Math.max(msg.rcvtime, freeTime);


        //如果这条请求已经reply过了，那么就再回复一次reply
        if(reqStats.containsKey(msg) && reqStats.get(msg) == STABLE) {
            recTime += msg.rcvtime + netDlyToClis[Client.getCliArrayIndex(c)];
            Message replyMsg = new ReplyMsg(v, t, c, id, "result", id, c, recTime);
            freeTime = recTime;
            Simulator.sendMsg(replyMsg, sendTag);
            return;
        }

        if(!reqStats.containsKey(msg)) {
            //把消息放进缓存
            addMessageToCache(msg);
            reqStats.put(msg, PROCESSING);
        }
        //如果是主节点
        if(isPrimary()) {
            n++;
            Message prePrepareMsg = new PrePrepareMsg(v, n, reqlyMsg, id, id, id, recTime);
            addMessageToCache(prePrepareMsg);
            Simulator.sendMsgToOthers(prePrepareMsg, id, sendTag);
            System.out.println("Leader " + msg.rcvId + " receive REQUEST message from " + msg.sndId + " at time:" + recTime);
        }
        Simulator.max_occupy_bandwidth = Math.max(Simulator.max_occupy_bandwidth, Simulator.inFlyMsgLen);
    }

    public void receivePreprepare(Message msg) {
        PrePrepareMsg prePrepareMsg = (PrePrepareMsg)msg;
        int msgv = prePrepareMsg.v;
        int msgn = prePrepareMsg.n;
        int i = prePrepareMsg.i;
        long recTime = Math.max(msg.rcvtime, freeTime);
        //节点id，需要检查是否是由leader发出
        //检查消息的视图是否与节点视图相符，消息的发送者是否是主节点，
        if(msgv < v || i != msgv % Simulator.RN) {
            return;
        }
        //把prepare消息和其包含的request消息放进缓存
        System.out.println("Node " + id + " receive PREPREPARE message " + prePrepareMsg.n + " from " + prePrepareMsg.sndId + " at time:" + recTime);
        receiveRequest(prePrepareMsg.m);
        addMessageToCache(msg);

        //更新节点当前处理的消息序号
        n = Math.max(n, prePrepareMsg.n);

        //生成Prepare-Vote消息并广播
        String d = Utils.getMD5Digest(prePrepareMsg.mString());        // request摘要
        Message prepareMsg = new PrepareMsg(msgv, msgn, d, id, id, id, recTime + Simulator.RSA_SIG_DELAY);
        freeTime = recTime + Simulator.RSA_SIG_DELAY;
        if(isInMsgCache(prepareMsg)) return;
        addMessageToCache(prepareMsg);
        Simulator.sendMsgToOthers(prepareMsg, id, sendTag);

        Simulator.max_occupy_bandwidth = Math.max(Simulator.max_occupy_bandwidth, Simulator.inFlyMsgLen);
    }

    public void receivePrepare(Message msg) {
        if(isTimeOut) return;
        PrepareMsg prepareMsg = (PrepareMsg)msg;
        long recTime = Math.max(prepareMsg.rcvtime, freeTime);
        System.out.println(id + "receive PREPARE from " + msg.sndId + " at time:" + recTime);

        int msgv = prepareMsg.v;
        int msgn = prepareMsg.n;
        //检查缓存中是否有这条消息，消息的视图是否合法，序号是否在水位内
        if(isInMsgCache(msg) || msgv < v) {
            return;
        }
        freeTime = recTime + Simulator.RSA_VER_DELAY;

        //把prepare消息放进缓存
        addMessageToCache(msg);

        Set<Message> msgs = msgCache.get(prepareMsg.type);
        int cnt = 0;
        for(Message m : msgs){
            PrepareMsg vmsg = (PrepareMsg) m;
            if(vmsg.n == prepareMsg.n && vmsg.v == prepareMsg.v){
                cnt++;
            }
        }
        if(cnt == Math.ceil(Simulator.RN / 3)){
            CommitMsg commitMsg = new CommitMsg(prepareMsg.v, prepareMsg.n, prepareMsg.d,
                    id, id, id, Math.max(prepareMsg.rcvtime, freeTime) + Simulator.RSA_SIG_DELAY);
            // if(isInMsgCache(commitMsg)) return;
            freeTime = Math.max(prepareMsg.rcvtime, freeTime) + Simulator.RSA_SIG_DELAY;
            if(isInMsgCache(commitMsg)) return;  // 增加的语句
            addMessageToCache(commitMsg);
            Simulator.sendMsgToOthers(commitMsg, id, sendTag);
        }

        Simulator.max_occupy_bandwidth = Math.max(Simulator.max_occupy_bandwidth, Simulator.inFlyMsgLen);
    }

    public void receiveCommit(Message msg) {
        if(isTimeOut) return;
        CommitMsg commitMsg = (CommitMsg)msg;
        long recTime = Math.max(freeTime, commitMsg.rcvtime);
        System.out.println(id + "receive COMMIT from " + msg.sndId + " at time:" + msg.rcvtime);

        int msgv = commitMsg.v;
        int msgn = commitMsg.n;
        //检查消息的视图是否合法，序号是否在水位内
        if(isInMsgCache(msg) || msgv < v) {
            return;
        }
        freeTime = recTime + Simulator.RSA_VER_DELAY;
        //把commit消息放进缓存
        addMessageToCache(msg);

        Set<Message> msgs = msgCache.get(commitMsg.type);
        int cnt = 0;
        for(Message m : msgs){
            CommitMsg vmsg = (CommitMsg) m;
            if(vmsg.n == commitMsg.n && vmsg.v == commitMsg.v){
                cnt++;
            }
        }
        if(cnt == Math.ceil(Simulator.RN / 3)){
            Set<Message> ppMsgs = msgCache.get(Message.PREPREPARE);
            int c = 1;
            long t = 0;
            for(Message m : ppMsgs){
                PrePrepareMsg ppmsg = (PrePrepareMsg) m;
                if(ppmsg.n == msgn && ppmsg.v == msgv){
                    c = ((RequestMsg)ppmsg.m).c;
                    t = ((RequestMsg)ppmsg.m).t;
                    break;
                }
            }

            ReplyMsg replyMsg = new ReplyMsg(commitMsg.v, t, c,
                    id, "success", id, c, recTime + Simulator.RSA_SIG_DELAY + netDlyToClis[Client.getCliArrayIndex(c)]);
            if(isInMsgCache(replyMsg)) return;
            addMessageToCache(replyMsg);
            Simulator.sendMsg(replyMsg, sendTag);

        }

        Simulator.max_occupy_bandwidth = Math.max(Simulator.max_occupy_bandwidth, Simulator.inFlyMsgLen);
    }

    public boolean isPrimary() {
        return getPriId() == id;
    }
    public int getPriId() {
        return v % Simulator.RN;
    }
    /**
     * 将消息存到缓存中
     * @param m
     */
    private void addMessageToCache(Message m) {
        Set<Message> msgSet = msgCache.get(m.type);
        if(msgSet == null) {
            msgSet = new HashSet<>();
            msgCache.put(m.type, msgSet);
        }
        msgSet.add(m);
    }

    /**
     * 将消息存到缓存中
     * @param m
     */
    private boolean isInMsgCache(Message m) {
        Set<Message> msgSet = msgCache.get(m.type);
        if(msgSet == null) {
            return false;
        }
        return msgSet.contains(m);
    }
}
