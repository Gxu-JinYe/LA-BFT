package com.ljh.simulator.vo;

public class PreCommitVoteMsg extends Message{

    public int v;

    public int n;

    public String d;

    public String s;

    public int i;

    //消息结构
    //<PRECOMMIT_VOTE, v, n, d, s, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;s表示签名份额;i表示节点id
    public PreCommitVoteMsg(int v, int n, String d, String s, int i, int sndId, int rcvId, double rcvtime) {
        super(sndId, rcvId, rcvtime);
        this.type = PRECOMMIT_VOTE;
        this.len = PCVMSGLEN;
        this.v = v;
        this.n = n;
        this.d = d;
        this.s = s;
        this.i = i;
    }

    public Message copy(int rcvId, double rcvtime) {
        //m是浅复制，不过没有关系，不会修改它的值
        return new PreCommitVoteMsg(v, n, d, s, i, sndId, rcvId, rcvtime);
    }

    public boolean equals(Object obj) {
        if (obj instanceof PreCommitVoteMsg) {
            PreCommitVoteMsg msg = (PreCommitVoteMsg) obj;
            return (v == msg.v && n == msg.n && d.equals(msg.d) && s.equals(msg.s) && i == msg.i);
        }
        return super.equals(obj);
    }

    public int hashCode() {
        String str = "" + v + n + d + s + i;
        return str.hashCode();
    }

    public String toString() {
        return super.toString() + "视图编号:"+v+";序列号:"+n;
    }
}
