package com.yorunge.runner;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.*;
import java.util.HashSet;

/** Bundled original audio: no network or storage permission. */
public final class GameAudio {
    private final SharedPreferences prefs;
    private final Context context;
    private final AudioAttributes attributes;
    private static final String[] MUSIC_FILES={"orbit.wav","ion.wav","drift.wav","turkish_march.wav","ode.wav","waltz.wav","pulsar.wav","nebula.wav","elise.wav"};
    private static final String[] MUSIC_NAMES={"Yörünge","İyon","Gece Uçuşu","Türk Marşı","Neşeye Övgü","Yıldız Valsi","Süpernova","Kristal","Für Elise"};
    private int musicIndex;
    private final SoundPool pool;
    private final AudioManager manager;
    private final AudioFocusRequest focus;
    private final HashSet<Integer> ready=new HashSet<>();
    private MediaPlayer music;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable restoreVolume=new Runnable(){public void run(){if(!released && music!=null)music.setVolume(.32f,.32f);}};
    private final int[] sounds=new int[6];
    public boolean musicOn, effectsOn;
    private boolean foreground=false,hasFocus=false,released=false;
    public GameAudio(Context context,SharedPreferences prefs){
        this.context=context.getApplicationContext();this.prefs=prefs;musicOn=prefs.getBoolean("music",true);effectsOn=prefs.getBoolean("effects",true);
        musicIndex=Math.max(0,Math.min(MUSIC_FILES.length-1,prefs.getInt("musicIndex",0)));
        attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        manager=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        pool=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build();
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener(){public void onLoadComplete(SoundPool p,int id,int status){if(status==0)ready.add(id);}});
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener(){public void onAudioFocusChange(int change){
            hasFocus=change==AudioManager.AUDIOFOCUS_GAIN;
            if(!hasFocus)pool.autoPause();
            sync();
        }}).build();
        try {
            String[] names={"jump.wav","turn.wav","hit.wav","finish.wav","coin.wav","frenzy.wav"};
            for(int i=0;i<names.length;i++)try(AssetFileDescriptor fd=context.getAssets().openFd(names[i])){sounds[i]=pool.load(fd,1);}
            loadMusic(musicIndex);
        }catch(Exception e){android.util.Log.e("OrbitAudio","Could not load bundled audio",e);if(music!=null){music.release();music=null;}}
    }
    private boolean loadMusic(int index){
        MediaPlayer replacement=new MediaPlayer();
        try {
            replacement.setAudioAttributes(attributes);
            try(AssetFileDescriptor fd=context.getAssets().openFd(MUSIC_FILES[index])){replacement.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());}
            replacement.setLooping(true);replacement.setVolume(.32f,.32f);replacement.prepare();
            if(music!=null)music.release();music=replacement;musicIndex=index;return true;
        }catch(Exception e){replacement.release();android.util.Log.e("OrbitAudio","Could not change music",e);return false;}
    }
    public void nextMusic(){if(released)return;if(loadMusic((musicIndex+1)%MUSIC_FILES.length)){prefs.edit().putInt("musicIndex",musicIndex).apply();acquire();sync();}}
    public String musicLabel(){return "Parça: "+MUSIC_NAMES[musicIndex]+" · "+(musicIndex+1)+"/"+MUSIC_FILES.length;}
    private void sync(){if(released || music==null)return;if(foreground && hasFocus && musicOn){if(!music.isPlaying())music.start();}else if(music.isPlaying())music.pause();}
    private void acquire(){if(foreground && (musicOn || effectsOn) && !hasFocus)hasFocus=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
    public void foreground(){if(released)return;foreground=true;acquire();sync();}
    public void background(){if(released)return;foreground=false;sync();pool.autoPause();manager.abandonAudioFocusRequest(focus);hasFocus=false;}
    public void toggleMusic(){musicOn=!musicOn;prefs.edit().putBoolean("music",musicOn).apply();acquire();sync();}
    public void toggleEffects(){effectsOn=!effectsOn;prefs.edit().putBoolean("effects",effectsOn).apply();if(!effectsOn)pool.autoPause();acquire();}
    public void play(int sound){
        if(!released && foreground && hasFocus && effectsOn && ready.contains(sounds[sound])){
            float gain=sound==2?.64f:.55f;
            pool.play(sounds[sound],gain,gain,sound==2?3:1,0,1);
            if(sound==2 && music!=null){music.setVolume(.14f,.14f);handler.removeCallbacks(restoreVolume);handler.postDelayed(restoreVolume,850);}
        }
    }
    public boolean musicPlaying(){return music!=null && !released && music.isPlaying();}
    public void release(){if(released)return;background();released=true;handler.removeCallbacks(restoreVolume);pool.release();if(music!=null)music.release();}
}
