package com.yorunge.runner;

/** Character physics shared by rendering, collisions and course simulations, including custom character configuration. */
public final class Runner {
    public static int customColorIndex=0, customHeadType=0, customTrait=0;
    public static final int[] PALETTE={0xffffcd77,0xff9bdeef,0xffd7a8ed,0xff67dfcc,0xffff7a66,0xffb9e86d};
    public static final String[] COLOR_NAMES={"Kehribar","Buzul","Mor","Turkuaz","Mercan","Neon"};
    public static final String[] HEAD_NAMES={"Anten","Kanat","Miğfer","Taç"};
    public static final String[] TRAIT_NAMES={"Dengeli","Yüksek Zıplama","Hızlı Koşu","Süzülme"};

    public static int effectiveTrait(int character){return character==3?customTrait:character;}
    public static int color(int character,int[] defaults){
        return character==3?PALETTE[(customColorIndex%PALETTE.length+PALETTE.length)%PALETTE.length]:defaults[Math.max(0,Math.min(2,character))];
    }
    public static int headType(int character){return character==3?customHeadType:character;}
    public static double speed(int character,int level){
        int t=effectiveTrait(character);
        return Track.speed(level)*(t==2?1.10:1);
    }
    public static double jumpSeconds(int character){
        int t=effectiveTrait(character);
        return Track.JUMP_SECONDS*(t==1?1.10:t==3?1.15:1);
    }
    public static double height(int character,double air){
        if(air<=0)return 0;
        int t=effectiveTrait(character);
        double mult=t==1?1.25:1;
        return Math.sin(Math.max(0,1-air/jumpSeconds(character))*Math.PI)*mult;
    }
    public static boolean lethal(int character,int tile,double air){
        return tile==Track.WALL?height(character,air)<.25:(tile==Track.HOLE || tile==Track.RED) && air<=0;
    }
    public static String description(int character){
        int t=effectiveTrait(character);
        return t==1?"%25 yüksek zıplama":t==2?"%10 hızlı koşu":t==3?"%15 uzun süzülme":"Dengeli koşu ve zıplama";
    }
}
