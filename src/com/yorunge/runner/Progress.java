package com.yorunge.runner;

/** No selection or completion can unlock a level beyond the current frontier. */
public final class Progress {
    public static int clamp(int unlocked) {return Math.max(0,Math.min(Track.LEVELS-1,unlocked));}
    public static int select(int requested,int unlocked){return Math.max(0,Math.min(clamp(unlocked),requested));}
    public static int complete(int level,int unlocked){
        unlocked=clamp(unlocked);
        return level==unlocked?clamp(unlocked+1):unlocked;
    }
    public static int next(int level){return level==Track.LEVELS-1?-1:level+1;}
}
