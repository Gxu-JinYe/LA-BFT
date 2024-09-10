package com.ljh.simulator;


public class CalculateDelay {

    double AGG = 30;
    double SHS = 0.3;
    double SHV = 6;
    double RSS = 1;
    double RSV = 0.12;

    int bSize = 50;
    int n = 1000;

    double T_C_N = 30;
    double T_N_N = 12;


    public double calPBFTOneDelay(){
        return T_C_N +
                RSV*bSize + RSS + T_N_N +
                RSV + RSS + T_N_N +
                RSV*n + RSS + T_N_N +
                RSV*n + RSS + T_C_N +
                RSV*((2*n)/3 + 1);
    }

    public double calPBFTConcurrentAvgDelay(int concurrent){
        int round = concurrent % bSize == 0? concurrent/bSize : concurrent/bSize + 1;
        double TotalDelay = 0;
        double T_PBFT = calPBFTOneDelay();
        for(int i = 1; i < round + 1; i++){
            TotalDelay += i * T_PBFT;
        }
        return TotalDelay / concurrent;
    }

    public double calPBFTConcurrentDelay(int concurrent){
        int round = concurrent % bSize == 0? concurrent/bSize : concurrent/bSize + 1;
        double TotalDelay = 0;
        double T_PBFT = calPBFTOneDelay();
        for(int i = 1; i < round + 1; i++){
            TotalDelay += i * T_PBFT;
        }
        return TotalDelay;
    }

    public double calSBFCOneDelay(){
        return T_C_N +
                RSV*bSize + RSS + T_N_N +
                RSV*n + RSV + SHS + T_N_N +
                AGG + T_N_N +
                SHV + SHS + T_N_N +
                AGG + T_N_N +
                SHV + RSS + T_C_N +
                RSV*(n/3 + 1);
    }


    public static void main(String[] args) {
        CalculateDelay calculateDelay = new CalculateDelay();
        System.out.println("SBFC单轮共识时延：" + calculateDelay.calSBFCOneDelay());
        System.out.println("PBFT单轮共识时延：" + calculateDelay.calPBFTOneDelay());

        System.out.println("PBFT并发2000条交易的总时延：" + calculateDelay.calPBFTConcurrentDelay(2000));
    }
}
