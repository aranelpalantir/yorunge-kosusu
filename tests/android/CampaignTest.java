package com.yorunge.tests;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import java.lang.reflect.*;

public class CampaignTest extends Instrumentation {
    private Object game;
    private void set(String name,Object value)throws Exception{Field f=game.getClass().getDeclaredField(name);f.setAccessible(true);f.set(game,value);}
    private double number(String name)throws Exception{Field f=game.getClass().getDeclaredField(name);f.setAccessible(true);return f.getDouble(game);}
    private void touch(int action,int... controls)throws Exception{
        android.view.MotionEvent.PointerProperties[] properties=new android.view.MotionEvent.PointerProperties[controls.length];
        android.view.MotionEvent.PointerCoords[] coords=new android.view.MotionEvent.PointerCoords[controls.length];
        for(int i=0;i<controls.length;i++){
            Field f=game.getClass().getDeclaredField(controls[i]==1?"left":controls[i]==2?"right":"jump");f.setAccessible(true);
            android.graphics.RectF r=(android.graphics.RectF)f.get(game);
            properties[i]=new android.view.MotionEvent.PointerProperties();properties[i].id=i;properties[i].toolType=1;
            coords[i]=new android.view.MotionEvent.PointerCoords();coords[i].x=r.centerX();coords[i].y=r.centerY();coords[i].pressure=1;coords[i].size=1;
        }
        long t=android.os.SystemClock.uptimeMillis();
        android.view.MotionEvent event=android.view.MotionEvent.obtain(t,t,action,controls.length,properties,coords,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
        ((android.view.View)game).onTouchEvent(event);event.recycle();
    }
    private Object object(String name)throws Exception{Field f=game.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(game);}
    private void point(int action,float x,float y){long t=android.os.SystemClock.uptimeMillis();android.view.MotionEvent event=android.view.MotionEvent.obtain(t,t,action,x,y,0);((android.view.View)game).onTouchEvent(event);event.recycle();}
    private void tap(android.graphics.RectF r){point(0,r.centerX(),r.centerY());point(1,r.centerX(),r.centerY());}
    private int get(String name)throws Exception{Field f=game.getClass().getDeclaredField(name);f.setAccessible(true);return f.getInt(game);}
    private void call(String name)throws Exception{Method m=game.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(game);}
    private void tick(double dt)throws Exception{Method m=game.getClass().getDeclaredMethod("update",double.class);m.setAccessible(true);m.invoke(game,dt);}
    private int track(String method,int... args)throws Exception{
        Class<?> cls=getTargetContext().getClassLoader().loadClass("com.yorunge.runner.Track");Class<?>[] types=new Class<?>[args.length];Object[] values=new Object[args.length];
        for(int i=0;i<args.length;i++){types[i]=int.class;values[i]=args[i];}
        return (Integer)cls.getMethod(method,types).invoke(null,values);
    }
    private void check(boolean v,String msg){if(!v)throw new AssertionError(msg);}
    @Override public void onCreate(Bundle b){super.onCreate(b);start();}
    @Override public void onStart(){
        final Bundle result=new Bundle();
        final SharedPreferences prefs=getTargetContext().getSharedPreferences("orbit",0);
        final int oldMusicIndex=prefs.getInt("musicIndex",0);
        final long oldSignals=prefs.getLong("signalsSeen",0);
        final int original=prefs.getInt("unlocked",0),oldCharacter=prefs.getInt("character",0);
        final boolean oldMusic=prefs.getBoolean("music",true),oldEffects=prefs.getBoolean("effects",true);
        try {
            Intent intent=new Intent().setClassName(getTargetContext(),"com.yorunge.runner.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final Activity activity=startActivitySync(intent);
            waitForIdleSync();android.os.SystemClock.sleep(600);
            runOnMainSync(new Runnable(){public void run(){try{
                Field f=activity.getClass().getDeclaredField("game");f.setAccessible(true);game=f.get(activity);
                set("unlocked",0);set("level",49);call("start");check(get("level")==0,"Locked level bypass");
                for(int level=0;level<50;level++){
                    set("unlocked",level);set("level",level);call("start");
                    set("distance",track("length",level)*4-5-.01);tick(.02);
                    check(get("unlocked")==Math.min(49,level+1),"Wrong saved unlock");
                    check(prefs.getInt("unlocked",-1)==Math.min(49,level+1),"Unlock not saved");
                    check(get("state")== (level==49?6:1),"Intermediate popup or final overflow");
                    if(level==49){tick(4);check(get("state")==4,"Final scene must end on the win screen");}
                    check(get("level")==Math.min(49,level+1),"Not automatic next");
                    check(get("completedLevel")==level && number("completionNotice")>0,"Missing completion banner");
                }
                for(int kind=1;kind<=2;kind++){
                    int row=-1,lane=-1;
                    search:for(int r=6;r<track("length",0);r++)for(int l=0;l<8;l++)if(track("tile",0,r,l)==kind){row=r;lane=l;break search;}
                    check(row>=0,"Missing hazard kind");
                    set("level",0);call("start");set("distance",row*4-5+.1);set("air",0.0);set("lane",(double)lane);set("targetLane",lane);tick(0);
                    if(kind==1)check(get("state")==3,"Hole did not cause immediate fall");
                    else {
                        check(get("state")==1,"Red must allow a short escape window");
                        tick(.1);call("leap");check(get("state")==1 && number("air")>0,"Early red escape failed");
                        call("start");set("distance",row*4-5+.1);set("air",0.0);set("lane",(double)lane);set("targetLane",lane);tick(0);
                        set("elapsed",.25);tick(0);check(get("state")==3,"Late red escape allowed");
                        call("leap");check(get("state")==3 && number("air")==0,"Late jump rescued a fall");
                        tick(.035);check(((Float)object("fallCamera"))>0,"Fall camera did not follow");
                        check(get("fallingRow")==row && get("fallingLane")==lane,"Red plate did not detach");
                    }
                    call("start");set("distance",row*4-5+.1);set("air",.4);set("lane",(double)lane);set("targetLane",lane);tick(0);
                    check(get("state")==1,"Jump did not clear hazard");
                }
                // Two actual touch pointers: jump held while a second finger turns left/right.
                for(int direction=1;direction<=2;direction++){
                    call("start");set("distance",10.0);set("air",0.0);
                    touch(0,3);touch(5 | (1<<8),3,direction);tick(.035);
                    check(number("air")>0,"Second finger cancelled jump");
                    check(direction==1?number("lane")<0:number("lane")>0,"Air steering reversed or ignored");
                    touch(6 | (1<<8),3,direction);touch(1,3);
                }
                // Reverse order: direction held before jump.
                call("start");set("distance",10.0);set("air",0.0);
                touch(0,1);touch(5 | (1<<8),1,3);tick(.035);
                check(number("air")>0 && number("lane")<0,"Direction-first multi-touch failed");
                touch(3,1,3);
                Object liveAudio=object("audio");boolean beforeLiveMute=liveAudio.getClass().getField("musicOn").getBoolean(liveAudio);
                tap((android.graphics.RectF)object("inGameMusic"));
                check(get("state")==1,"Music control paused gameplay");
                check(liveAudio.getClass().getField("musicOn").getBoolean(liveAudio)!=beforeLiveMute,"In-game music toggle failed");
                call("suspend");check(get("state")==2,"Pause failed");
                // Alien collision restarts this level without advancing/unlocking anything.
                set("level",20);set("unlocked",20);call("start");set("elapsed",0.0);set("distance",(track("squeezeEnd",20,0)+1.5)*4-5);set("lane",(double)track("route",20,0));set("targetLane",track("route",20,0));set("air",0.0);tick(0);
                check(get("state")==1 && get("level")==20 && number("distance")==0,"Alien must restart same level");
                check(get("unlocked")==20 && number("restartNotice")>0,"Alien must preserve locks and show feedback");
                call("menu");
                android.graphics.RectF strip=(android.graphics.RectF)object("levelStrip");
                point(0,strip.right-30,strip.centerY());point(2,strip.left+30,strip.centerY());point(1,strip.left+30,strip.centerY());
                check(((Float)object("levelScroll"))>0,"Carousel did not scroll");
                float u=(Float)object("u");set("levelScroll",17*88*u);
                point(0,strip.left+35*u,strip.centerY());point(1,strip.left+35*u,strip.centerY());
                check(get("level")==17,"Open carousel selection failed");
                set("levelScroll",25*88*u);point(0,strip.left+35*u,strip.centerY());point(1,strip.left+35*u,strip.centerY());
                check(get("level")==17,"Locked carousel selection allowed");
                android.graphics.RectF[] characters=(android.graphics.RectF[])object("characterButtons");tap(characters[2]);
                check(get("character")==2 && prefs.getInt("character",-1)==2,"Character not persisted");
                Object sound=object("audio");Class<?> audioClass=sound.getClass();
                boolean musicBefore=prefs.getBoolean("music",true);tap((android.graphics.RectF)object("musicButton"));
                check(prefs.getBoolean("music",true)!=musicBefore,"Music toggle not persisted");
                if(!(Boolean)audioClass.getField("musicOn").get(sound))audioClass.getMethod("toggleMusic").invoke(sound);
                audioClass.getMethod("foreground").invoke(sound);
                check((Boolean)audioClass.getMethod("musicPlaying").invoke(sound),"Music failed to play");
                int selectedMusic=prefs.getInt("musicIndex",0);
                for(int index=0;index<6;index++){
                    tap((android.graphics.RectF)object("trackButton"));
                    check(prefs.getInt("musicIndex",-1)==(selectedMusic+index+1)%6,"Track selection not saved or wrap failed");
                    check((Boolean)audioClass.getMethod("musicPlaying").invoke(sound),"Selected music failed to play");
                }
                audioClass.getMethod("toggleMusic").invoke(sound);
                tap((android.graphics.RectF)object("trackButton"));
                check(!(Boolean)audioClass.getMethod("musicPlaying").invoke(sound),"Selecting track must not unmute");
                audioClass.getMethod("toggleMusic").invoke(sound);
                audioClass.getMethod("background").invoke(sound);
                check(!(Boolean)audioClass.getMethod("musicPlaying").invoke(sound),"Music continued in background");
                audioClass.getMethod("foreground").invoke(sound);
                boolean effectsBefore=prefs.getBoolean("effects",true);tap((android.graphics.RectF)object("effectsButton"));
                check(prefs.getBoolean("effects",true)!=effectsBefore,"Effects toggle failed");
                set("unlocked",49);set("level",4);call("start");set("signalsSeen",0L);
                set("distance",(track("length",4)-3)*4-5+.1);tick(0);
                check(number("signalTime")>0 && !((String)object("signalText")).isEmpty(),"Missing quiet signal");
                check((prefs.getLong("signalsSeen",0)&(1L<<4))!=0,"Signal not remembered");
                set("signalTime",0.0);tick(0);check(number("signalTime")==0,"Signal repeated every frame");
                // Native Canvas render of a late-course alien encounter for visual QA.
                set("unlocked",49);set("level",20);call("start");set("distance",(track("squeezeEnd",20,0)+1.5)*4-8);set("lane",(double)track("route",20,0));set("targetLane",track("route",20,0));set("cameraLane",(double)track("route",20,0));set("air",0.0);set("elapsed",0.0);
                android.view.View view=(android.view.View)game;
                android.graphics.Bitmap screenshot=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                view.draw(new android.graphics.Canvas(screenshot));
                java.io.File shot=new java.io.File(getTargetContext().getExternalFilesDir(null),"alien-v4.png");
                java.io.FileOutputStream output=new java.io.FileOutputStream(shot);screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);output.close();screenshot.recycle();
                result.putString("stream","PASS: 50 real GameView completions, automatic transitions, saved unlocks, locked selection, hole/red collision airborne clearance, two-finger steering, alien restart, carousel selection/locks, character persistence, music playback/toggles/lifecycle and effects toggle.\n");
            }catch(Throwable t){result.putString("failure",t.toString());}finally{prefs.edit().putInt("unlocked",original).putInt("character",oldCharacter).putBoolean("music",oldMusic).putBoolean("effects",oldEffects).putLong("signalsSeen",oldSignals).putInt("musicIndex",oldMusicIndex).commit();activity.finish();}}});
            finish(result.containsKey("failure")?Activity.RESULT_CANCELED:Activity.RESULT_OK,result);
        }catch(Throwable t){prefs.edit().putInt("unlocked",original).putInt("character",oldCharacter).putBoolean("music",oldMusic).putBoolean("effects",oldEffects).putLong("signalsSeen",oldSignals).putInt("musicIndex",oldMusicIndex).commit();result.putString("failure",t.toString());finish(Activity.RESULT_CANCELED,result);}
    }
}
