package com.yorunge.runner;

import java.util.Arrays;
import java.util.Random;

/** Non-repeating seeded procedural endless tunnel generator with dynamic encounters, boosts, shields, and collapsing red plates. */
public final class Endless {
    private static final int WINDOW=160;
    private final Random rng;
    private final int[][] tiles=new int[WINDOW][8];
    private final double[][] collapseTimes=new double[WINDOW][8];
    private final boolean[] shieldRow=new boolean[WINDOW];
    private final int[] shieldLane=new int[WINDOW];
    private final int[] crystalLane=new int[WINDOW];
    private final boolean[] patrolRow=new boolean[WINDOW];
    private final int[] patrolLane=new int[WINDOW];
    private int generatedUpTo=-1;
    private int cursor=0,currentRoute=0,encounterCount=0;
    private static final int[] PALETTES={0xff255e73,0xff594b83,0xff345b99,0xff865332,0xff326c56,0xff665e87,0xff786141,0xff2d6a6e};

    public Endless(long seed){
        rng=new Random(seed^0x9E3779B97F4A7C15L);
        for(double[] r:collapseTimes)Arrays.fill(r,Double.POSITIVE_INFINITY);
        ensure(60);
    }
    private void clearRow(int r){
        int idx=r%WINDOW;
        Arrays.fill(tiles[idx],Track.FLOOR);
        Arrays.fill(collapseTimes[idx],Double.POSITIVE_INFINITY);
        shieldRow[idx]=false;
        crystalLane[idx]=-1;
        patrolRow[idx]=false;
    }
    public void ensure(int targetRow){
        while(generatedUpTo<targetRow){
            if(cursor==0){
                for(int r=0;r<6;r++){
                    clearRow(r);
                    if(r==2)Arrays.fill(tiles[r%WINDOW],Track.WALL);
                }
                generatedUpTo=5;cursor=6;
            }else{
                generateEncounter();
            }
        }
    }
    private void generateEncounter(){
        encounterCount++;
        int start=cursor;
        boolean ringFirst=rng.nextBoolean();
        int ring,dangerStart,dangerEnd;
        if(ringFirst){
            ring=cursor+2+rng.nextInt(4);
            dangerStart=ring+1;
            dangerEnd=ring+4+rng.nextInt(4);
        }else{
            dangerStart=cursor+3+rng.nextInt(3);
            dangerEnd=dangerStart+3+rng.nextInt(4);
            ring=dangerEnd+2+rng.nextInt(2);
        }
        int squeeze=Math.max(ring,dangerEnd);
        int end=squeeze+5+rng.nextInt(4);
        int previous=currentRoute;
        int move=1+rng.nextInt(3);
        currentRoute=Track.wrap(currentRoute+(rng.nextBoolean()?move:-move));
        int width=encounterCount>=12 && rng.nextInt(3)>0?1:2;
        for(int r=start;r<end;r++)clearRow(r);
        for(int lane=0;lane<8;lane++)tiles[ring%WINDOW][lane]=rng.nextInt(3)==0?Track.RED:Track.HOLE;
        for(int r=dangerStart;r<=dangerEnd;r++){
            int idx=r%WINDOW;
            for(int lane=0;lane<8;lane++){
                if(Track.wrap(lane-currentRoute)>=width)tiles[idx][lane]=rng.nextBoolean()?Track.HOLE:Track.RED;
            }
        }
        for(int r=start;r<ring-1;r++){
            int idx=r%WINDOW;
            for(int lane=0;lane<8;lane++){
                if(Track.wrap(lane-currentRoute)>2 && Track.wrap(currentRoute-lane)>2 && Track.wrap(lane-previous)>2 && Track.wrap(previous-lane)>2 && rng.nextInt(3)==0){
                    tiles[idx][lane]=rng.nextBoolean()?Track.HOLE:Track.RED;
                }
            }
        }
        if(ring-2>start && rng.nextInt(5)<3){
            int bLane=Track.wrap(currentRoute+(rng.nextBoolean()?1:-1));
            if(tiles[(ring-2)%WINDOW][bLane]==Track.FLOOR)tiles[(ring-2)%WINDOW][bLane]=Track.BOOST;
        }
        for(int r=dangerStart+1;r<=Math.min(dangerEnd,dangerStart+3);r++){
            if(tiles[r%WINDOW][currentRoute]==Track.FLOOR)crystalLane[r%WINDOW]=currentRoute;
        }
        if(encounterCount%4==2 && squeeze+2<end-1){
            int sRow=squeeze+2,sLane=Track.wrap(currentRoute+1);
            if(tiles[sRow%WINDOW][sLane]==Track.FLOOR){
                shieldRow[sRow%WINDOW]=true;shieldLane[sRow%WINDOW]=sLane;
            }
        }
        if(encounterCount>=4 && rng.nextInt(3)==0 && squeeze+3<end-1){
            int pRow=squeeze+2;
            patrolRow[pRow%WINDOW]=true;patrolLane[pRow%WINDOW]=currentRoute;
        }
        generatedUpTo=end-1;
        cursor=end;
    }
    public int tile(int row,int lane){
        if(row<0)return Track.FLOOR;
        ensure(row+32);
        return tiles[row%WINDOW][Track.wrap(lane)];
    }
    public int color(int row){
        int sector=Math.max(0,row/28);
        return PALETTES[(sector*3+sector/2)%PALETTES.length];
    }
    public double speed(double distance){
        double eqLevel=Math.min(60,distance/45.0);
        return 9.0+eqLevel*.32;
    }
    public boolean hasShield(int row,int lane){
        if(row<0)return false;
        ensure(row+8);
        int idx=row%WINDOW;
        return shieldRow[idx] && shieldLane[idx]==Track.wrap(lane);
    }
    public void consumeShield(int row){
        if(row>=0)shieldRow[row%WINDOW]=false;
    }
    public boolean hasCrystal(int row,int lane){
        if(row<0)return false;
        ensure(row+8);
        return crystalLane[row%WINDOW]==Track.wrap(lane);
    }
    public void consumeCrystal(int row){
        if(row>=0)crystalLane[row%WINDOW]=-1;
    }
    public boolean hasPatrol(int row){
        if(row<0)return false;
        ensure(row+8);
        return patrolRow[row%WINDOW];
    }
    public double patrolLane(int row,double elapsed){
        int base=patrolLane[row%WINDOW];
        return base+Math.sin(elapsed*2.4+row*.3)*1.35;
    }
    public boolean triggerCollapse(int row,int lane,double now){
        if(row<0)return false;
        ensure(row+12);
        lane=Track.wrap(lane);
        if(tile(row,lane)!=Track.RED || collapseTimes[row%WINDOW][lane]<=now)return false;
        collapseTimes[row%WINDOW][lane]=now;
        for(int dr=-2;dr<=2;dr++)for(int dl=-2;dl<=2;dl++){
            int dist=Math.abs(dr)+Math.abs(dl);
            if(dist==0 || dist>2)continue;
            int r=row+dr,l=Track.wrap(lane+dl);
            if(r>=0 && tile(r,l)==Track.RED){
                double arrival=now+dist*.13+((r*17+l*13)%5)*.01;
                if(arrival<collapseTimes[r%WINDOW][l])collapseTimes[r%WINDOW][l]=arrival;
            }
        }
        return true;
    }
    public double collapseAge(int row,int lane,double now){
        if(row<0 || tile(row,lane)!=Track.RED)return -1;
        double t=now-collapseTimes[row%WINDOW][Track.wrap(lane)];
        return t<0?-1:t;
    }
    public boolean canEscape(int row,int lane,double now){
        double age=collapseAge(row,lane,now);
        return age>=0 && age<Collapse.ESCAPE_SECONDS;
    }
}
