import com.yorunge.runner.Collapse;
import com.yorunge.runner.Track;
public class CollapseTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        int[][] tiles=new int[3][8];
        tiles[1][0]=tiles[1][1]=tiles[1][2]=tiles[1][7]=tiles[2][2]=tiles[0][4]=Track.RED;
        Collapse c=new Collapse(tiles);
        check(!c.trigger(0,0,10),"Floor must not trigger");
        check(c.trigger(1,0,10),"First impact missing");
        check(c.age(1,0,10)==0 && c.canEscape(1,0,10.20),"Short escape window missing");
        check(!c.canEscape(1,0,10.25),"Late jump permitted");
        check(c.age(1,1,10.1)<0 && c.age(1,7,10.1)<0,"Neighbors fell immediately");
        check(c.age(1,1,10.18)>=0 && c.age(1,7,10.18)>=0,"Edge/seam propagation failed");
        check(c.age(1,2,10.18)<0 && c.age(1,2,10.36)>=0,"Second wave timing failed");
        check(c.age(2,2,10.55)>=0,"Longitudinal propagation failed");
        check(c.age(0,4,100)<0,"Collapse crossed safe floor/diagonal gap");
        check(!c.trigger(1,0,11),"Retrigger extended grace");
        check(!c.canEscape(1,0,11),"Old collapsed tile can support player");
        check(new Collapse(tiles).age(1,0,100)<0,"Retry must reset collapses");
        Collapse direct=new Collapse(tiles);direct.trigger(1,0,0);
        check(direct.trigger(1,2,.05) && direct.age(1,2,.05)==0,"Direct impact should accelerate scheduled collapse");
        check(Collapse.drop(.2)>0 && Collapse.drop(.4)>2*Collapse.drop(.2),"Falling should accelerate");
        System.out.println("PASS: 240ms escape cutoff, delayed edge/seam chain, isolated plates, no rearming, direct impacts and reset.");
    }
}
