package com.ljh.simulator;
import javax.sound.midi.Soundbank;
import java.util.*;

public class Solution {

    public static long[] getDistance(int[] type){
        HashMap<Integer, HashSet<Integer>> mp = new HashMap<>();
        for(int i = 0; i < type.length; i++){
            if(!mp.containsKey(type[i])){
                HashSet<Integer> set = new HashSet<>();
                set.add(i);
                mp.put(type[i], set);
            }else{
                HashSet<Integer> set = mp.get(type[i]);
                set.add(i);
            }
        }
        long[] distances = new long[type.length];
        for(int i = 0; i < type.length; i++){
            long distance = 0;
            HashSet<Integer> set = mp.get(type[i]);
            for(int idx : set){
                distance += Math.abs(i - idx);
            }
            distances[i] = distance;
        }
        return distances;

    }

    public static void main(String args[]){
        long[] distances = getDistance(new int[]{2, 2, 1, 3, 1, 3, 2});
        for(long dis : distances){
            System.out.print(dis + " ");
        }
    }
}
