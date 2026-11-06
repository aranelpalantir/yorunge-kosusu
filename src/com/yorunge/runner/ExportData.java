package com.yorunge.runner;

import java.nio.charset.StandardCharsets;

/** Exports exact Java Track, Story, Runner, and Enemy data as JSON so the iOS PWA stays 100% synchronized with the Android Java source. */
public final class ExportData {
    private static String jsStr(String s){
        StringBuilder out=new StringBuilder("\"");
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(c=='"' || c=='\\')out.append('\\').append(c);
            else if(c>127)out.append(String.format("\\u%04x",(int)c));
            else out.append(c);
        }
        return out.append("\"").toString();
    }
    public static void main(String[] args) throws Exception {
        StringBuilder sb=new StringBuilder();
        sb.append("window.ORBIT_DATA={\n");
        sb.append("  jumpSeconds:").append(Track.JUMP_SECONDS).append(",\n");
        sb.append("  escapeSeconds:").append(Collapse.ESCAPE_SECONDS).append(",\n");
        sb.append("  palette:[");
        for(int i=0;i<Runner.PALETTE.length;i++){
            if(i>0)sb.append(",");
            sb.append(String.format("\"#%06x\"",Runner.PALETTE[i]&0xffffff));
        }
        sb.append("],\n");
        sb.append("  headNames:[");
        for(int i=0;i<Runner.HEAD_NAMES.length;i++){
            if(i>0)sb.append(",");
            sb.append(jsStr(Runner.HEAD_NAMES[i]));
        }
        sb.append("],\n");
        sb.append("  traitNames:[");
        for(int i=0;i<Runner.TRAIT_NAMES.length;i++){
            if(i>0)sb.append(",");
            sb.append(jsStr(Runner.TRAIT_NAMES[i]));
        }
        sb.append("],\n");
        sb.append("  levels:[\n");
        for(int l=0;l<Track.LEVELS;l++){
            if(l>0)sb.append(",\n");
            sb.append("    {name:").append(jsStr(Track.name(l))).append(",");
            sb.append("length:").append(Track.length(l)).append(",");
            sb.append("speed:").append(Track.speed(l)).append(",");
            sb.append("color:\"").append(String.format("#%06x",Track.color(l)&0xffffff)).append("\",");
            sb.append("rgb:[").append((Track.color(l)>>16)&255).append(",").append((Track.color(l)>>8)&255).append(",").append(Track.color(l)&255).append("],");
            sb.append("shieldRow:").append(Track.shieldRow(l)).append(",");
            sb.append("shieldLane:").append(Track.shieldLane(l)).append(",");
            sb.append("markRow:").append(Story.markRow(l)).append(",");
            sb.append("fragment:").append(jsStr(Story.fragment(l))).append(",");
            sb.append("crystals:[");
            for(int r=0;r<Track.length(l);r++){
                if(r>0)sb.append(",");
                sb.append(Track.crystalLane(l,r));
            }
            sb.append("],enemies:[");
            boolean firstE=true;
            for(int s=0;s<Track.sectionCount(l);s++){
                if(Enemy.active(l,s)){
                    if(!firstE)sb.append(",");
                    firstE=false;
                    sb.append("{section:").append(s)
                      .append(",lane:").append(Enemy.lane(l,s))
                      .append(",first:").append(Track.squeezeEnd(l,s)+1.5)
                      .append(",last:").append(Track.sectionEnd(l,s)-1.5)
                      .append(",period:").append(5.2-(l-20)*.035)
                      .append("}");
                }
            }
            sb.append("],tiles:[");
            for(int r=0;r<Track.length(l);r++){
                if(r>0)sb.append(",");
                sb.append("[");
                for(int side=0;side<8;side++){
                    if(side>0)sb.append(",");
                    sb.append(Track.tile(l,r,side));
                }
                sb.append("]");
            }
            sb.append("]}");
        }
        sb.append("\n  ]\n};\n");
        System.out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}
