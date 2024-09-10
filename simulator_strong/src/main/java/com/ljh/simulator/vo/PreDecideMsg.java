package com.ljh.simulator.vo;

public class PreDecideMsg extends Message{

    public int v;

    public int n;

    public int c;

    public int[] is;


    public PreDecideMsg(int v, int n, int c, int[] is, int sndId, int rcvId, double rcvtime) {
        super(sndId, rcvId, rcvtime);
        this.type = PREDECIDE;
        this.len = PDCMSGLEN;
        this.v = v;
        this.n = n;
        this.c = c;
    }
}
