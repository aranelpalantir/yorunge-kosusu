package com.yorunge.runner;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private GameView game;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        game = new GameView(this);
        setContentView(game);
    }
    @Override protected void onPause() { super.onPause(); game.suspend(); game.audioBackground(); }
    @Override protected void onResume() { super.onResume(); if (game != null) {game.resumeClock();game.audioForeground();} }
    @Override protected void onDestroy() { if(game!=null)game.releaseAudio();super.onDestroy(); }
    @Override public void onBackPressed() { if (!game.back()) super.onBackPressed(); }
}
