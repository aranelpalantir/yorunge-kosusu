package com.yorunge.runner;

import java.util.Arrays;
import java.util.PriorityQueue;

/** Edge-connected red tiles collapse in a delayed wave, including the tunnel seam. */
public final class Collapse {
    public static final double ESCAPE_SECONDS=.24;
    private final int[][] tiles;
    private final double[][] times;
    public final int level;
    public Collapse(int level){
        this.level=level;tiles=new int[Track.length(level)][8];
        for(int r=0;r<tiles.length;r++)for(int l=0;l<8;l++)tiles[r][l]=Track.tile(level,r,l);
        times=emptyTimes(tiles.length);
    }
    /** Small hand-authored boards can exercise propagation independently of course generation. */
    public Collapse(int[][] board){level=-1;tiles=new int[board.length][8];for(int r=0;r<board.length;r++)tiles[r]=board[r].clone();times=emptyTimes(board.length);}
    private static double[][] emptyTimes(int rows){double[][] t=new double[rows][8];for(double[] row:t)Arrays.fill(row,Double.POSITIVE_INFINITY);return t;}
    private static final class Node implements Comparable<Node>{
        final int row,lane;final double time;
        Node(int r,int l,double t){row=r;lane=l;time=t;}
        public int compareTo(Node other){return Double.compare(time,other.time);}
    }
    private boolean red(int row,int lane){return row>=0 && row<tiles.length && tiles[row][Track.wrap(lane)]==Track.RED;}
    public boolean trigger(int row,int lane,double now){
        lane=Track.wrap(lane);
        if(!red(row,lane) || times[row][lane]<=now)return false;
        PriorityQueue<Node> queue=new PriorityQueue<>();times[row][lane]=now;queue.add(new Node(row,lane,now));
        while(!queue.isEmpty()){
            Node n=queue.poll();if(n.time>times[n.row][n.lane])continue;
            int[] rows={n.row-1,n.row+1,n.row,n.row},lanes={n.lane,n.lane,n.lane-1,n.lane+1};
            for(int i=0;i<4;i++){
                int r=rows[i],l=Track.wrap(lanes[i]);if(!red(r,l))continue;
                double arrival=n.time+.11+((r*17+l*13+Math.max(level,0))%7)*.01;
                if(arrival<times[r][l]){times[r][l]=arrival;queue.add(new Node(r,l,arrival));}
            }
        }
        return true;
    }
    public double age(int row,int lane,double now){if(!red(row,lane))return -1;double t=now-times[row][Track.wrap(lane)];return t<0?-1:t;}
    public boolean canEscape(int row,int lane,double now){double age=age(row,lane,now);return age>=0 && age<ESCAPE_SECONDS;}
    public static double drop(double age){return age<=0?0:age*age*600;}
}
