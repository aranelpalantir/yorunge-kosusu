package com.yorunge.runner;

/** Straight-line patrols between two holes, in recovery stretches of late courses. */
public final class Enemy {
    public static boolean active(int level,int section){return section>=0 && section<Track.sectionCount(level) && Track.hasEnemy(level,section);}
    public static int lane(int level,int section){return Track.route(level,section);}
    public static double position(int level,int section,double time){
        double period=5.2-(level-20)*.035;
        double phase=(time/period+section*.31)%1;
        double progress=phase<.5?phase*2:2-phase*2;
        double first=Track.squeezeEnd(level,section)+1.5,last=Track.sectionEnd(level,section)-1.5;
        return (first+progress*(last-first))*4;
    }
    public static boolean hit(int level,int section,double time,double distance,double lane,double air){
        return hitAtHeight(level,section,time,distance,lane,Track.jumpHeight(air));
    }
    public static boolean hitAtHeight(int level,int section,double time,double distance,double lane,double height){
        if(!active(level,section))return false;
        double difference=Math.abs(lane-lane(level,section))%8;
        difference=Math.min(difference,8-difference);
        return difference<.38 && Math.abs(position(level,section,time)-(distance+5))<1.1 && height<.65;
    }
}
