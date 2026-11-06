package com.yorunge.runner;

import android.graphics.*;

/** Small original geometric icons, in a shared 24-unit coordinate space. */
public final class UiIcons {
    public static final int NONE=0,PLAY=1,RETRY=2,HOME=3,JUMP=4,MUSIC=5,SOUND=6,LOCK=7,CHECK=8,NEXT=9,STAR=10,SHIELD=11,INFINITY=12,EDIT=13;
    public static void draw(Canvas c,Paint p,int icon,float x,float y,float size,int color,boolean off){
        c.save();c.translate(x-size/2,y-size/2);c.scale(size/24,size/24);
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        Path path=new Path();
        switch(icon){
            case PLAY:case NEXT:
                path.moveTo(7,4);path.lineTo(19,12);path.lineTo(7,20);path.close();c.drawPath(path,p);
                if(icon==NEXT)c.drawLine(22,5,22,19,p);break;
            case RETRY:
                c.drawArc(4,4,20,20,205,300,false,p);
                path.moveTo(4.75f,3);path.lineTo(4.75f,8.62f);path.lineTo(10.4f,8.62f);c.drawPath(path,p);break;
            case HOME:
                path.moveTo(2,11);path.lineTo(12,3);path.lineTo(22,11);path.moveTo(5,9);path.lineTo(5,21);path.lineTo(10,21);path.lineTo(10,15);path.lineTo(14,15);path.lineTo(14,21);path.lineTo(19,21);path.lineTo(19,9);c.drawPath(path,p);break;
            case JUMP:
                c.drawLine(12,18,12,4,p);c.drawLine(12,4,6,10,p);c.drawLine(12,4,18,10,p);c.drawLine(4,22,20,22,p);break;
            case MUSIC:
                c.drawLine(9,17,9,5,p);c.drawLine(9,5,20,3,p);c.drawLine(20,3,20,15,p);c.drawOval(3,15,9,21,p);c.drawOval(14,13,20,19,p);break;
            case SOUND:
                path.moveTo(3,9);path.lineTo(7,9);path.lineTo(13,4);path.lineTo(13,20);path.lineTo(7,15);path.lineTo(3,15);path.close();c.drawPath(path,p);
                if(!off){c.drawArc(12,7,20,17,-65,130,false,p);c.drawArc(10,3,24,21,-55,110,false,p);}break;
            case LOCK:
                c.drawRoundRect(5,10,19,22,2,2,p);c.drawArc(8,2,16,16,180,180,false,p);c.drawLine(8,8,8,10,p);c.drawLine(16,8,16,10,p);c.drawLine(12,15,12,18,p);break;
            case CHECK:
                path.moveTo(4,12);path.lineTo(10,18);path.lineTo(21,5);c.drawPath(path,p);break;
            case STAR:
                p.setStyle(Paint.Style.FILL);
                for(int i=0;i<10;i++){
                    double a=i*Math.PI/5-Math.PI/2,r=i%2==0?10:4.2;
                    float sx=12+(float)(Math.cos(a)*r),sy=12+(float)(Math.sin(a)*r);
                    if(i==0)path.moveTo(sx,sy);else path.lineTo(sx,sy);
                }
                path.close();c.drawPath(path,p);p.setStyle(Paint.Style.STROKE);break;
            case SHIELD:
                path.moveTo(12,3);path.lineTo(20,6);path.lineTo(19,14);path.lineTo(12,21);path.lineTo(5,14);path.lineTo(4,6);path.close();c.drawPath(path,p);break;
            case INFINITY:
                path.moveTo(12,12);
                path.cubicTo(9,7,3,7,3,12);
                path.cubicTo(3,17,9,17,12,12);
                path.cubicTo(15,7,21,7,21,12);
                path.cubicTo(21,17,15,17,12,12);
                c.drawPath(path,p);break;
            case EDIT:
                c.drawCircle(12,12,4,p);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;
                    c.drawLine(12+(float)Math.cos(a)*6.5f,12+(float)Math.sin(a)*6.5f,12+(float)Math.cos(a)*9.5f,12+(float)Math.sin(a)*9.5f,p);
                }
                break;
        }
        if(off)c.drawLine(3,3,21,21,p);
        p.setStyle(Paint.Style.FILL);c.restore();
    }
}
