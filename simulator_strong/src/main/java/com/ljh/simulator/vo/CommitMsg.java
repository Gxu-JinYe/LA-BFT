package com.ljh.simulator.vo;

public class CommitMsg extends Message {

    public int v;

    public int n;

    public String d;

    public String poly;

    public int i;

    //消息结构
    //<COMMIT, v, n, d, poly, i>:v表示视图编号;n表示序列号;d表示request消息的摘要;poly表示针对d的聚合签名;i表示节点id
    public CommitMsg(int v, int n, String d, String poly, int i, int sndId, int rcvId, double rcvtime) {
        super(sndId, rcvId, rcvtime);
        this.type = COMMIT;
        this.len = COMMSGLEN;
        this.v = v;
        this.n = n;
        this.d = d;
        this.poly = poly;
        this.i = i;
    }

    public Message copy(int rcvId, double rcvtime) {
        return new CommitMsg(v, n, new String(d), new String(poly), i, sndId, rcvId, rcvtime);
    }

    public boolean equals(Object obj) {
        if (obj instanceof PreCommitMsg) {
            PreCommitMsg msg = (PreCommitMsg) obj;
            return (v == msg.v && n == msg.n && d.equals(msg.d) && poly.equals(msg.poly) && i == msg.i);
        }
        return super.equals(obj);
    }

    public int hashCode() {
        String str = "" + v + n + d + poly + i;
        return str.hashCode();
    }

    public String toString() {
        return super.toString() + "视图编号:"+v+";序列号:"+n;
    }

}
