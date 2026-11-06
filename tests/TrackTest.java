import com.yorunge.runner.Track;
import com.yorunge.runner.Runner;
import com.yorunge.runner.Progress;
import com.yorunge.runner.Enemy;
import com.yorunge.runner.Story;
import com.yorunge.runner.Endless;
import java.util.HashSet;

public class TrackTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void simulate(int level,double dt,int character){
        double distance=0,air=Runner.jumpSeconds(character),lane=0,cooldown=0;int target=0,jumped=-1,recoveryJump=-1;
        while((distance+5)/4<Track.length(level)){
            int section=Track.sectionAt(level,(int)((distance+5)/4));
            int currentRow=(int)((distance+5)/4);
            boolean evade=Enemy.active(level,section) && distance>=(Track.squeezeEnd(level,section)+1)*4-5-Runner.speed(character,level)*.15;
            if(evade && recoveryJump!=section && air<=0){air=Runner.jumpSeconds(character);recoveryJump=section;}
            int route=Track.wrap(Track.route(level,section)+(evade?1:0));
            cooldown-=dt;
            int delta=Track.wrap(route-target);if(delta>4)delta-=8;
            if(delta!=0 && cooldown<=0){target+=delta>0?1:-1;cooldown=.22;}
            double takeoff=Track.ringRow(level,section)*4-5-Runner.speed(character,level)*.15;
            if(distance>=takeoff && jumped!=section && air<=0){air=Runner.jumpSeconds(character);jumped=section;}
            lane+=(target-lane)*Math.min(1,dt*16);
            air=Math.max(0,air-dt);distance+=dt*Runner.speed(character,level);
            int row=(int)((distance+5)/4);
            for(int enemy=0;enemy<Track.sectionCount(level);enemy++)check(!Enemy.hitAtHeight(level,enemy,distance/Runner.speed(character,level),distance,lane,Runner.height(character,air)),"Solver hit alien level "+level);
            check(!Runner.lethal(character,Track.tile(level,row,(int)Math.round(lane)),air),"Solver collision level "+level+" row "+row+" lane "+lane+" dt "+dt);
        }
    }
    public static void main(String[] args) {
        check(Math.abs(Runner.height(1,Runner.jumpSeconds(1)/2)-1.25)<.00001,"Nova jump height");
        check(Runner.jumpSeconds(1)>Runner.jumpSeconds(0),"Nova airtime");
        check(Math.abs(Runner.speed(2,0)/Runner.speed(0,0)-1.10)<.00001,"Atlas speed");
        for(int trait=0;trait<3;trait++){
            Runner.customTrait=trait;
            check(Runner.jumpSeconds(3)==Runner.jumpSeconds(trait) && Runner.speed(3,0)==Runner.speed(trait,0) && Runner.height(3,.4)==Runner.height(trait,.4),"Custom ability must match preset "+trait);
        }
        Runner.customTrait=3;
        check(Math.abs(Runner.jumpSeconds(3)/Runner.jumpSeconds(0)-1.15)<.00001,"Glide airtime");
        check(Runner.speed(3,0)==Runner.speed(0,0),"Glide must not add speed");
        check(Math.abs(Runner.height(3,Runner.jumpSeconds(3)/2)-1)<.00001,"Glide must not add height");
        for(int level=1;level<50;level++){
            if(level%4==0)check(Track.speed(level)>Track.speed(level-1),"Missing four-level speed increase");
            else check(Track.speed(level)==Track.speed(level-1),"Speed changed between milestones");
        }
        HashSet<String> layouts=new HashSet<>();
        HashSet<String> openingPatterns=new HashSet<>();
        for(int level=0;level<Track.LEVELS;level++) {
            int holes=0,red=0;
            StringBuilder layout=new StringBuilder();
            for(int row=0;row<Track.length(level);row++) {
                int safe=0;
                for(int lane=0;lane<8;lane++) {
                    int tile=Track.tile(level,row,lane);layout.append(tile);
                    check(tile==Track.tile(level,row,lane-16),"Negative wrap");
                    if(tile==Track.HOLE)holes++;
                    if(tile==Track.RED)red++;
                    if(tile==Track.FLOOR)safe++;
                    if(row<2)check(tile==Track.FLOOR,"Unsafe spawn");
                    if(row==2)check(tile==(level==0?Track.WALL:Track.FLOOR),"Initial wall placement");
                    
                }
                if(row==Track.ringRow(level,0))check(safe==0,"Jump ring can be bypassed");
            }
            StringBuilder opening=new StringBuilder();
            for(int r=6;r<40;r++)for(int wall=0;wall<8;wall++)opening.append(Track.tile(level,r,wall));
            check(openingPatterns.add(opening.toString()),"Courses differ only in length");
            if(level>0)check(Track.color(level)!=Track.color(level-1),"Next-course preview must have a different palette");
            check(holes>10 && red>10,"Insufficient hazard density");
            check(layouts.add(layout.toString()),"Duplicate course");
            for(int fixedLane=0;fixedLane<8;fixedLane++) {
                int run=0,maxRun=0;
                for(int row=6;row<Track.length(level)-2;row++){
                    int t=Track.tile(level,row,fixedLane);
                    if(t==Track.HOLE || t==Track.RED)run++;else run=0;
                    maxRun=Math.max(maxRun,run);
                }
                for(int character=0;character<3;character++)check(maxRun*4>Runner.speed(character,level)*Runner.jumpSeconds(character),"Fixed-wall jumping can finish");
            }
            for(double step:new double[]{1.0/60,1.0/30,.035}){
                for(int character=0;character<3;character++)simulate(level,step,character);
                for(int trait=0;trait<4;trait++){Runner.customTrait=trait;simulate(level,step,3);}
            }
            // Simulate actual launch physics at 60 and 30 FPS, including the full wall width.
            for(double dt:new double[]{1.0/60,1.0/30,.035}) {
                double distance=0,air=Track.JUMP_SECONDS;
                while(distance<15) {
                    air=Math.max(0,air-dt);distance+=dt*Track.speed(level);
                    int row=(int)((distance+5)/4);
                    check(!Track.lethal(Track.tile(level,row,0),air),"Launch collision level "+level);
                }
            }
        }
        check(!Enemy.active(19,0) && Enemy.active(20,0),"Alien difficulty threshold");
        for(int level=20;level<50;level++)for(int section=0;section<Track.sectionCount(level);section++)if(Enemy.active(level,section)){
            int lane=Enemy.lane(level,section);
            check(Track.tile(level,Track.squeezeEnd(level,section)+1,lane)==Track.HOLE && Track.tile(level,Track.sectionEnd(level,section)-2,lane)==Track.HOLE,"Patrol endpoints must be holes");
            double z=Enemy.position(level,section,1.7);
            check(Enemy.hit(level,section,1.7,z-5,lane,0),"Alien grounded collision");
            check(!Enemy.hit(level,section,1.7,z-5,lane,.46),"Alien jump clearance");
            check(!Enemy.hit(level,section,1.7,z-5,lane+1,0),"Alien side dodge");
        }
        int fragments=0;
        for(int level=0;level<50;level++){
            check(!Story.name(level).isEmpty(),"Missing location name");
            if(!Story.fragment(level).isEmpty()){fragments++;int mark=Story.markRow(level);check(Track.length(level)-mark>=8,"Signal needs reading time before the exit");for(int row=mark;row<Track.length(level);row++){int hazards=0;for(int lane=0;lane<8;lane++)if(Track.tile(level,row,lane)!=Track.FLOOR)hazards++;check(hazards<=1,"Signal must be in safe exit level "+level+" row "+row+" mark "+mark+" hazards "+hazards);}check(Track.tile(level,mark,level+2)==Track.FLOOR,"Mark panel must be drawn on floor");}
            HashSet<Integer> intervals=new HashSet<>();
            for(int section=1;section<Track.sectionCount(level);section++)intervals.add(Track.ringRow(level,section)-Track.ringRow(level,section-1));
            check(intervals.size()>1,"Metronomic jump spacing level "+level);
        }
        check(fragments>0 && fragments<=6,"Story must stay sparse");
        check(Track.lethal(Track.RED,0),"Red must collapse");
        check(Track.lethal(Track.HOLE,0),"Hole must drop");
        check(!Track.lethal(Track.RED,.4),"Jump must clear red");
        check(Track.lethal(Track.WALL,0),"Wall must block grounded player");
        check(Progress.select(49,0)==0,"Locked selection");
        check(Progress.complete(49,0)==0,"Cannot skip completion");
        check(Progress.complete(0,10)==10,"Replay must preserve unlocks");
        int unlocked=0;
        for(int level=0;level<50;level++) {
            check(Progress.select(level,unlocked)==level,"Sequential unlock");
            unlocked=Progress.complete(level,unlocked);
            check(unlocked==Math.min(49,level+1),"Unlock exactly one");
            check(Progress.next(level)==(level==49?-1:level+1),"Automatic next boundary");
        }
        Endless e1=new Endless(101L),e2=new Endless(202L);
        HashSet<String> endlessSlices=new HashSet<>();
        StringBuilder run1=new StringBuilder(),run2=new StringBuilder();
        for(int s=0;s<20;s++){
            StringBuilder slice=new StringBuilder();
            for(int r=6+s*18;r<6+(s+1)*18;r++)for(int l=0;l<8;l++){
                int t1=e1.tile(r,l),t2=e2.tile(r,l);
                slice.append(t1);
                if(s<4){run1.append(t1);run2.append(t2);}
            }
            check(endlessSlices.add(slice.toString()),"Endless mode repeated a slice at "+s);
        }
        check(!run1.toString().equals(run2.toString()),"Endless seeds produced identical runs");
        System.out.println("PASS: 50 courses: jumping AND steering mandatory, complete physics playthrough at 30/60 FPS, collisions, sequential progression, and non-repeating Endless mode.");
    }
}
