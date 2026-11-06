package com.yorunge.runner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.*;
import android.util.SparseIntArray;

public final class GameView extends View {
    private static final int MENU=0, RUN=1, PAUSE=2, FALL=3, WIN=4, CUSTOMIZE=5, ENDING=6;
    private final Paint p=new Paint(3);
    private final Path path=new Path();
    private final RectF left=new RectF(), right=new RectF(), jump=new RectF(), pause=new RectF(), primary=new RectF(), secondary=new RectF();
    private final RectF inGameMusic=new RectF(),trackButton=new RectF(),musicButton=new RectF(),effectsButton=new RectF(),menuStart=new RectF(),endlessButton=new RectF(),levelStrip=new RectF();
    private final RectF[] characterButtons={new RectF(),new RectF(),new RectF(),new RectF()};
    private final RectF[] customColorBtns={new RectF(),new RectF(),new RectF(),new RectF(),new RectF(),new RectF()};
    private final RectF[] customHeadBtns={new RectF(),new RectF(),new RectF(),new RectF()};
    private final RectF[] customTraitBtns={new RectF(),new RectF(),new RectF(),new RectF()};
    private final RectF customSaveBtn=new RectF();
    private final int[] characterColors={0xffffcd77,0xff9bdeef,0xffd7a8ed};
    private final String[] characterNames={"Keşif","Nova","Atlas","Özel"};
    private GameAudio audio;
    private Collapse collapse;
    private Endless endless;
    private boolean endlessMode=false,newRecord=false,shieldActive=false,shieldTaken=false,flawlessRun=true;
    private boolean[] crystalTaken=new boolean[256];
    private int bestEndlessMeters=0,currentMeters=0,totalCrystals=0,comboMultiplier=1,frenzyCharge=0;
    private long flawlessMask=0,completedMask=0;
    private double boostTimer=0,dustTime=0,shieldBreakTime=0,frenzyTimer=0,calloutTime=0,prevAir=0;
    private float headTurn=0;
    // Visual-only animation state; never read by physics or collisions.
    private static final int POSE_RUN=0,POSE_IDLE=1,POSE_WAVE=2,POSE_FALL=3,POSE_CHEER=4;
    private static final int PARTICLES=64;
    private static final int[] DUST_COLORS={0xffbfe7dd,0xff8fc9bd};
    private static final int[] CONFETTI_COLORS={0xffffcd77,0xff67dfcc,0xff42e8ff,0xffff7a66,0xffd7a8ed};
    private double animClock=0,stride=0;
    private float squash=0,squashV=0,airPose=0,leanAnim=0,antenna=0,antennaV=0;
    private float selectPulse=0,dizzyTime=0,shakeTime=0,hitFlash=0,robotX=0,robotY=0;
    private boolean wasAirborne=false;
    // Final scene: the robot walks into the light, signals, and the light answers.
    private static final double ENDING_SECONDS=3.6,ENDING_WALK=1.05;
    private double endingTime=0;
    private float signalGlow=0,whiteOut=0;
    private int animPrevState=MENU,partNext=0;
    private final float[] partX=new float[PARTICLES],partY=new float[PARTICLES],partVX=new float[PARTICLES],partVY=new float[PARTICLES],partLife=new float[PARTICLES],partMax=new float[PARTICLES],partSize=new float[PARTICLES];
    private final int[] partColor=new int[PARTICLES];
    private String calloutText="";
    private int calloutColor=0xffffcd77;
    private float dustX=0,dustY=0;
    private float fallCamera=0;
    private double jumpFromDrop=0;
    private int character=0;
    private float levelScroll=0,dragStart=0,scrollStart=0;
    private boolean stripDragging=false,stripMoved=false;
    private String menuHint="Bölümleri sağa / sola kaydır";
    private double restartNotice=0,completionNotice=0,signalTime=0;
    private String signalText="";
    private long signalsSeen=0;
    private int completedLevel=-1,fallingRow=-1,fallingLane=-1;
    private final SparseIntArray touches=new SparseIntArray();
    private final SharedPreferences prefs;
    private int state=MENU, level=0, unlocked, targetLane=0;
    private double distance=0, lane=0, cameraLane=0, air=0, fall=0, elapsed=0, repeat=0;
    private long last=0;
    private float w,h,u,cx,cy,focal;
    private boolean keyLeft,keyRight,keyJump;
    private int overlayChoice=0;
    private static final int BG=Color.rgb(8,23,34), INK=Color.rgb(233,246,243), MINT=Color.rgb(103,223,204), GOLD=Color.rgb(255,205,119), CYAN=Color.rgb(66,232,255);
    private String failure="Boşluğa düştün. Zıpla ya da duvar değiştir.";

    public GameView(Context ctx) {
        super(ctx); setFocusableInTouchMode(true); requestFocus();
        prefs=ctx.getSharedPreferences("orbit",0); unlocked=Progress.clamp(prefs.getInt("unlocked",0)); level=unlocked;
        character=Math.max(0,Math.min(3,prefs.getInt("character",0)));
        Runner.customColorIndex=prefs.getInt("customColor",3);
        Runner.customHeadType=prefs.getInt("customHead",3);
        Runner.customTrait=prefs.getInt("customTrait",3);
        signalsSeen=prefs.getLong("signalsSeen",0);
        flawlessMask=prefs.getLong("flawlessMask",0);
        completedMask=prefs.getLong("completedMask",unlocked>0?((1L<<unlocked)-1):0);
        bestEndlessMeters=prefs.getInt("bestEndless",0);
        totalCrystals=prefs.getInt("totalCrystals",0);
        audio=new GameAudio(ctx,prefs);collapse=new Collapse(level);
        setContentDescription("Yörünge Koşusu. Sol ve sağ düğmeleriyle duvar değiştir, zıpla düğmesiyle boşlukları aş.");
    }
    public void audioForeground(){audio.foreground();}
    public void audioBackground(){audio.background();}
    public void releaseAudio(){audio.release();}
    private void menu(){state=MENU;endlessMode=false;touches.clear();stripDragging=false;centerSelection();invalidate();}
    private void centerSelection(){levelScroll=Math.max(0,Math.min(maxScroll(),level*88*u-levelStrip.width()/2+38*u));}
    private float maxScroll(){return Math.max(0,Track.LEVELS*88*u-12*u-levelStrip.width());}
    public void suspend() { if(state==RUN)state=PAUSE; overlayChoice=0; touches.clear(); keyLeft=keyRight=keyJump=false; last=0; invalidate(); }
    public void resumeClock(){last=0;invalidate();}
    public boolean back(){if(state==RUN){suspend();return true;}if(state!=MENU){menu();return true;}return false;}
    private void start(){
        endlessMode=false;level=Progress.select(level,unlocked);
        distance=0;lane=0;cameraLane=0;targetLane=0;collapse=new Collapse(level);
        fallCamera=0;jumpFromDrop=0;air=Runner.jumpSeconds(character);prevAir=air;fall=0;
        fallingRow=-1;fallingLane=-1;elapsed=0;repeat=0;last=0;
        boostTimer=0;dustTime=0;shieldBreakTime=0;frenzyTimer=0;calloutTime=0;
        comboMultiplier=1;frenzyCharge=0;java.util.Arrays.fill(crystalTaken,false);
        shieldActive=false;shieldTaken=false;
        touches.clear();keyLeft=keyRight=keyJump=false;state=RUN;invalidate();
    }
    private void startEndless(){
        endlessMode=true;newRecord=false;currentMeters=0;
        endless=new Endless(System.nanoTime()^((long)character<<32));
        distance=0;lane=0;cameraLane=0;targetLane=0;
        fallCamera=0;jumpFromDrop=0;air=Runner.jumpSeconds(character);prevAir=air;fall=0;
        fallingRow=-1;fallingLane=-1;elapsed=0;repeat=0;last=0;
        boostTimer=0;dustTime=0;shieldBreakTime=0;frenzyTimer=0;calloutTime=0;
        comboMultiplier=1;frenzyCharge=0;
        shieldActive=true;shieldTaken=false;
        touches.clear();keyLeft=keyRight=keyJump=false;state=RUN;invalidate();
    }
    private boolean held(int action){for(int i=0;i<touches.size();i++)if(touches.valueAt(i)==action)return true;return action==1?keyLeft:action==2?keyRight:keyJump;}
    private void turn(int dir){targetLane+=dir;repeat=.22;audio.play(1);}
    private void leap(){if(state==RUN && air<=0){jumpFromDrop=supportDrop();air=Runner.jumpSeconds(character);audio.play(0);}}
    private double supportDrop(){
        if(state==FALL)return Collapse.drop(fall);
        if(air>0)return jumpFromDrop*Math.max(0,1-(Runner.jumpSeconds(character)-air)/.16);
        int r=(int)((distance+5)/4),l=(int)Math.round(lane);
        if(endlessMode)return endless==null?0:Collapse.drop(endless.collapseAge(r,l,elapsed));
        if(collapse==null || collapse.level!=level)return 0;
        return Collapse.drop(collapse.age(r,l,elapsed));
    }
    private double currentSpeed(){
        int trait=Runner.effectiveTrait(character);
        double mult=trait==2?1.10:1;
        double base=endlessMode?endless.speed(distance)*mult:Runner.speed(character,level);
        if(frenzyTimer>0)return base*1.28;
        return boostTimer>0?base*1.22:base;
    }
    private void showCallout(String msg,int col){
        calloutText=msg;calloutColor=col;calloutTime=1.15;
    }
    private void collectCrystal(){
        totalCrystals+=comboMultiplier;
        prefs.edit().putInt("totalCrystals",totalCrystals).apply();
        audio.play(4);
        if(frenzyTimer<=0){
            frenzyCharge++;
            if(frenzyCharge%5==0 && comboMultiplier<5){
                comboMultiplier++;
                showCallout("ÇARPAN x"+comboMultiplier+"!",GOLD);
            }
            if(frenzyCharge>=12){
                frenzyCharge=0;frenzyTimer=4.2;shieldActive=true;
                audio.play(5);showCallout("YÖRÜNGE ÇILGINLIĞI!",0xffff7a66);
            }
        }
    }
    private void triggerDust(){
        double angle=(lane-cameraLane)*Math.PI/4;
        dustX=cx+(float)(Math.sin(angle)*3*focal/8);
        dustY=cy+(float)(Math.cos(angle)*3*focal/8*.924);
        dustTime=.28;
    }
    private void consumeShieldRescue(){
        shieldActive=false;shieldBreakTime=.55;
        jumpFromDrop=0;air=Runner.jumpSeconds(character)*1.12;
        audio.play(0);triggerDust();
    }
    private void burst(float x,float y,int count,float speed,int[] colors){
        for(int i=0;i<count;i++){
            int s=partNext;partNext=(partNext+1)%PARTICLES;
            double a=Math.random()*Math.PI*2;float v=speed*(.35f+(float)Math.random()*.65f);
            partX[s]=x;partY[s]=y;partVX[s]=(float)Math.cos(a)*v;partVY[s]=(float)Math.sin(a)*v-speed*.35f;
            partMax[s]=partLife[s]=.45f+(float)Math.random()*.45f;
            partSize[s]=(2.5f+(float)Math.random()*3.5f)*u;partColor[s]=colors[i%colors.length];
        }
    }
    private void crash(){
        shakeTime=.42f;hitFlash=.28f;
        int body=Runner.color(character,characterColors);
        burst(robotX,robotY-40*u,18,320*u,new int[]{body,shade(body,.72f),INK,0xff163342});
    }
    private void updateAnim(double dt){
        if(state==PAUSE)return;
        animClock+=dt;
        float d=(float)dt;
        boolean airborne=(state==RUN || state==ENDING) && air>0;
        if(state==ENDING)stride+=dt*11*Math.max(0,1-endingTime/ENDING_WALK);
        if(state==RUN){
            int trait=Runner.effectiveTrait(character);
            double before=stride;
            if(!airborne)stride+=dt*Math.min(15,7+currentSpeed()*.3)*(trait==2?.85:trait==1?.92:trait==3?.8:1);
            if(trait==2 && (int)(stride/Math.PI)!=(int)(before/Math.PI))burst(robotX,robotY,2,46*u,DUST_COLORS);
            if(airborne && !wasAirborne){squashV-=2.6f;antennaV+=Math.sin(animClock*7)>0?34:-34;}
        }
        if((state==RUN || state==ENDING) && !airborne && wasAirborne){squashV+=3.4f;antennaV+=Math.sin(animClock*7)>0?46:-46;}
        if(state==FALL && animPrevState==RUN)shakeTime=Math.max(shakeTime,.22f);
        animPrevState=state;wasAirborne=airborne;
        squashV+=(-squash*180-squashV*14)*d;squash=Math.max(-.32f,Math.min(.32f,squash+squashV*d));
        airPose+=((airborne?1:0)-airPose)*Math.min(1,d*14);
        leanAnim+=((float)(targetLane-lane)-leanAnim)*Math.min(1,d*10);
        antennaV+=((-leanAnim*9-antenna)*140-antennaV*9)*d;antenna=Math.max(-9,Math.min(9,antenna+antennaV*d));
        selectPulse=Math.max(0,selectPulse-d);dizzyTime=Math.max(0,dizzyTime-d);shakeTime=Math.max(0,shakeTime-d);hitFlash=Math.max(0,hitFlash-d);whiteOut=Math.max(0,whiteOut-d);
        for(int i=0;i<PARTICLES;i++){
            if(partLife[i]<=0)continue;
            partLife[i]-=d;partVY[i]+=620*u*d;partX[i]+=partVX[i]*d;partY[i]+=partVY[i]*d;
        }
    }
    private void drawParticles(Canvas c){
        for(int i=0;i<PARTICLES;i++){
            if(partLife[i]<=0)continue;
            int col=partColor[i];
            p.setColor(Color.argb((int)(255*Math.min(1,partLife[i]/partMax[i]*1.6f)),Color.red(col),Color.green(col),Color.blue(col)));
            float s=partSize[i];
            c.drawRect(partX[i]-s,partY[i]-s*.7f,partX[i]+s,partY[i]+s*.7f,p);
        }
    }
    private void update(double dt){
        if(state==RUN || state==FALL || state==CUSTOMIZE || state==ENDING)elapsed+=dt;
        restartNotice=Math.max(0,restartNotice-dt);completionNotice=Math.max(0,completionNotice-dt);signalTime=Math.max(0,signalTime-dt);
        boostTimer=Math.max(0,boostTimer-dt);dustTime=Math.max(0,dustTime-dt);shieldBreakTime=Math.max(0,shieldBreakTime-dt);
        frenzyTimer=Math.max(0,frenzyTimer-dt);calloutTime=Math.max(0,calloutTime-dt);
        double cycle=elapsed%7.2;
        boolean wantLook=state==ENDING?endingTime>2.4:state==CUSTOMIZE || state==FALL || dizzyTime>0 || completionNotice>1.1 || calloutTime>0 || frenzyTimer>0 || (cycle>4.5 && cycle<6.1);
        float speedTurn=wantLook?5.5f:4.2f;
        if(wantLook)headTurn=(float)Math.min(1,headTurn+dt*speedTurn);
        else headTurn=(float)Math.max(0,headTurn-dt*speedTurn);
        if(state==RUN){
            repeat-=dt;
            int dir=(held(2)?1:0)-(held(1)?1:0);
            if(dir!=0 && repeat<=0)turn(dir);
            lane+=(targetLane-lane)*Math.min(1,dt*16);
            cameraLane+=(lane-cameraLane)*Math.min(1,dt*8);
            prevAir=air;
            if(air>0){
                air=Math.max(0,air-dt);
                if(air==0 && prevAir>0)triggerDust();
            }
            distance+=dt*currentSpeed();
            int row=(int)((distance+5)/4);
            int curLane=Track.wrap((int)Math.round(lane));
            if(endlessMode){
                currentMeters=(int)(distance*1.2);
                if(currentMeters>bestEndlessMeters){
                    bestEndlessMeters=currentMeters;newRecord=true;
                    prefs.edit().putInt("bestEndless",bestEndlessMeters).apply();
                }
                if(!shieldActive && endless.hasShield(row,curLane) && Runner.height(character,air)<.65){
                    shieldActive=true;endless.consumeShield(row);audio.play(0);
                }
                if(endless.hasCrystal(row,curLane) && Runner.height(character,air)<.75){
                    endless.consumeCrystal(row);collectCrystal();
                }
                if(endless.hasPatrol(row)){
                    double pLane=endless.patrolLane(row,elapsed);
                    double diff=Math.abs(lane-pLane)%8;diff=Math.min(diff,8-diff);
                    if(diff>0.42 && diff<1.25 && Math.abs(row*4+2-(distance+5))<.45 && calloutTime<=0){
                        totalCrystals+=2*comboMultiplier;prefs.edit().putInt("totalCrystals",totalCrystals).apply();
                        showCallout("TEĞET GEÇİŞ! +"+(2*comboMultiplier),MINT);
                    }
                    if(diff<.42 && Math.abs(row*4+2-(distance+5))<1.3 && Runner.height(character,air)<.65){
                        if(frenzyTimer>0 || shieldActive){if(frenzyTimer<=0)consumeShieldRescue();else{jumpFromDrop=0;air=Runner.jumpSeconds(character);audio.play(0);}return;}
                        failure="Uzaylı devriyesine çarptın! ("+currentMeters+" m)";
                        audio.play(2);state=FALL;fall=0;touches.clear();crash();return;
                    }
                }
                int tile=endless.tile(row,curLane);
                if(tile==Track.BOOST && air<=.08){
                    boostTimer=.65;jumpFromDrop=0;air=Runner.jumpSeconds(character)*1.20;
                    audio.play(0);triggerDust();showCallout("SÜPER SIÇRAMA!",CYAN);return;
                }
                boolean fatal=Runner.lethal(character,tile,air);
                if(tile==Track.RED && air<=0){
                    if(endless.triggerCollapse(row,curLane,elapsed))audio.play(1);
                    fatal=!endless.canEscape(row,curLane,elapsed);
                }
                if(fatal){
                    if(frenzyTimer>0){jumpFromDrop=0;air=Runner.jumpSeconds(character)*1.05;audio.play(0);triggerDust();return;}
                    if(shieldActive){consumeShieldRescue();return;}
                    failure=(tile==Track.RED?"Kırmızı plaka çöktü! ":"Boşluğa düştün! ")+"Mesafe: "+currentMeters+" m";
                    if(tile==Track.RED){fallingRow=row;fallingLane=curLane;}
                    audio.play(2);state=FALL;overlayChoice=0;keyLeft=keyRight=keyJump=false;fall=tile==Track.RED?Math.min(.45,Math.max(0,endless.collapseAge(row,curLane,elapsed))):0;touches.clear();
                }
                return;
            }
            int mark=Story.markRow(level);
            if(mark>=0 && row>=mark && (signalsSeen&(1L<<level))==0){
                signalText=Story.fragment(level);signalTime=3;signalsSeen|=1L<<level;
                prefs.edit().putLong("signalsSeen",signalsSeen).apply();
            }
            if(!shieldTaken && !shieldActive && row==Track.shieldRow(level) && curLane==Track.shieldLane(level) && Runner.height(character,air)<.65){
                shieldActive=true;shieldTaken=true;audio.play(0);
            }
            if(row>=0 && row<crystalTaken.length && !crystalTaken[row] && Track.crystalLane(level,row)==curLane && Runner.height(character,air)<.75){
                crystalTaken[row]=true;collectCrystal();
            }
            if(row>=Track.length(level)){
                audio.play(3);completedLevel=level;completionNotice=2;
                if(level<Track.LEVELS-1)burst(robotX,robotY-50*u,26,340*u,CONFETTI_COLORS);
                completedMask|=1L<<level;
                if(flawlessRun)flawlessMask|=1L<<level;
                unlocked=Progress.complete(level,unlocked);
                prefs.edit().putInt("unlocked",unlocked).putLong("completedMask",completedMask).putLong("flawlessMask",flawlessMask).apply();
                touches.clear();
                int next=Progress.next(level);
                if(next<0){state=ENDING;endingTime=0;}else{level=next;flawlessRun=true;start();}
            }else {
                for(int section=0;section<Track.sectionCount(level);section++){
                    if(Enemy.active(level,section)){
                        double ez=Enemy.position(level,section,elapsed)-(distance+5);
                        double ediff=Math.abs(lane-Enemy.lane(level,section))%8;ediff=Math.min(ediff,8-ediff);
                        if(Math.abs(ez)<.5 && ediff>=.38 && ediff<=1.2 && calloutTime<=0){
                            totalCrystals+=2*comboMultiplier;prefs.edit().putInt("totalCrystals",totalCrystals).apply();
                            showCallout("TEĞET GEÇİŞ! +"+(2*comboMultiplier),MINT);
                        }
                    }
                    if(Enemy.hitAtHeight(level,section,elapsed,distance,lane,Runner.height(character,air))){
                        if(frenzyTimer>0){jumpFromDrop=0;air=Runner.jumpSeconds(character);audio.play(0);return;}
                        if(shieldActive){consumeShieldRescue();return;}
                        flawlessRun=false;audio.play(2);start();restartNotice=1.6;crash();dizzyTime=1.5f;return;
                    }
                }
                int tile=Track.tile(level,row,curLane);
                if(tile==Track.BOOST && air<=.08){
                    boostTimer=.65;jumpFromDrop=0;air=Runner.jumpSeconds(character)*1.20;
                    audio.play(0);triggerDust();showCallout("SÜPER SIÇRAMA!",CYAN);return;
                }
                boolean fatal=Runner.lethal(character,tile,air);
                if(tile==Track.RED && air<=0){
                    if(collapse.trigger(row,curLane,elapsed))audio.play(1);
                    fatal=!collapse.canEscape(row,curLane,elapsed);
                }
                if(fatal){
                    if(frenzyTimer>0){jumpFromDrop=0;air=Runner.jumpSeconds(character)*1.05;audio.play(0);triggerDust();return;}
                    if(shieldActive){consumeShieldRescue();return;}
                    flawlessRun=false;
                    failure=tile==Track.RED?"Kırmızı plaka çöktü! Üzerinden zıpla.":tile==Track.WALL?"Duvarı aşmak için daha yüksekte olmalısın.":"Boşluğa düştün. Zıpla ya da duvar değiştir.";
                    if(tile==Track.RED){fallingRow=row;fallingLane=curLane;}
                    if(tile==Track.WALL)crash();
                    audio.play(2);state=FALL;overlayChoice=0;keyLeft=keyRight=keyJump=false;fall=tile==Track.RED?Math.min(.45,Math.max(0,collapse.age(row,curLane,elapsed))):0;touches.clear();
                }
            }
        }else if(state==FALL){fall=Math.min(1.3,fall+dt);}
        else if(state==ENDING){
            endingTime+=dt;air=Math.max(0,air-dt);
            lane+=(targetLane-lane)*Math.min(1,dt*16);cameraLane+=(lane-cameraLane)*Math.min(1,dt*8);
            if(endingTime>=ENDING_SECONDS){state=WIN;whiteOut=.7f;}
        }
        if(state==RUN || state==FALL){float target=(float)(supportDrop()*u*.65);fallCamera+=(target-fallCamera)*Math.min(1,dt*12);}
    }
    @Override protected void onSizeChanged(int width,int height,int oldw,int oldh){
        w=width;h=height;u=Math.min(w/1100f,h/650f);cx=w*.5f;cy=h*.40f;focal=h*.88f;
        float s=98*u,m=32*u;
        left.set(m,h-m-s,m+s,h-m);right.set(m+s+18*u,h-m-s,m+2*s+18*u,h-m);
        jump.set(w-m-180*u,h-m-s,w-m,h-m);pause.set(w-m-64*u,m,w-m,m+58*u);
        primary.set(cx-175*u,h*.61f,cx+175*u,h*.61f+66*u);
        secondary.set(cx-175*u,h*.61f+82*u,cx+175*u,h*.61f+140*u);
        trackButton.set(28*u,20*u,350*u,72*u);
        inGameMusic.set(w-280*u,32*u,w-114*u,90*u);
        musicButton.set(w-208*u,20*u,w-28*u,72*u);
        effectsButton.set(w-400*u,20*u,w-220*u,72*u);
        menuStart.set(cx-310*u,h*.51f,cx-10*u,h*.51f+62*u);
        endlessButton.set(cx+10*u,h*.51f,cx+310*u,h*.51f+62*u);
        for(int i=0;i<4;i++){float x=cx+(i-1.5f)*175*u;characterButtons[i].set(x-80*u,h*.31f,x+80*u,h*.31f+114*u);}
        for(int i=0;i<6;i++){float x=cx-95*u+i*66*u;customColorBtns[i].set(x-26*u,h*.27f,x+26*u,h*.27f+52*u);}
        for(int i=0;i<4;i++){float x=cx-95*u+i*112*u;customHeadBtns[i].set(x-50*u,h*.44f,x+50*u,h*.44f+52*u);}
        for(int i=0;i<4;i++){float x=cx-95*u+i*112*u;customTraitBtns[i].set(x-50*u,h*.61f,x+50*u,h*.61f+52*u);}
        customSaveBtn.set(cx-160*u,h*.79f,cx+160*u,h*.79f+62*u);
        levelStrip.set(32*u,h-136*u,w-32*u,h-54*u);centerSelection();
    }
    @Override protected void onDraw(Canvas c){
        long now=System.nanoTime();double dt=last==0?0:Math.min(.035,(now-last)/1e9);last=now;update(dt);updateAnim(dt);
        float shake=shakeTime/.42f*9*u;
        c.drawColor(BG);c.save();c.translate((float)Math.sin(animClock*61)*shake,-fallCamera+(float)Math.cos(animClock*47)*shake);
        drawStars(c);drawSpeedLines(c);drawEndLight(c,false);drawTunnel(c);drawEndLight(c,true);drawEnemies(c);drawFallingPlate(c);drawRobot(c);drawParticles(c);
        c.restore();
        if(state==ENDING){p.setColor(Color.argb((int)(255*Math.max(0,Math.min(1,(endingTime-2.75)/.85))),255,241,207));c.drawRect(0,0,w,h,p);}
        if(hitFlash>0){p.setColor(Color.argb((int)(hitFlash/.28f*90),255,70,60));c.drawRect(0,0,w,h,p);}
        if(state==RUN || state==PAUSE || (state==FALL && fall<=1.1)){drawHud(c);controls(c);}
        if(state==MENU)drawMenu(c);
        if(state==CUSTOMIZE)drawCustomize(c);
        if(state==PAUSE)overlay(c,"Kısa bir mola",endlessMode?"Sonsuz Yörünge · "+currentMeters+" m":"Hazır olduğunda yörüngene dön.","Devam et","Ana menü","");
        if(state==FALL && fall>1.1)overlay(c,endlessMode && newRecord?"Yeni Rekor: "+currentMeters+" m!":"Bir daha deneyelim",failure,"Yeniden dene","Ana menü","");
        if(state==WIN)overlay(c,"Bölüm 50 bitti!","Uzakta bir ışık daha yandı.","İlk bölümden oyna","Ana menü","");
        if(state==PAUSE)drawAudio(c);
        if(state==RUN && restartNotice>0){p.setColor(0xdd7c3048);c.drawRoundRect(cx-260*u,120*u,cx+260*u,168*u,10*u,10*u,p);text(c,"Uzaylıya çarptın! Bölüm yeniden başladı.",cx,151*u,19,INK,true,Paint.Align.CENTER);}
        if(state==RUN && completionNotice>0){
            p.setColor(0xed193d4b);c.drawRoundRect(cx-210*u,116*u,cx+210*u,198*u,14*u,14*u,p);
            text(c,"Bölüm "+(completedLevel+1)+" bitti!",cx,151*u,27,GOLD,true,Paint.Align.CENTER);
            text(c,"Şimdi Bölüm "+(level+1)+(level>0 && level%4==0?" · Hız arttı":""),cx,182*u,18,INK,false,Paint.Align.CENTER);
        }
        if(state==RUN && shieldBreakTime>0){
            p.setColor(0xd0184a5c);c.drawRoundRect(cx-195*u,120*u,cx+195*u,162*u,10*u,10*u,p);
            text(c,"Enerji kalkanı seni kurtardı!",cx,147*u,18,CYAN,true,Paint.Align.CENTER);
        }
        if(state==RUN && calloutTime>0){
            float scale=(float)Math.min(1.15,calloutTime*1.8);
            text(c,calloutText,cx,138*u,21*scale,calloutColor,true,Paint.Align.CENTER);
        }
        if(state==RUN && signalTime>0){
            p.setTextSize(19*u);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
            float half=p.measureText(signalText)/2+18*u;
            p.setColor(0xc4081722);c.drawRoundRect(cx-half,h-182*u,cx+half,h-146*u,10*u,10*u,p);
            text(c,signalText,cx,h-157*u,19,0xffe4eef0,false,Paint.Align.CENTER);
        }
        if(whiteOut>0){p.setColor(Color.argb((int)(255*whiteOut/.7f),255,241,207));c.drawRect(0,0,w,h,p);}
        if(state==RUN || state==MENU || state==CUSTOMIZE || state==ENDING || whiteOut>0 || (state==FALL && fall<1.15))postInvalidateOnAnimation();
    }
    private void drawStars(Canvas c){
        int baseColor=endlessMode?(endless==null?0xff255e73:endless.color((int)(distance/4))):Track.color(level);
        p.setColor(Color.argb(38,Color.red(baseColor),Color.green(baseColor),Color.blue(baseColor)));
        c.drawOval(cx-340*u,cy-190*u,cx+340*u,cy+190*u,p);
        for(int i=0;i<125;i++){
            int seed=(i+1)*1103515245+12345;
            float x=((seed>>>8)&65535)/65535f*w;
            seed=seed*1664525+1013904223;
            float y=((seed>>>8)&65535)/65535f*h;
            boolean bright=i%7==0;float radius=(bright?2.6f:i%3==0?1.6f:1.0f)*u;
            p.setColor(bright?0xffeff9ff:i%3==0?0xffa8ccd9:0xff6d98ae);c.drawCircle(x,y,radius,p);
            if(bright){p.setStrokeWidth(.9f*u);c.drawLine(x-4*u,y,x+4*u,y,p);c.drawLine(x,y-4*u,x,y+4*u,p);}
        }
        double cycle=(elapsed*.38)%6.0;
        if(cycle<1.1){
            float prog=(float)(cycle/1.1);
            float sx=w*(.15f+prog*.65f),sy=h*(.12f+prog*.28f);
            p.setColor(Color.argb((int)((1-prog)*190),220,248,255));
            p.setStrokeWidth(2*u);
            c.drawLine(sx,sy,sx-42*u,sy-18*u,p);
            c.drawCircle(sx,sy,2.2f*u,p);
        }
    }
    private static float pulseAt(double time,double center){return (float)Math.max(0,1-Math.abs(time-center)/.13);}
    /** The light past the last tunnel row of the final level; the spill pass tints the walls in front of it. */
    private void drawEndLight(Canvas c,boolean spill){
        if(endlessMode || level!=Track.LEVELS-1 || state==MENU || state==WIN)return;
        double z=Track.length(level)*4-distance;
        if(z>108)return;
        float reply=state==ENDING?Math.max(pulseAt(endingTime,1.95),pulseAt(endingTime,2.3)):0;
        float r=(float)(3*focal/(z+3))*(1.02f+reply*.05f);
        if(spill){
            int alpha=(int)Math.min(255,90*(1-z/108)*(1+reply*.9f));
            float reach=r*2.1f;
            p.setShader(new RadialGradient(cx,cy,reach,new int[]{Color.argb(alpha,255,224,160),Color.argb(alpha,255,224,160),Color.argb(0,255,224,160)},new float[]{0,.45f,1},Shader.TileMode.CLAMP));
            c.drawCircle(cx,cy,reach,p);
        }else{
            p.setShader(new RadialGradient(cx,cy,r,new int[]{0xffffffff,0xfffff1cf,mixColor(0xffffd98a,0xfffff1cf,reply)},new float[]{0,.45f+reply*.2f,1},Shader.TileMode.CLAMP));
            c.drawCircle(cx,cy,r,p);
        }
        p.setShader(null);
    }
    private void drawSpeedLines(Canvas c){
        double spd=currentSpeed();
        if(spd<9.8 && boostTimer<=0)return;
        int count=boostTimer>0?18:10;
        p.setStrokeWidth((boostTimer>0?2.2f:1.4f)*u);
        for(int i=0;i<count;i++){
            double a=i*Math.PI*2/count+elapsed*.4;
            double z=((i*19.0-(distance*2.8))%85+85)%85+6;
            float x0=cx+(float)(Math.sin(a)*3.4*focal/(z+3));
            float y0=cy+(float)(Math.cos(a)*3.4*focal/(z+3));
            float x1=cx+(float)(Math.sin(a)*3.4*focal/(z+13));
            float y1=cy+(float)(Math.cos(a)*3.4*focal/(z+13));
            p.setColor(boostTimer>0?0x8842e8ff:0x44a8ccd9);
            c.drawLine(x0,y0,x1,y1,p);
        }
    }
    private float px(double a,double z){return cx+(float)(Math.sin(a)*3*focal/(z+3));}
    private float py(double a,double z){return cy+(float)(Math.cos(a)*3*focal/(z+3));}
    private static final double TUNNEL_NEAR=-2.4;
    private void drawTunnel(Canvas c){
        int first=Math.max(0,(int)(distance/4));
        int boundary=endlessMode?first+1000:Track.length(level);
        int lastRow=endlessMode?first+27:Math.min(boundary+(level<Track.LEVELS-1?26:-1),first+27);
        double rotation=cameraLane*Math.PI/4;
        // Floor rows continue past the camera so the tunnel reaches the screen edges at any aspect ratio.
        for(int row=lastRow;row>=Math.max(0,(int)((distance+TUNNEL_NEAR)/4));row--){
            double z0=Math.max(.15,row*4-distance),z1=(row+1)*4-distance,nearZ=Math.max(TUNNEL_NEAR,row*4-distance);
            if(z1<=TUNNEL_NEAR)continue;
            int drawLevel=row>=boundary?level+1:level;
            int localRow=row>=boundary?row-boundary:row;
            for(int side=0;side<8;side++){
                int tile=endlessMode?endless.tile(row,side):Track.tile(drawLevel,localRow,side);
                if(endlessMode){
                    if(endless.collapseAge(row,side,elapsed)>=0)continue;
                }else if(drawLevel==level && collapse!=null && collapse.level==level && collapse.age(row,side,elapsed)>=0)continue;
                if(tile==Track.HOLE)continue;
                double a=side*Math.PI/4-Math.PI/8-rotation,b=a+Math.PI/4;
                path.reset();path.moveTo(px(a,nearZ),py(a,nearZ));path.lineTo(px(b,nearZ),py(b,nearZ));path.lineTo(px(b,z1),py(b,z1));path.lineTo(px(a,z1),py(a,z1));path.close();
                float fade=(float)Math.max(.18,1-z0/115);
                int palette=endlessMode?endless.color(row):Track.color(drawLevel);float shade=side%2==0?1:.82f;
                int r=(int)(Color.red(palette)*fade*shade),g=(int)(Color.green(palette)*fade*shade),bl=(int)(Color.blue(palette)*fade*shade);
                p.setStyle(Paint.Style.FILL);
                p.setColor(tile==Track.RED?Color.rgb((int)(198*fade),(int)(53*fade),(int)(59*fade)):tile==Track.BOOST?Color.rgb((int)(40*fade),(int)(195*fade),(int)(230*fade)):Color.rgb(r,g,bl));
                c.drawPath(path,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(.7f,1.6f*u));p.setColor(Color.argb((int)(125*fade),103,223,204));c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
                if(z1<=.15)continue;
                if(tile==Track.RED){
                    p.setColor(0xffffb2a0);p.setStrokeWidth(2*u);
                    double mid=(a+b)/2,zc=(z0+z1)/2;
                    c.drawLine(px(a+.16,zc-.4),py(a+.16,zc-.4),px(mid,zc),py(mid,zc),p);
                    c.drawLine(px(mid,zc),py(mid,zc),px(b-.16,zc+.4),py(b-.16,zc+.4),p);
                }else if(tile==Track.BOOST){
                    p.setColor(0xffd6fbff);p.setStrokeWidth(2.4f*u);
                    double mid=(a+b)/2,zc=(z0+z1)/2;
                    c.drawLine(px(a+.18,zc-.5),py(a+.18,zc-.5),px(mid,zc+.6),py(mid,zc+.6),p);
                    c.drawLine(px(mid,zc+.6),py(mid,zc+.6),px(b-.18,zc-.5),py(b-.18,zc-.5),p);
                }
                boolean hasShieldOrb=endlessMode?endless.hasShield(row,side):(!shieldTaken && drawLevel==level && localRow==Track.shieldRow(drawLevel) && side==Track.shieldLane(drawLevel));
                if(hasShieldOrb){
                    double mid=(a+b)/2,zc=(z0+z1)/2;
                    float ox=cx+(float)(Math.sin(mid)*2.65*focal/(zc+3));
                    float oy=cy+(float)(Math.cos(mid)*2.65*focal/(zc+3));
                    float rad=(float)(.26*focal/(zc+3));
                    p.setColor(0x6642e8ff);c.drawCircle(ox,oy,rad*1.4f,p);
                    p.setColor(CYAN);c.drawCircle(ox,oy,rad,p);
                    p.setColor(INK);c.drawCircle(ox-rad*.25f,oy-rad*.25f,rad*.28f,p);
                }
                boolean hasCrystal=endlessMode?endless.hasCrystal(row,side):(drawLevel==level && localRow>=0 && localRow<crystalTaken.length && !crystalTaken[localRow] && Track.crystalLane(drawLevel,localRow)==side);
                if(hasCrystal){
                    double mid=(a+b)/2,zc=(z0+z1)/2;
                    float ox=cx+(float)(Math.sin(mid)*2.68*focal/(zc+3));
                    float oy=cy+(float)(Math.cos(mid)*2.68*focal/(zc+3));
                    float rad=(float)(.21*focal/(zc+3));
                    path.reset();
                    path.moveTo(ox,oy-rad*1.25f);path.lineTo(ox+rad,oy);
                    path.lineTo(ox,oy+rad*1.25f);path.lineTo(ox-rad,oy);path.close();
                    p.setColor(GOLD);c.drawPath(path,p);
                    p.setColor(0xfffff2cc);c.drawCircle(ox-rad*.2f,oy-rad*.3f,rad*.25f,p);
                }
                if(!endlessMode && localRow==Story.markRow(drawLevel) && side==Track.wrap(drawLevel+2) && z0<60){
                    double mid=(a+b)/2,zc=(z0+z1)/2;
                    float mx=px(mid,zc),my=py(mid,zc),size=(float)(.23*focal/(zc+3));
                    p.setColor(0xffb3c6b9);p.setStrokeWidth(Math.max(1,size*.13f));
                    for(int mark=0;mark<3;mark++)c.drawLine(mx+(mark-1)*size*.65f,my-size*.6f,mx+(mark-1)*size*.65f,my+size*.3f,p);
                    p.setStyle(Paint.Style.STROKE);c.drawCircle(mx,my+size*.7f,size*.22f,p);p.setStyle(Paint.Style.FILL);
                }
                if(tile==Track.WALL){
                    float ax=px(a,z0),ay=py(a,z0),bx=px(b,z0),by=py(b,z0);
                    float iax=cx+(ax-cx)*.79f,iay=cy+(ay-cy)*.79f;
                    float ibx=cx+(bx-cx)*.79f,iby=cy+(by-cy)*.79f;
                    float backAx=cx+(px(a,z1)-cx)*.79f,backAy=cy+(py(a,z1)-cy)*.79f;
                    float backBx=cx+(px(b,z1)-cx)*.79f,backBy=cy+(py(b,z1)-cy)*.79f;
                    path.reset();path.moveTo(iax,iay);path.lineTo(ibx,iby);path.lineTo(backBx,backBy);path.lineTo(backAx,backAy);path.close();
                    p.setColor(side%2==0?0xff8f562c:0xffb37336);c.drawPath(path,p);
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*u);p.setColor(0xfff5ba68);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
                    path.reset();path.moveTo(ax,ay);path.lineTo(bx,by);
                    path.lineTo(cx+(bx-cx)*.79f,cy+(by-cy)*.79f);
                    path.lineTo(cx+(ax-cx)*.79f,cy+(ay-cy)*.79f);path.close();
                    p.setColor(0xffd9984e);c.drawPath(path,p);
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*u);p.setColor(GOLD);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
                }
                if(row==boundary || (localRow>3 && (localRow*17+drawLevel*31)%23==0)){p.setColor(Color.argb((int)(210*fade),255,205,119));p.setStrokeWidth(2*u);c.drawLine(px(a,z0),py(a,z0),px(b,z0),py(b,z0),p);}
            }
        }
    }
    private void drawFallingPlate(Canvas c){
        int first=Math.max(0,(int)(distance/4));
        int lastRow=endlessMode?first+27:Math.min(Track.length(level)-1,first+27);
        if(!endlessMode && (collapse==null || collapse.level!=level))return;
        for(int row=lastRow;row>=first;row--)for(int side=0;side<8;side++){
            double age=endlessMode?endless.collapseAge(row,side,elapsed):collapse.age(row,side,elapsed);
            if(age<0 || age>1.3)continue;
            double a=side*Math.PI/4-Math.PI/8-cameraLane*Math.PI/4,b=a+Math.PI/4;
            double mid=(a+b)/2,z0=Math.max(.15,row*4-distance),z1=(row+1)*4-distance;
            if(z1<=.15)continue;
            float drop=(float)(Collapse.drop(age)*u);
            c.save();c.translate((float)Math.sin(mid)*drop,(float)Math.cos(mid)*drop);
            path.reset();path.moveTo(px(a,z0),py(a,z0));path.lineTo(px(b,z0),py(b,z0));path.lineTo(px(b,z1),py(b,z1));path.lineTo(px(a,z1),py(a,z1));path.close();
            p.setColor(0xffc6353b);c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*u);p.setColor(0xffffb2a0);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
            double zc=(z0+z1)/2;
            c.drawLine(px(a+.1,zc),py(a+.1,zc),px(mid,zc-.5),py(mid,zc-.5),p);
            c.drawLine(px(mid,zc-.5),py(mid,zc-.5),px(b-.1,zc+.3),py(b-.1,zc+.3),p);
            c.restore();
        }
    }
    private static int mixColor(int a,int b,float t){
        return Color.rgb((int)(Color.red(a)+(Color.red(b)-Color.red(a))*t),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*t),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t));
    }
    private boolean alienDanger(double alienLane,double z){
        double diff=Math.abs(lane-alienLane)%8;diff=Math.min(diff,8-diff);
        return state==RUN && diff<.6 && z>4 && z<21;
    }
    private void drawAlienSprite(Canvas c,double angle,double z,boolean markings,int seed,boolean danger){
        float x=cx+(float)(Math.sin(angle)*2.7*focal/(z+3));
        float y=cy+(float)(Math.cos(angle)*2.7*focal/(z+3));
        float size=(float)(.34*focal/(z+3));
        float t=(float)(animClock*4.2+seed*1.7);
        float wob=(float)Math.sin(t)*.07f;
        float pulse=danger?.5f+.5f*(float)Math.sin(animClock*14):0;
        y+=(float)Math.sin(t*.5f)*size*.1f;
        if(danger)size*=1.06f;
        int body=danger?mixColor(0xffe0d45c,0xffff7a66,pulse):0xffb9e86d;
        boolean detailed=size>=6;
        if(detailed){
            // Skirt lobes ripple around the body as it hovers.
            p.setColor(shade(body,.7f));
            for(int i=0;i<4;i++)c.drawCircle(x+(i-1.5f)*size*.46f,y+size*(.58f+(float)Math.sin(t*1.6f+i*1.5f)*.1f),size*.22f,p);
        }
        p.setColor(body);c.drawOval(x-size*(1+wob),y-size*(1-wob),x+size*(1+wob),y+size*.65f,p);
        boolean blink=(animClock+seed*.9)%2.8<.1;
        p.setColor(0xff233e27);
        if(blink){c.drawRect(x-size*.62f,y-size*.11f,x-size*.18f,y-size*.03f,p);c.drawRect(x+size*.18f,y-size*.11f,x+size*.62f,y-size*.03f,p);}
        else{
            c.drawOval(x-size*.66f,y-size*.25f,x-size*.15f,y+size*.10f,p);c.drawOval(x+size*.15f,y-size*.25f,x+size*.66f,y+size*.10f,p);
            if(detailed){
                float dx=robotX-x,dy=robotY-40*u-y,len=Math.max(1,(float)Math.hypot(dx,dy));
                float ox=dx/len*size*.08f,oy=dy/len*size*.05f;
                p.setColor(danger?0xffffe9c4:0xffeaffc4);
                c.drawCircle(x-size*.405f+ox,y-size*.075f+oy,size*.095f,p);c.drawCircle(x+size*.405f+ox,y-size*.075f+oy,size*.095f,p);
            }
        }
        if(markings){p.setColor(0xff547146);p.setStrokeWidth(Math.max(1,size*.07f));for(int mark=-1;mark<=1;mark++)c.drawLine(x+mark*size*.22f,y+size*.25f,x+mark*size*.22f,y+size*.45f,p);}
        float tipL=detailed?(float)Math.sin(t*1.3f)*size*.12f:0,tipR=detailed?(float)Math.sin(t*1.3f+1.4f)*size*.12f:0;
        p.setColor(0xffeaffc4);p.setStrokeWidth(Math.max(1,size*.1f));
        c.drawLine(x-size*.5f,y-size*.6f,x-size*.85f+tipL,y-size*1.4f,p);c.drawLine(x+size*.5f,y-size*.6f,x+size*.85f+tipR,y-size*1.4f,p);
        if(detailed){c.drawCircle(x-size*.85f+tipL,y-size*1.42f,size*.09f,p);c.drawCircle(x+size*.85f+tipR,y-size*1.42f,size*.09f,p);}
        if(danger){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.5f,size*.09f));
            p.setColor(Color.argb((int)(110+pulse*130),255,122,102));
            c.drawCircle(x,y-size*.12f,size*(1.3f+pulse*.14f),p);
            p.setStyle(Paint.Style.FILL);
        }
    }
    private void drawEnemies(Canvas c){
        if(endlessMode){
            int first=Math.max(0,(int)(distance/4));
            for(int row=first+26;row>=first;row--){
                if(!endless.hasPatrol(row))continue;
                double z=row*4+2-distance;
                if(z<.1 || z>100)continue;
                double angle=(endless.patrolLane(row,elapsed)-cameraLane)*Math.PI/4;
                drawAlienSprite(c,angle,z,row>80,row,alienDanger(endless.patrolLane(row,elapsed),z));
            }
            return;
        }
        for(int section=Track.sectionCount(level)-1;section>=0;section--){
            if(!Enemy.active(level,section))continue;
            double z=Enemy.position(level,section,elapsed)-distance;
            if(z<.1 || z>100)continue;
            double angle=(Enemy.lane(level,section)-cameraLane)*Math.PI/4;
            drawAlienSprite(c,angle,z,level>=21,section,alienDanger(Enemy.lane(level,section),z));
        }
    }
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
    private static int shade(int color,float k){return Color.rgb((int)(Color.red(color)*k),(int)(Color.green(color)*k),(int)(Color.blue(color)*k));}
    private void limb(Canvas c,float x0,float y0,float x1,float y1,float x2,float y2,float width,int color){
        p.setColor(color);p.setStrokeWidth(width*u);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x0*u,y0*u,x1*u,y1*u,p);c.drawLine(x1*u,y1*u,x2*u,y2*u,p);
    }
    private void drawHand(Canvas c,int head,float x,float y,int color){
        p.setColor(color);
        if(head==1)c.drawCircle(x*u,y*u,6.5f*u,p);
        else if(head==2)c.drawRoundRect((x-5)*u,(y-7)*u,(x+5)*u,(y+7)*u,3*u,3*u,p);
        else c.drawCircle(x*u,y*u,5*u,p);
    }
    private void drawRobotBody(Canvas c,int charIdx,double phase,float air01,float airProg,boolean fastTrail,int pose){
        int bodyColor=Runner.color(charIdx,characterColors);
        int bodyDark=shade(bodyColor,.72f);
        int head=Runner.headType(charIdx);
        int trait=Runner.effectiveTrait(charIdx);
        boolean striding=pose==POSE_RUN || pose==POSE_CHEER;
        boolean idle=pose==POSE_IDLE || pose==POSE_WAVE;
        float runAmt=striding?1-air01:0,airAmt=striding?air01:0;
        float sinP=(float)Math.sin(phase),cosP=(float)Math.cos(phase),clock=(float)animClock;
        // Each ability gets its own gait: Nova bounces, Atlas stomps, gliders skim.
        float stepLift=trait==2?8:trait==1?6:trait==3?3.5f:6.5f;
        float bobAmp=trait==2?2.2f:trait==1?5:trait==3?1.6f:3.4f;
        float rollAmp=trait==2?3.6f:trait==1?1.5f:trait==3?.8f:2.2f;
        float stance=trait==2?13:10.5f;
        float breath=idle?(float)Math.sin(clock*2.4f+charIdx)*1.1f:0;
        float bob=runAmt*Math.abs(sinP)*bobAmp;
        float tuck=(float)Math.sin(airProg*Math.PI);
        float footL=-stepLift*Math.max(0,sinP)*runAmt+airAmt*(-3*tuck+4*(1-tuck));
        float footR=-stepLift*Math.max(0,-sinP)*runAmt+airAmt*(1*tuck+4*(1-tuck));
        float splay=0;
        if(pose==POSE_FALL){float kick=(float)Math.sin(clock*22)*6;footL=1+kick;footR=1-kick;splay=5;}

        if(striding && (fastTrail || (trait==2 && runAmt>.5f))){
            float len=(fastTrail?36:18)*(.8f+.2f*(float)Math.sin(clock*38));
            int outer=frenzyTimer>0 || !fastTrail?0xccff9b54:0xcc42e8ff;
            for(int side=-1;side<=1;side+=2){
                float fx=side*(stance-1);
                path.reset();path.moveTo((fx-6)*u,-6*u);path.quadTo((fx-5)*u,len*.5f*u,fx*u,(len+6)*u);path.quadTo((fx+5)*u,len*.5f*u,(fx+6)*u,-6*u);path.close();
                p.setColor(outer);c.drawPath(path,p);
            }
        }
        // Jointed legs; the knee bends outward as the foot lifts.
        int legColor=0xff2c4a58;
        limb(c,-stance,-10,-stance+footL*.45f,(-10+footL)/2,-stance-splay,footL,5,legColor);
        limb(c,stance,-10,stance-footR*.45f,(-10+footR)/2,stance+splay,footR,5,legColor);
        p.setColor(INK);
        float wL=7*(1+footL*.012f),wR=7*(1+footR*.012f);
        c.drawRoundRect((-stance-splay-wL)*u,(footL-4)*u,(-stance-splay+wL)*u,(footL+7)*u,4*u,4*u,p);
        c.drawRoundRect((stance+splay-wR)*u,(footR-4)*u,(stance+splay+wR)*u,(footR+7)*u,4*u,4*u,p);

        float roll=runAmt*sinP*rollAmp;
        c.save();c.translate(0,(-bob+breath-5)*u);c.rotate(roll);
        boolean forcedFace=state==CUSTOMIZE || idle;
        float turn=forcedFace?1f:headTurn;

        if(head==1){
            // Nova's fins beat quickly in the air and rest on the ground.
            float reach=.55f+.45f*air01,flap=(float)Math.sin(clock*(air01>.3f?18:5))*6;
            p.setColor(shade(bodyColor,.82f));
            for(int side=-1;side<=1;side+=2){
                path.reset();path.moveTo(side*22*u,-30*u);path.lineTo(side*(22+24*reach)*u,(-42+flap)*u);path.lineTo(side*24*u,-16*u);path.close();
                c.drawPath(path,p);
            }
        }
        // 1. Lower Torso / Suit (always facing forward into the tunnel, with back pack)
        p.setColor(bodyDark);c.drawRoundRect(-23*u,-26*u,23*u,2*u,9*u,9*u,p);
        p.setColor(bodyColor);c.drawRoundRect(-22*u,-32*u,22*u,-6*u,8*u,8*u,p);
        // Back thruster pack stays on the back of the torso
        p.setColor(0xff163342);c.drawRoundRect(-13*u,-29*u,13*u,-10*u,4*u,4*u,p);
        p.setColor(fastTrail?CYAN:MINT);c.drawCircle(0,-19.5f*u,(4.5f+(float)Math.sin(clock*5)*.5f)*u,p);
        p.setColor(INK);c.drawCircle(0,-19.5f*u,2f*u,p);
        if(signalGlow>0){
            p.setColor(Color.argb((int)(235*signalGlow),255,224,160));c.drawCircle(0,-19.5f*u,(5+6*signalGlow)*u,p);
        }

        // Arms swing against the legs, then rise and spread through the jump.
        float swing=cosP*5.5f*runAmt;
        float settle=airProg*airProg*(3-2*airProg);
        float airX=trait==3?40:mix(30,34,settle),airY=trait==3?-30+(float)Math.sin(clock*6)*2:mix(-40,-26,settle);
        float handLX=-mix(27,airX,airAmt),handLY=mix(-16+swing,airY,airAmt);
        float handRX=mix(27,airX,airAmt),handRY=mix(-16-swing,airY,airAmt);
        if(idle){handLX=-26;handLY=-12+breath;handRX=26;handRY=-12+breath;}
        if(pose==POSE_WAVE){handRX=31+(float)Math.sin(clock*11)*5;handRY=-46;}
        if(pose==POSE_CHEER){handLX=-31;handLY=-48+(float)Math.sin(clock*14)*5;handRX=31;handRY=-48-(float)Math.sin(clock*14)*5;}
        if(pose==POSE_FALL){
            handLX=-31-(float)Math.sin(clock*19)*3;handLY=-40+(float)Math.sin(clock*26)*9;
            handRX=31+(float)Math.sin(clock*19)*3;handRY=-40-(float)Math.sin(clock*26)*9;
        }
        limb(c,-22,-27,(-22+handLX)/2-3,(-27+handLY)/2,handLX,handLY,4.5f,bodyDark);
        limb(c,22,-27,(22+handRX)/2+3,(-27+handRY)/2,handRX,handRY,4.5f,bodyDark);
        drawHand(c,head,handLX,handLY,bodyColor);
        drawHand(c,head,handRX,handRY,bodyColor);

        // Neck ring separating torso and rotating head
        p.setColor(0xff102a38);c.drawRoundRect(-12*u,-35*u,12*u,-30*u,2*u,2*u,p);

        // 2. Separate Head (rotates smoothly over the left shoulder to look back and smile)
        c.save();
        float s=turn*turn*(3-2*turn);
        float lean=striding?leanAnim:0;
        float headTilt=(forcedFace?0:(float)Math.sin(s*Math.PI)*-6f)-roll*.5f-lean*4;
        float headShift=(forcedFace?0:s*-3.5f*u)+lean*5*u;
        c.translate(headShift,(-46+runAmt*Math.abs(cosP)*1.4f)*u);
        c.rotate(headTilt);
        p.setColor(bodyColor);
        c.drawRoundRect(-21*u,-15*u,21*u,14*u,head==2?5*u:12*u,head==2?5*u:12*u,p);

        // Headgear on top of the head
        p.setStrokeWidth(3*u);
        if(head==2){
            c.drawRect(-18*u,-23*u,-10*u,-13*u,p);c.drawRect(10*u,-23*u,18*u,-13*u,p);
        }else if(head==3){
            path.reset();
            path.moveTo(-14*u,-14*u);path.lineTo(-10*u,-26*u);path.lineTo(-4*u,-18*u);
            path.lineTo(0,-29*u);path.lineTo(4*u,-18*u);path.lineTo(10*u,-26*u);path.lineTo(14*u,-14*u);
            path.close();c.drawPath(path,p);
        }else if(head==0){
            float sway=((float)Math.sin(phase*.7)*3.2f+(striding?antenna:0))*u;
            c.drawLine(0,-15*u,sway,-25*u,p);c.drawCircle(sway,-28*u,4f*u,p);
            p.setColor(signalGlow>0?0xffffe0a0:0xffeff9ff);c.drawCircle(sway-(signalGlow>0?0:1.1f*u),-28*u-(signalGlow>0?0:1.1f*u),(1.4f+signalGlow*4f)*u,p);
        }

        // Back of the head plates (slides right and shrinks smoothly using 3D cosine projection)
        if(s<.65f){
            float backProg=s/.65f;
            float bx=(float)Math.sin(backProg*Math.PI*.5)*16*u;
            float bw=(float)Math.cos(backProg*Math.PI*.5)*6*u;
            // Same three-stroke maintenance mark as the tunnel panels and late patrols.
            p.setColor(0xff5f7476);p.setStrokeWidth(1.8f*u);
            for(int mark=-1;mark<=1;mark++)c.drawLine(bx+mark*bw*.7f,-7*u,bx+mark*bw*.7f,1*u,p);
            c.drawCircle(bx,5*u,1.5f*u,p);
        }

        // Face visor & expression (continuous 3D cylindrical wrap from left profile to front)
        boolean dizzy=dizzyTime>0 && striding;
        if(s>.18f){
            float t=(s-.18f)/.82f;
            double rot=(1-t)*Math.PI*.48;
            float fx=-(float)Math.sin(rot)*14*u;
            float fw=Math.max(4f*u,(float)Math.cos(rot)*16*u);
            p.setColor(0xff102a38);
            c.drawRoundRect(fx-fw,-10*u,fx+fw,9*u,5*u,5*u,p);
            p.setColor(MINT);
            float eyeSep=fw*.46f;
            float leftEyeScale=Math.max(0f,(t-.22f)/.78f);
            boolean happy=t>.75f && (state==CUSTOMIZE || state==ENDING || pose==POSE_WAVE || pose==POSE_CHEER || calloutTime>0 || frenzyTimer>0 || ((int)(elapsed*1.8))%3!=0);
            boolean blink=(animClock+charIdx*.7)%3.4<.11;
            if(pose==POSE_FALL){
                if(leftEyeScale>.15f)c.drawCircle(fx-eyeSep,-3*u,3.7f*u*leftEyeScale,p);
                c.drawCircle(fx+eyeSep,-3*u,3.7f*u,p);
                c.drawCircle(fx,4.2f*u,2.4f*u,p);
            }else if(dizzy){
                p.setStrokeWidth(1.9f*u);p.setStrokeCap(Paint.Cap.ROUND);
                for(int side=-1;side<=1;side+=2){
                    float ex=fx+side*eyeSep,ey=-2.5f*u,r=2.6f*u;
                    c.drawLine(ex-r,ey-r,ex+r,ey+r,p);c.drawLine(ex-r,ey+r,ex+r,ey-r,p);
                }
                float wave=(float)Math.sin(clock*12)*1.2f*u;
                c.drawLine(fx-fw*.3f,4*u+wave,fx,4*u-wave,p);c.drawLine(fx,4*u-wave,fx+fw*.3f,4*u+wave,p);
            }else if(blink && !happy){
                p.setStrokeWidth(2f*u);p.setStrokeCap(Paint.Cap.ROUND);
                if(leftEyeScale>.15f)c.drawLine(fx-eyeSep-2.6f*u,-2.5f*u,fx-eyeSep+2.6f*u,-2.5f*u,p);
                c.drawLine(fx+eyeSep-2.6f*u,-2.5f*u,fx+eyeSep+2.6f*u,-2.5f*u,p);
            }else if(happy){
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.3f*u);p.setStrokeCap(Paint.Cap.ROUND);
                c.drawArc(fx-eyeSep-3*u,-6*u,fx-eyeSep+3*u,0,190,160,false,p);
                c.drawArc(fx+eyeSep-3*u,-6*u,fx+eyeSep+3*u,0,190,160,false,p);
                c.drawArc(fx-fw*.44f,-1.5f*u,fx+fw*.44f,6.5f*u,15,150,false,p);
                p.setStyle(Paint.Style.FILL);
            }else{
                if(leftEyeScale>.15f)c.drawCircle(fx-eyeSep,-2.5f*u,2.8f*u*leftEyeScale,p);
                c.drawCircle(fx+eyeSep,-2.5f*u,2.8f*u,p);
                if(t>.45f){
                    float smileAlpha=(t-.45f)/.55f;
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2f*u*smileAlpha);p.setStrokeCap(Paint.Cap.ROUND);
                    c.drawArc(fx-fw*.34f*smileAlpha,-.5f*u,fx+fw*.34f*smileAlpha,5.8f*u,20,140,false,p);
                    p.setStyle(Paint.Style.FILL);
                }
            }
        }
        if(dizzy){
            p.setColor(GOLD);
            for(int i=0;i<3;i++){
                double a=clock*5+i*2.09;
                float sx=(float)Math.cos(a)*20*u,sy=(-24+(float)Math.sin(a)*5)*u,r=3.4f*u;
                path.reset();path.moveTo(sx,sy-r);path.lineTo(sx+r,sy);path.lineTo(sx,sy+r);path.lineTo(sx-r,sy);path.close();
                c.drawPath(path,p);
            }
        }
        c.restore();
        c.restore();

        if(striding && (air01>.3f || fastTrail)){
            p.setColor(fastTrail?0xcc42e8ff:trait==1?0xcc9bdeef:0x99bceef0);
            int jets=trait==1?4:3;
            for(int i=0;i<jets;i++)c.drawCircle((i-(jets-1)/2f)*11*u,(12+i*6)*u,(3.6f-i*.5f)*(.85f+.15f*(float)Math.sin(clock*40+i))*u,p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawRobot(Canvas c){
        if(state==FALL && fall>=1.1)return;
        double angle=(lane-cameraLane)*Math.PI/4;
        double walk=state==ENDING?Math.min(1,endingTime/ENDING_WALK):0;
        double robotZ=5+12*(1-(1-walk)*(1-walk));
        float depth=(float)(8/(robotZ+3));
        float ground=cy+(float)(Math.cos(angle)*3*focal/(robotZ+3)*.924);
        float lift=(float)(Runner.height(character,air)*100*u)*depth;
        float y=ground-lift+(float)(supportDrop()*u);
        float x=cx+(float)(Math.sin(angle)*3*focal/(robotZ+3));float scale=(state==FALL?(float)(1-fall*.6):1)*depth;
        robotX=x;robotY=y;
        signalGlow=state==ENDING?Math.max(pulseAt(endingTime,1.2),pulseAt(endingTime,1.55)):0;
        if(dustTime>0){
            float prog=(float)(1-dustTime/.28);
            p.setColor(Color.argb((int)((1-prog)*210),103,223,204));
            for(int i=-2;i<=2;i++){
                if(i==0)continue;
                c.drawCircle(dustX+i*14*u*prog,dustY-6*u*prog,(4.5f-prog*3f)*u,p);
            }
        }
        float shadowScale=(float)Math.max(.45,1-Runner.height(character,air)*.45)*(1+squash*.6f)*depth;
        p.setColor(0x66000000);c.drawOval(x-27*u*shadowScale,ground-8*u*shadowScale,x+27*u*shadowScale,ground+7*u*shadowScale,p);
        float tumble=state==FALL?(float)(Math.sin(fall*9)*Math.min(1,fall*3)*22):0;
        c.save();c.translate(x,y);c.rotate(leanAnim*-16+tumble);c.scale(scale*(1+squash*.7f),scale*(1-squash));
        if(shieldActive || frenzyTimer>0){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f*u);
            p.setColor(frenzyTimer>0?0xddff9b54:0xaa42e8ff);
            c.drawCircle(0,-34*u,(44+(float)Math.sin(animClock*6)*1.6f)*u,p);p.setStyle(Paint.Style.FILL);
        }
        int pose=state==FALL?POSE_FALL:state==MENU?POSE_IDLE:state==RUN && completionNotice>1.1?POSE_CHEER:POSE_RUN;
        float airProg=air>0?(float)Math.max(0,Math.min(1,1-air/Runner.jumpSeconds(character))):0;
        drawRobotBody(c,character,stride,airPose,airProg,boostTimer>0 || frenzyTimer>0,pose);
        c.restore();
        signalGlow=0;
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean bold,Paint.Align align){p.setColor(color);p.setTextSize(size*u);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));p.setTextAlign(align);c.drawText(s,x,y,p);}
    private void drawHud(Canvas c){
        p.setColor(0xe6081722);c.drawRect(0,0,w,112*u,p);
        if(endlessMode){
            text(c,"Sonsuz Yörünge",32*u,46*u,22,GOLD,true,Paint.Align.LEFT);
            text(c,"En İyi: "+bestEndlessMeters+" m",32*u,77*u,16,0xffa7c3cb,false,Paint.Align.LEFT);
            text(c,currentMeters+" m",cx,56*u,28,INK,true,Paint.Align.CENTER);
            if(shieldActive)text(c,"Kalkan Aktif",cx,84*u,15,CYAN,true,Paint.Align.CENTER);
        }else{
            text(c,"Bölüm "+(level+1)+" / "+Track.LEVELS,32*u,46*u,22,INK,true,Paint.Align.LEFT);
            text(c,Track.name(level)+(shieldActive?" · Kalkan Aktif":""),32*u,77*u,16,shieldActive?CYAN:0xffa7c3cb,false,Paint.Align.LEFT);
            if(state==RUN && level<Track.LEVELS-1 && Track.length(level)*4-distance<70){
                text(c,"İleride: Bölüm "+(level+2),cx,cy-64*u,20,GOLD,true,Paint.Align.CENTER);
            }
            float prog=(float)Math.min(1,(distance+5)/(Track.length(level)*4));
            p.setColor(0xff274551);c.drawRoundRect(cx-130*u,38*u,cx+130*u,45*u,4*u,4*u,p);p.setColor(MINT);c.drawRoundRect(cx-130*u,38*u,cx-130*u+260*u*prog,45*u,4*u,4*u,p);
            text(c,"%"+(int)(prog*100)+" · ◆ "+totalCrystals+" (x"+comboMultiplier+")",cx,78*u,16,INK,false,Paint.Align.CENTER);
        }
        if(endlessMode){
            text(c,"◆ "+totalCrystals+" · Çarpan x"+comboMultiplier,cx,84*u,15,frenzyTimer>0?0xffff9b54:GOLD,true,Paint.Align.CENTER);
        }
        button(c,inGameMusic,audio.musicOn?"Müzik: Açık":"Müzik: Kapalı",false,UiIcons.MUSIC,!audio.musicOn);
        button(c,pause,"",false);p.setColor(INK);c.drawRect(pause.centerX()-9*u,pause.centerY()-10*u,pause.centerX()-3*u,pause.centerY()+10*u,p);c.drawRect(pause.centerX()+3*u,pause.centerY()-10*u,pause.centerX()+9*u,pause.centerY()+10*u,p);
    }
    private void button(Canvas c,RectF r,String s,boolean accent){button(c,r,s,accent,UiIcons.NONE,false);}
    private void button(Canvas c,RectF r,String s,boolean accent,int icon,boolean off){
        p.setColor(accent?GOLD:0xee173543);c.drawRoundRect(r,14*u,14*u,p);
        float font=22,iconSpace=icon==UiIcons.NONE?0:34*u;
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(font*u);
        float available=r.width()-32*u-iconSpace;
        if(p.measureText(s)>available)font=Math.max(14,font*available/p.measureText(s));
        p.setTextSize(font*u);float total=p.measureText(s)+iconSpace;
        float start=r.centerX()-total/2;
        if(icon!=UiIcons.NONE)UiIcons.draw(c,p,icon,start+12*u,r.centerY(),24*u,accent?BG:INK,off);
        Paint.FontMetrics metrics=p.getFontMetrics();
        text(c,s,start+iconSpace,r.centerY()-(metrics.ascent+metrics.descent)/2,font,accent?BG:INK,true,Paint.Align.LEFT);
    }
    private void controls(Canvas c){button(c,left,"",held(1));button(c,right,"",held(2));button(c,jump,"Zıpla",true,UiIcons.JUMP,false);arrow(c,left,-1);arrow(c,right,1);}
    private void arrow(Canvas c,RectF r,int sign){p.setColor((sign<0?held(1):held(2))?BG:INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4*u);p.setStrokeCap(Paint.Cap.ROUND);path.reset();path.moveTo(r.centerX()-sign*8*u,r.centerY()-14*u);path.lineTo(r.centerX()+sign*8*u,r.centerY());path.lineTo(r.centerX()-sign*8*u,r.centerY()+14*u);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);}
    private void overlay(Canvas c,String title,String detail,String action,String other,String hint){
        p.setColor(0xeb081722);c.drawRect(0,0,w,h,p);
        text(c,title,cx,h*.32f,48,INK,true,Paint.Align.CENTER);
        text(c,detail,cx,h*.41f,22,0xffb9d1d5,false,Paint.Align.CENTER);
        if(!hint.isEmpty())text(c,hint,cx,h*.49f,18,0xffb9d1d5,false,Paint.Align.CENTER);
        button(c,primary,action,overlayChoice==0,state==FALL || state==WIN?UiIcons.RETRY:UiIcons.PLAY,false);
        button(c,secondary,other,overlayChoice==1,UiIcons.HOME,false);
        if(state==MENU)text(c,"Açılan: "+(unlocked+1)+" / 50 bölüm",cx,h-24*u,15,0xff8eacb7,false,Paint.Align.CENTER);
    }
    private void drawAudio(Canvas c){
        button(c,trackButton,audio.musicLabel(),false,UiIcons.NEXT,false);
        button(c,musicButton,audio.musicOn?"Müzik: Açık":"Müzik: Kapalı",false,UiIcons.MUSIC,!audio.musicOn);
        button(c,effectsButton,audio.effectsOn?"Efekt: Açık":"Efekt: Kapalı",false,UiIcons.SOUND,!audio.effectsOn);
    }
    private void drawCustomize(Canvas c){
        p.setColor(0xf4081722);c.drawRect(0,0,w,h,p);
        text(c,"Kendi Karakterini Oluştur",cx,h*.14f,36,INK,true,Paint.Align.CENTER);
        // Live animated preview card on the left
        RectF preview=new RectF(cx-390*u,h*.22f,cx-170*u,h*.72f);
        p.setColor(0xff142d39);c.drawRoundRect(preview,16*u,16*u,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f*u);p.setColor(Runner.color(3,characterColors));
        c.drawRoundRect(preview,16*u,16*u,p);p.setStyle(Paint.Style.FILL);
        // The preview runs with the chosen gait and jumps every few seconds at the ability's real airtime and height.
        double jumpTime=elapsed%3.2,jumpSeconds=Runner.jumpSeconds(3);
        boolean jumping=jumpTime<jumpSeconds;
        float jumpBlend=jumping?(float)Math.min(1,Math.min(jumpTime,jumpSeconds-jumpTime)/.08):0;
        float jumpLift=jumping?(float)(Runner.height(3,jumpSeconds-jumpTime)*60*u):0;
        float feetY=preview.centerY()+50*u;
        float previewShadow=1-jumpLift/(150*u);
        p.setColor(0x66000000);c.drawOval(preview.centerX()-38*u*previewShadow,feetY-5*u,preview.centerX()+38*u*previewShadow,feetY+13*u,p);
        int previewTrait=Runner.customTrait;
        c.save();c.translate(preview.centerX(),feetY-jumpLift);c.scale(1.45f,1.45f);
        drawRobotBody(c,3,elapsed*9.6*(previewTrait==2?.85:previewTrait==1?.92:previewTrait==3?.8:1),jumpBlend,jumping?(float)(jumpTime/jumpSeconds):0,false,POSE_RUN);
        c.restore();
        text(c,"Özel Robot",preview.centerX(),preview.bottom-42*u,20,Runner.color(3,characterColors),true,Paint.Align.CENTER);
        text(c,Runner.description(3),preview.centerX(),preview.bottom-18*u,14,INK,false,Paint.Align.CENTER);

        // Right side customization rows
        text(c,"Renk:",cx-140*u,h*.24f,18,0xffb9d1d5,true,Paint.Align.LEFT);
        for(int i=0;i<6;i++){
            RectF r=customColorBtns[i];
            p.setColor(Runner.PALETTE[i]);c.drawRoundRect(r,10*u,10*u,p);
            if(Runner.customColorIndex==i){
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f*u);p.setColor(INK);
                c.drawRoundRect(r,10*u,10*u,p);p.setStyle(Paint.Style.FILL);
                UiIcons.draw(c,p,UiIcons.CHECK,r.centerX(),r.centerY(),18*u,BG,false);
            }
        }
        text(c,"Başlık / Gövde Tipi:",cx-140*u,h*.41f,18,0xffb9d1d5,true,Paint.Align.LEFT);
        for(int i=0;i<4;i++){
            RectF r=customHeadBtns[i];
            boolean sel=Runner.customHeadType==i;
            p.setColor(sel?0xff345766:0xff173543);c.drawRoundRect(r,10*u,10*u,p);
            if(sel){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f*u);p.setColor(GOLD);c.drawRoundRect(r,10*u,10*u,p);p.setStyle(Paint.Style.FILL);}
            text(c,Runner.HEAD_NAMES[i],r.centerX(),r.centerY()+6*u,16,sel?GOLD:INK,sel,Paint.Align.CENTER);
        }
        text(c,"Yetenek Gücü:",cx-140*u,h*.58f,18,0xffb9d1d5,true,Paint.Align.LEFT);
        for(int i=0;i<4;i++){
            RectF r=customTraitBtns[i];
            boolean sel=Runner.customTrait==i;
            p.setColor(sel?0xff345766:0xff173543);c.drawRoundRect(r,10*u,10*u,p);
            if(sel){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f*u);p.setColor(GOLD);c.drawRoundRect(r,10*u,10*u,p);p.setStyle(Paint.Style.FILL);}
            text(c,Runner.TRAIT_NAMES[i],r.centerX(),r.centerY()+6*u,14,sel?GOLD:INK,sel,Paint.Align.CENTER);
        }
        button(c,customSaveBtn,"Tamam",true,UiIcons.CHECK,false);
    }
    private void drawMenu(Canvas c){
        p.setColor(0xf0081722);c.drawRect(0,0,w,h,p);drawAudio(c);
        text(c,"Yörünge Koşusu",cx,h*.20f,44,INK,true,Paint.Align.CENTER);
        text(c,"Zıpla. Yön değiştir. Uzaylılardan kaç.",cx,h*.265f,20,0xffb9d1d5,false,Paint.Align.CENTER);
        for(int i=0;i<4;i++){
            boolean selected=i==character;
            int col=Runner.color(i,characterColors);
            int head=Runner.headType(i);
            RectF r=characterButtons[i];p.setStyle(Paint.Style.FILL);
            p.setColor(selected?0xff345766:0xff173543);c.drawRoundRect(r,12*u,12*u,p);
            if(selected){
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*u);p.setColor(col);
                c.drawRoundRect(r,12*u,12*u,p);p.setStyle(Paint.Style.FILL);
                float badgeX=r.right-18*u,badgeY=r.top+18*u;
                c.drawCircle(badgeX,badgeY,11*u,p);
                UiIcons.draw(c,p,UiIcons.CHECK,badgeX,badgeY,16*u,BG,false);
            }
            if(i==3){
                UiIcons.draw(c,p,UiIcons.EDIT,r.left+18*u,r.top+18*u,16*u,0xffb9d1d5,false);
            }
            float x=r.centerX();
            float hop=selected?(float)Math.sin(Math.max(0,selectPulse/.45f)*Math.PI)*12*u:0;
            c.save();c.translate(x,r.top+56*u-hop);c.scale(.62f,.62f);
            drawRobotBody(c,i,animClock*2.2+i*1.3,0,0,false,selected && animClock%4.2<1.3?POSE_WAVE:POSE_IDLE);
            c.restore();
            text(c,i==3?"Özel (Tasarım)":characterNames[i],x,r.bottom-38*u,17,selected?col:INK,selected,Paint.Align.CENTER);
            text(c,Runner.description(i),x,r.bottom-14*u,13,INK,false,Paint.Align.CENTER);
        }
        button(c,menuStart,"Başla · Bölüm "+(level+1),true,UiIcons.PLAY,false);
        button(c,endlessButton,bestEndlessMeters>0?"Sonsuz · "+bestEndlessMeters+" m":"Sonsuz Yörünge",false,UiIcons.INFINITY,false);
        text(c,Track.name(level)+(level>=20?" · Uzaylı devriyesi":""),cx,menuStart.bottom+27*u,18,0xffb9d1d5,false,Paint.Align.CENTER);
        text(c,"Bölümler",levelStrip.left,levelStrip.top-18*u,22,INK,true,Paint.Align.LEFT);
        int starCount=Long.bitCount(flawlessMask)+Long.bitCount(completedMask)*2;
        text(c,(unlocked+1)+" / 50 açık · ★ "+starCount+" · ◆ "+totalCrystals,levelStrip.right,levelStrip.top-18*u,17,GOLD,false,Paint.Align.RIGHT);
        c.save();c.clipRect(levelStrip);
        for(int i=0;i<Track.LEVELS;i++){
            float x=levelStrip.left+i*88*u-levelScroll;
            if(x+76*u<levelStrip.left || x>levelStrip.right)continue;
            p.setColor(i==level?GOLD:i<=unlocked?0xff244a58:0xff142d39);
            c.drawRoundRect(x,levelStrip.top,x+76*u,levelStrip.bottom,12*u,12*u,p);
            if(i>unlocked)UiIcons.draw(c,p,UiIcons.LOCK,x+62*u,levelStrip.top+16*u,15*u,0xffb9d1d5,false);
            else if(i==level)UiIcons.draw(c,p,UiIcons.CHECK,x+62*u,levelStrip.top+16*u,14*u,BG,false);
            text(c,Integer.toString(i+1),x+38*u,levelStrip.top+34*u,24,i==level?BG:INK,true,Paint.Align.CENTER);
            int stars=((flawlessMask&(1L<<i))!=0)?3:((completedMask&(1L<<i))!=0 || i<unlocked)?2:0;
            if(stars>0){
                for(int s=0;s<3;s++){
                    int sc=s<stars?(i==level?BG:GOLD):(i==level?0x55081722:0x558eacb7);
                    UiIcons.draw(c,p,UiIcons.STAR,x+22*u+s*16*u,levelStrip.bottom-16*u,12*u,sc,false);
                }
            }else{
                text(c,i>unlocked?"Kilitli":i==level?"Seçili":"Açık",x+38*u,levelStrip.bottom-13*u,13,i==level?BG:0xffb9d1d5,false,Paint.Align.CENTER);
            }
        }
        c.restore();text(c,menuHint,cx,h-20*u,16,0xffb9d1d5,false,Paint.Align.CENTER);
    }
    private boolean customizeTouch(MotionEvent e){
        if(e.getActionMasked()!=MotionEvent.ACTION_UP)return true;
        float x=e.getX(),y=e.getY();
        for(int i=0;i<6;i++)if(customColorBtns[i].contains(x,y)){
            Runner.customColorIndex=i;prefs.edit().putInt("customColor",i).apply();audio.play(1);return true;
        }
        for(int i=0;i<4;i++)if(customHeadBtns[i].contains(x,y)){
            Runner.customHeadType=i;prefs.edit().putInt("customHead",i).apply();audio.play(1);return true;
        }
        for(int i=0;i<4;i++)if(customTraitBtns[i].contains(x,y)){
            Runner.customTrait=i;prefs.edit().putInt("customTrait",i).apply();audio.play(1);return true;
        }
        if(customSaveBtn.contains(x,y)){
            character=3;prefs.edit().putInt("character",3).apply();audio.play(0);state=MENU;return true;
        }
        return true;
    }
    private boolean menuTouch(MotionEvent e){
        float x=e.getX(),y=e.getY();int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            if(levelStrip.contains(x,y)){stripDragging=true;stripMoved=false;dragStart=x;scrollStart=levelScroll;return true;}
        }else if(action==MotionEvent.ACTION_MOVE && stripDragging){
            if(Math.abs(x-dragStart)>8*u)stripMoved=true;
            levelScroll=Math.max(0,Math.min(maxScroll(),scrollStart+dragStart-x));return true;
        }else if(action==MotionEvent.ACTION_UP){
            if(stripDragging){
                if(!stripMoved && levelStrip.contains(x,y)){
                    float relative=x-levelStrip.left+levelScroll;int selected=(int)(relative/(88*u));
                    if(selected>=0 && selected<Track.LEVELS && relative%(88*u)<76*u){
                        if(selected<=unlocked){level=selected;menuHint="Bölüm "+(level+1)+" seçildi";audio.play(1);}
                        else menuHint="Önce Bölüm "+(unlocked+1)+" tamamlanmalı";
                    }
                }
                stripDragging=false;return true;
            }
            if(trackButton.contains(x,y))audio.nextMusic();
            else if(musicButton.contains(x,y))audio.toggleMusic();
            else if(effectsButton.contains(x,y))audio.toggleEffects();
            else if(menuStart.contains(x,y)){audio.play(0);flawlessRun=true;start();}
            else if(endlessButton.contains(x,y)){audio.play(0);startEndless();}
            else for(int i=0;i<4;i++)if(characterButtons[i].contains(x,y)){
                if(i==3){character=3;prefs.edit().putInt("character",3).apply();audio.play(1);state=CUSTOMIZE;}
                else{character=i;prefs.edit().putInt("character",character).apply();audio.play(1);selectPulse=.45f;}
            }
        }else if(action==MotionEvent.ACTION_CANCEL){stripDragging=false;}
        return true;
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        if(state==CUSTOMIZE){customizeTouch(e);invalidate();return true;}
        if(state==MENU){menuTouch(e);invalidate();return true;}
        int a=e.getActionMasked(),index=e.getActionIndex(),id=e.getPointerId(index);float x=e.getX(index),y=e.getY(index);
        if(a==MotionEvent.ACTION_DOWN || a==MotionEvent.ACTION_POINTER_DOWN){
            if(state==RUN){if(inGameMusic.contains(x,y)){audio.toggleMusic();}else if(pause.contains(x,y)){suspend();}else if(left.contains(x,y)){touches.put(id,1);turn(-1);}else if(right.contains(x,y)){touches.put(id,2);turn(1);}else if(jump.contains(x,y)){touches.put(id,3);leap();}}
            else if(a==MotionEvent.ACTION_DOWN && state!=ENDING && (state!=FALL || fall>1.1)){
                if(state==PAUSE && trackButton.contains(x,y)){audio.nextMusic();invalidate();return true;}
                if(state==PAUSE && musicButton.contains(x,y)){audio.toggleMusic();invalidate();return true;}
                if(state==PAUSE && effectsButton.contains(x,y)){audio.toggleEffects();invalidate();return true;}
                if(primary.contains(x,y)){
                    if(state==PAUSE){state=RUN;last=0;}
                    else if(endlessMode){startEndless();}
                    else{if(state==WIN)level=0;start();}
                }
                else if(secondary.contains(x,y)){menu();}
            }
        }else if(a==MotionEvent.ACTION_UP || a==MotionEvent.ACTION_POINTER_UP){touches.delete(id);performClick();}
        else if(a==MotionEvent.ACTION_CANCEL)touches.clear();
        else if(a==MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++){int pid=e.getPointerId(i),v=touches.get(pid);RectF r=v==1?left:v==2?right:jump;if(v!=0 && !r.contains(e.getX(i),e.getY(i)))touches.delete(pid);}}
        invalidate();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int code,KeyEvent e){
        boolean confirm=code==KeyEvent.KEYCODE_SPACE || code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_DPAD_CENTER;
        boolean leftKey=code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_A;
        boolean rightKey=code==KeyEvent.KEYCODE_DPAD_RIGHT || code==KeyEvent.KEYCODE_D;
        boolean upKey=code==KeyEvent.KEYCODE_DPAD_UP || code==KeyEvent.KEYCODE_W;
        boolean downKey=code==KeyEvent.KEYCODE_DPAD_DOWN || code==KeyEvent.KEYCODE_S;
        if(state==RUN){
            if(code==KeyEvent.KEYCODE_SPACE || upKey){if(e.getRepeatCount()==0)leap();return true;}
            if(leftKey){keyLeft=true;return true;}
            if(rightKey){keyRight=true;return true;}
            if(code==KeyEvent.KEYCODE_ESCAPE){suspend();return true;}
        }else if(state==MENU){
            if(leftKey && level>0){level--;centerSelection();audio.play(1);invalidate();return true;}
            if(rightKey && level<unlocked){level++;centerSelection();audio.play(1);invalidate();return true;}
            if(confirm && e.getRepeatCount()==0){audio.play(0);flawlessRun=true;start();return true;}
        }else if(state==CUSTOMIZE){
            if(confirm || code==KeyEvent.KEYCODE_ESCAPE){character=3;prefs.edit().putInt("character",3).apply();audio.play(0);state=MENU;invalidate();return true;}
        }else if(state==PAUSE || (state==FALL && fall>1.1) || state==WIN){
            if(upKey || downKey || leftKey || rightKey){overlayChoice=1-overlayChoice;audio.play(1);invalidate();return true;}
            if(confirm && e.getRepeatCount()==0){
                if(overlayChoice==0){
                    if(state==PAUSE){state=RUN;last=0;}
                    else if(endlessMode){startEndless();}
                    else{if(state==WIN)level=0;start();}
                }else{menu();}
                invalidate();return true;
            }
        }
        return super.onKeyDown(code,e);
    }
    @Override public boolean onKeyUp(int code,KeyEvent e){if(code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_A){keyLeft=false;return true;}if(code==KeyEvent.KEYCODE_DPAD_RIGHT || code==KeyEvent.KEYCODE_D){keyRight=false;return true;}return super.onKeyUp(code,e);}
}
