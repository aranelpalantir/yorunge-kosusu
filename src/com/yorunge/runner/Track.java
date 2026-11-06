package com.yorunge.runner;

import java.util.Arrays;
import java.util.Random;

/** Fixed seeded courses: irregular encounter spacing, no repeating row cycle. */
public final class Track {
    public static final int SIDES=8, LEVELS=50;
    public static final int FLOOR=0, HOLE=1, RED=2, WALL=3, BOOST=4;
    public static final double JUMP_SECONDS=.92;
    private static final Course[] COURSES=new Course[LEVELS];
    private static final class Course {
        int length,color,shieldRow=-1,shieldLane=-1;int[] start,end,ring,squeeze,route,width,dangerStart,dangerEnd,crystalLane;int[][] tiles;
        Course(int level){
            Random random=new Random(0x5931acL+level*7919L);
            int count=4+level/10;
            start=new int[count];end=new int[count];ring=new int[count];squeeze=new int[count];route=new int[count];width=new int[count];dangerStart=new int[count];dangerEnd=new int[count];
            int cursor=6,current=0;
            for(int s=0;s<count;s++){
                start[s]=cursor;
                // Uneven breathing room, jump timing, danger stretches and recovery lengths.
                if(random.nextBoolean()){
                    ring[s]=cursor+2+random.nextInt(4);
                    dangerStart[s]=ring[s]+1;
                    dangerEnd[s]=ring[s]+4+random.nextInt(3);
                }else{
                    dangerStart[s]=cursor+3+random.nextInt(3);
                    dangerEnd[s]=dangerStart[s]+3+random.nextInt(3);
                    ring[s]=dangerEnd[s]+2+random.nextInt(2);
                }
                squeeze[s]=Math.max(ring[s],dangerEnd[s]);
                end[s]=squeeze[s]+5+random.nextInt(4);
                int move=1+random.nextInt(3);current=wrap(current+(random.nextBoolean()?move:-move));route[s]=current;
                width[s]=level>=35?1:2;
                cursor=end[s];
            }
            length=cursor+3;
            tiles=new int[length][SIDES];
            crystalLane=new int[length];
            Arrays.fill(crystalLane,-1);
            if(level==0)for(int lane=0;lane<SIDES;lane++)tiles[2][lane]=WALL;
            for(int s=0;s<count;s++){
                for(int lane=0;lane<SIDES;lane++)tiles[ring[s]][lane]=random.nextInt(3)==0?RED:HOLE;
                for(int row=dangerStart[s];row<=dangerEnd[s];row++)for(int lane=0;lane<SIDES;lane++){
                    if(wrap(lane-route[s])>=width[s])tiles[row][lane]=random.nextBoolean()?HOLE:RED;
                }
                // Scattered damage in the approach, away from both the entry and landing paths.
                for(int row=start[s];row<ring[s]-1;row++)for(int lane=0;lane<SIDES;lane++){
                    int previous=s==0?0:route[s-1];
                    if(wrap(lane-route[s])>2 && wrap(route[s]-lane)>2 && wrap(lane-previous)>2 && wrap(previous-lane)>2 && random.nextInt(3)==0)tiles[row][lane]=random.nextBoolean()?HOLE:RED;
                }
                if(hasEnemy(level,s)){
                    tiles[squeeze[s]+1][route[s]]=HOLE;
                    tiles[end[s]-2][route[s]]=HOLE;
                }
                if(level>=2 && (s+level)%2==0 && ring[s]-2>start[s]){
                    int bLane=wrap(route[s]+(s%2==0?1:-1));
                    if(tiles[ring[s]-2][bLane]==FLOOR)tiles[ring[s]-2][bLane]=BOOST;
                }
                for(int r=dangerStart[s]+1;r<=Math.min(dangerEnd[s],dangerStart[s]+3);r++){
                    if(r<length && tiles[r][route[s]]==FLOOR)crystalLane[r]=route[s];
                }
            }
            if(level>=4 && count>1){
                int mid=count/2;
                int sRow=Math.min(length-4,squeeze[mid]+2);
                int sLane=wrap(route[mid]+1);
                if(tiles[sRow][sLane]==FLOOR){shieldRow=sRow;shieldLane=sLane;}
            }
            // A different muted material color for every course, rather than a five-color cycle.
            int[] bases={0x225b70,0x594b83,0x345b99,0x865332,0x326c56,0x665e87,0x786141};
            int base=bases[random.nextInt(bases.length)];
            int r=Math.min(155,((base>>16)&255)+random.nextInt(17));
            int g=Math.min(155,((base>>8)&255)+random.nextInt(17));
            int b=Math.min(165,(base&255)+random.nextInt(17));
            color=0xff000000|(r<<16)|(g<<8)|b;
        }
    }
    static {for(int level=0;level<LEVELS;level++)COURSES[level]=new Course(level);}
    public static boolean hasEnemy(int level,int section){return level>=20 && (section==0 || (level*7+section*11)%5<(level>=35?4:2));}
    public static int length(int level){return COURSES[level].length;}
    public static int sectionCount(int level){return COURSES[level].start.length;}
    public static int sectionStart(int level,int section){return COURSES[level].start[section];}
    public static int sectionEnd(int level,int section){return COURSES[level].end[section];}
    public static int ringRow(int level,int section){return COURSES[level].ring[section];}
    public static int squeezeEnd(int level,int section){return COURSES[level].squeeze[section];}
    public static int sectionAt(int level,int row){int section=0;while(section+1<sectionCount(level) && row>=sectionEnd(level,section))section++;return section;}
    public static int route(int level,int section){return COURSES[level].route[section];}
    public static int shieldRow(int level){return COURSES[level].shieldRow;}
    public static int shieldLane(int level){return COURSES[level].shieldLane;}
    public static int crystalLane(int level,int row){return row<0 || row>=length(level)?-1:COURSES[level].crystalLane[row];}
    public static int color(int level){return COURSES[level].color;}
    public static double speed(int level){return 8.8+(level/4)*.4;}
    public static int wrap(int lane){return (lane%SIDES+SIDES)%SIDES;}
    public static String name(int level){return Story.name(level);}
    public static int tile(int level,int row,int lane){return row<0 || row>=length(level)?FLOOR:COURSES[level].tiles[row][wrap(lane)];}
    public static double jumpHeight(double air){return air<=0?0:Math.sin((JUMP_SECONDS-air)/JUMP_SECONDS*Math.PI);}
    public static boolean lethal(int tile,double air){if(tile==WALL)return jumpHeight(air)<.25;return (tile==HOLE || tile==RED) && air<=0;}
}
