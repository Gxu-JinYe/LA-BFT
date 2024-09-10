package com.ljh.simulator.vo;

public class PrepareVoteMsg extends Message{
    public int v;

    public int n;

    public String d;

    public String s;

    public int i;

    //消息结构
    //<PREPARE_VOTE, v, n, d, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;s表示对request消息的签名份额;i表示节点id
    public PrepareVoteMsg(int v, int n, String d, String s, int i, int sndId, int rcvId, double rcvtime) {
        super(sndId, rcvId, rcvtime);
        this.type = PREPARE_VOTE;
        this.len = PRVMSGLEN;
        this.v = v;
        this.n = n;
        this.d = d;
        this.s = s;
        this.i = i;
    }

    public Message copy(int rcvId, double rcvtime) {
        return new PrepareVoteMsg(v, n, new String(d), new String(s), i, sndId, rcvId, rcvtime);
    }

    public boolean equals(Object obj) {
        if (obj instanceof PrepareVoteMsg) {
            PrepareVoteMsg msg = (PrepareVoteMsg) obj;
            return (v == msg.v && n == msg.n && d.equals(msg.d) && i == msg.i);
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
