package com.ljh.simulator.vo;

import com.ljh.simulator.Simulator;

import java.util.Comparator;

public class Message {

    public static final int REQUEST = 0;

    public static final int PREPARE = 1;

    public static final int PREPARE_VOTE = 2;

    public static final int PRECOMMIT = 3;

    public static final int PRECOMMIT_VOTE = 4;

    public static final int COMMIT = 5;

    public static final int REPLY = 6;

    public static final int SUBMIT = 7;

    public static final int PREDECIDE = 8;

    public static final int DECIDE = 9;

    public static final int RESPONSE = 10;

    public static final int VIEWCHANGE = 7;

    public static final int NEWVIEW = 8;

    public static final long REQMSGLEN = 100;				//Request消息的大小(bytes),可按实际情况设置

    public static final long PREMSGLEN = 4 + REQMSGLEN;		//Prepare消息的大小

    public static final long PRVMSGLEN = 36;				//Prepare-Vote消息的大小

    public static final long PPCMSGLEN = 36;                //PreCommit消息的大小

    public static final long PCVMSGLEN = 36;                //PreCommit-Vote消息的大小

    public static final long COMMSGLEN = 36;                //Commit消息的大小

    public static final long REPMSGLEN = 16;				//Reply消息的大小

    public static final long SUBMSGLEN = 4 + REQMSGLEN;

    public static final long PDCMSGLEN = 36;

    public static final long DECMSGLEN = 36;

    public static final long RESMSGLEN = 36;

    public static final long VCHMSGBASELEN = 4;				//ViewChange消息的基础大小

    public static final long NEVMSGBASELEN = 3;				//NewView消息的基础大小

    public int type;				//消息类型

    public int sndId;				//消息发送端id

    public int rcvId;  				//消息接收端id

    public double rcvtime;  			//消息接收时间

    public long len;				//消息大小

    public static Comparator<Message> cmp = new Comparator<Message>(){
        public int compare(Message c1, Message c2) {
            return (int) (c1.rcvtime - c2.rcvtime);
        }
    };

    public Message(int sndId, int rcvId, double rcvtime) {
        this.sndId = sndId;
        this.rcvId = rcvId;
        this.rcvtime = rcvtime;
    }

    public void print(String tag) {
        if(!Simulator.SHOWDETAILINFO) return;
        String prefix = "【"+tag+"】";
        System.out.println(prefix+toString());
    }

    public Message copy(int rcvId, double rcvtime) {
        return new Message(sndId, rcvId, rcvtime);
    }

    public int hashCode() {
        String str = "" + type + sndId + rcvId + rcvtime;
        return str.hashCode();
    }

    public String toString() {
        String[] typeName = {"Request","Prepare","Prepare-Vote","PreCommit","PreCommit-Vote","Commit","Reply"
                ,"ViewChange","NewView"};
        return "消息类型:"+typeName[type]+";发送者id:"
                +sndId+";接收者id:"+rcvId+";消息接收时间戳:"+rcvtime+";";
    }
}
