package com.ljh.simulator.entity;

import com.ljh.simulator.Simulator;
import com.ljh.simulator.utils.Utils;
import com.ljh.simulator.vo.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Node {

    public static final int PROCESSING = 0;		//没有收到f+1个reply

    public static final int STABLE = 1;			//已经收到了f+1个reply

    public String receiveTag = "Receive";

    public String sendTag = "Send";

    public int id; 										//当前节点的id

    public int v;										//视图编号

    public int n;										//消息处理序列号

    public int lastRepNum;								//最新回复的消息处理序列号

    public int netDlys[];								//与其他节点的网络延迟

    public int netDlyToClis[];							//与客户端的网络延迟

    public boolean isTimeOut;							//当前正在处理的请求是否超时（如果超时了不会再发送任何消息）

    //消息缓存<type, <msg>>:type消息类型;
    public Map<Integer, Set<Message>> msgCache;

    //最新reply的状态集合<c, <c, t, r>>:c客户端编号;t请求消息时间戳;r返回结果
    //public Map<Integer, LastReply> lastReplyMap;

    //checkpoints集合<n, <c, <c, t, r>>>:n消息处理序列号
    //public Map<Integer, Map<Integer, LastReply>> checkPoints;

    //request请求状态
    public Map<Message, Integer> reqStats;

    public Node(int id, int[] netDlys, int[] netDlyToClis) {
        this.id = id;
        this.netDlys = netDlys;
        this.netDlyToClis = netDlyToClis;
        msgCache = new HashMap<>();
        reqStats = new HashMap<>();
        //初始时启动Timer
    }

    public void msgProcess(Message msg) {
        msg.print(receiveTag);
        switch (msg.type) {
            case Message.REQUEST:
                receiveRequest(msg);
                break;
            case Message.PREPARE:
                receivePrepare(msg);
                break;
            case Message.PREPARE_VOTE:
                receivePrepareVote(msg);
                break;
            case Message.PRECOMMIT:
                receivePreCommit(msg);
                break;
            case Message.PRECOMMIT_VOTE:
                receivePreCommitVote(msg);
                break;
            case Message.COMMIT:
                receiveCommit(msg);
                break;
            default:
                System.out.println("【Error】消息类型错误！");
                return;
        }
    }

    public void receiveRequest(Message msg){
        if(msg == null) return;

        RequestMsg reqMsg = (RequestMsg)msg;
        int c = reqMsg.c;
        long t = reqMsg.t;

        //如果这条请求已经reply过了，那么就再回复一次reply
        if(reqStats.containsKey(msg) && reqStats.get(msg) == STABLE) {
            long recTime = msg.rcvtime + netDlyToClis[Client.getCliArrayIndex(c)];
            Message replyMsg = new ReplyMsg(v, t, c, id, "result", id, c, recTime);
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
            Message prePrepareMsg = new PrepareMsg(v, n, reqMsg, id, id, id, reqMsg.rcvtime);
            addMessageToCache(prePrepareMsg);
            Simulator.sendMsgToOthers(prePrepareMsg, id, sendTag);
            System.out.println("Leader " + msg.rcvId + " receive REQUEST message from " + msg.sndId + " at time:" + reqMsg.rcvtime);
        }
    }

    public void receivePrepare(Message msg) {
        PrepareMsg prepareMsg = (PrepareMsg)msg;
        int msgv = prepareMsg.v;
        int msgn = prepareMsg.n;
        int i = prepareMsg.i;
        //节点id，需要检查是否是由leader发出
        //检查消息的视图是否与节点视图相符，消息的发送者是否是主节点，
        if(msgv < v || i != msgv % Simulator.RN) {
            return;
        }
        //把prepare消息和其包含的request消息放进缓存
        System.out.println("Node " + id + " receive PREPARE message " + prepareMsg.n + " from " + prepareMsg.sndId + " at time:" + prepareMsg.rcvtime);
        receiveRequest(prepareMsg.m);
        addMessageToCache(msg);

        //更新节点当前处理的消息序号
        n = Math.max(n, prepareMsg.n);

        //生成Prepare-Vote消息并广播
        String d = Utils.getMD5Digest(prepareMsg.mString());        // request摘要
        String s = id + " sign for prepare message " + prepareMsg.n;    // 签名份额
        Message prepareVoteMsg = new PrepareVoteMsg(msgv, msgn, d, s, id, id, msgv % Simulator.RN, msg.rcvtime + Simulator.THR_SIG_DELAY);
        if(isInMsgCache(prepareVoteMsg)) return;
        addMessageToCache(prepareVoteMsg);
        Simulator.sendMsg(prepareVoteMsg, sendTag, netDlys);
    }

    public void receivePrepareVote(Message msg){
        if(msg == null) return;

        PrepareVoteMsg prepareVoteMsg = (PrepareVoteMsg)msg;
        addMessageToCache(prepareVoteMsg);
        System.out.println("Leader " + id + " receive PrepareVote message " + prepareVoteMsg.n + " from " + prepareVoteMsg.sndId + " at time:" + prepareVoteMsg.rcvtime);

        Set<Message> msgs = msgCache.get(prepareVoteMsg.type);
        int cnt = 0;
        for(Message m : msgs){
            PrepareVoteMsg vmsg = (PrepareVoteMsg) m;
            if(vmsg.n == prepareVoteMsg.n && vmsg.v == prepareVoteMsg.v){
                cnt++;
            }
        }
        if(cnt == Math.ceil(Simulator.RN / 3)){

            String poly = "poly signature";
            PreCommitMsg preCommitMsg = new PreCommitMsg(prepareVoteMsg.v, prepareVoteMsg.n, prepareVoteMsg.d, poly,
                    id, id, id, prepareVoteMsg.rcvtime + Simulator.THR_AGG_DELAY);
            if(isInMsgCache(preCommitMsg)) return;
            addMessageToCache(preCommitMsg);
            Simulator.sendMsgToOthers(preCommitMsg, id, sendTag);
        }
    }

    public void receivePreCommit(Message msg) {
        PreCommitMsg preCommitMsg = (PreCommitMsg) msg;
        int msgv = preCommitMsg.v;
        int msgn = preCommitMsg.n;
        int i = preCommitMsg.i;
        //节点id，需要检查是否是由leader发出
        //检查消息的视图是否与节点视图相符，消息的发送者是否是主节点，
        if(msgv < v || i != msgv % Simulator.RN) {
            return;
        }

        //把PreCommit消息和其包含的request消息放进缓存
        System.out.println("Node " + id + " receive PRECOMMIT message " + preCommitMsg.n + " from " + preCommitMsg.sndId + " at time:" + preCommitMsg.rcvtime);
        addMessageToCache(msg);

        //生成PreCommit_Vote消息并广播
        String s = id + " sign for PreCommit message " + preCommitMsg.n;    // 签名份额
        Message preCommitVoteMsg = new PreCommitVoteMsg(msgv, msgn, preCommitMsg.d, s, id, id, msgv % Simulator.RN, msg.rcvtime + Simulator.THR_VER_DELAY + Simulator.THR_SIG_DELAY);
        //if(isInMsgCache(preCommitVoteMsg)) return;
        addMessageToCache(preCommitVoteMsg);
        Simulator.sendMsg(preCommitVoteMsg, sendTag, netDlys);
    }

    public void receivePreCommitVote(Message msg){
        if(msg == null) return;

        PreCommitVoteMsg preCommitVoteMsg = (PreCommitVoteMsg) msg;
        addMessageToCache(preCommitVoteMsg);

        System.out.println("Leader " + id + " receive PreCommit_Vote message " + preCommitVoteMsg.n + " from " + preCommitVoteMsg.sndId + " at time:" + preCommitVoteMsg.rcvtime);

        Set<Message> msgs = msgCache.get(preCommitVoteMsg.type);
        int cnt = 0;
        for(Message m : msgs){
            PreCommitVoteMsg vmsg = (PreCommitVoteMsg) m;
            if(vmsg.n == preCommitVoteMsg.n && vmsg.v == preCommitVoteMsg.v){
                cnt++;
            }
        }
        if(cnt == Math.ceil(Simulator.RN / 3)){
            String poly = "poly signature";
            CommitMsg commitMsg = new CommitMsg(preCommitVoteMsg.v, preCommitVoteMsg.n, preCommitVoteMsg.d, poly,
                    id, id, id, preCommitVoteMsg.rcvtime + Simulator.THR_AGG_DELAY);
            // if(isInMsgCache(commitMsg)) return;
            addMessageToCache(commitMsg);
            Simulator.sendMsgToOthers(commitMsg, id, sendTag);

        }
    }

    public void receiveCommit(Message msg){
        CommitMsg commitMsg = (CommitMsg) msg;
        int msgv = commitMsg.v;
        int msgn = commitMsg.n;
        int i = commitMsg.i;
        String msgd = commitMsg.d;
        //节点id，需要检查是否是由leader发出
        //检查消息的视图是否与节点视图相符，消息的发送者是否是主节点，
        if(msgv < v || i != msgv % Simulator.RN) {
            return;
        }

        //把COMMIT消息放进缓存
        addMessageToCache(commitMsg);
        System.out.println("Node " + id + " receive COMMIT message " + commitMsg.n + " from " + commitMsg.sndId + " at time:" + commitMsg.rcvtime);

        Set<Message> messages = msgCache.get(Message.PREPARE);
        for(Message m : messages){
            PrepareMsg p = (PrepareMsg) m;
            if(p.v == msgv && p.n == msgn){
                RequestMsg reqMsg = (RequestMsg) p.m;
                //生成REPLY消息给client
                String r = "success";
                ReplyMsg replyMsg = new ReplyMsg(msgv, reqMsg.t, reqMsg.c, id, r, id, Client.getCliId(reqMsg.c), msg.rcvtime);
                Simulator.sendMsg(replyMsg, sendTag, netDlyToClis);
            }
        }

    }
    /**
     * 消息是否存在缓存中
     * @param m
     */
    private boolean isInMsgCache(Message m) {
        Set<Message> msgSet = msgCache.get(m.type);
        if(msgSet == null) {
            return false;
        }
        return msgSet.contains(m);
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

    public boolean isPrimary() {
        return getPriId() == id;
    }

    public int getPriId() {
        return v % Simulator.RN;
    }

}
