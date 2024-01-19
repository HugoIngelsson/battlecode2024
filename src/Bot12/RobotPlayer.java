package Bot12;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class RobotPlayer {
    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static Robot robot;
    static int id;

    public static void run(RobotController rc) throws GameActionException {
        id = rc.readSharedArray(0);
        rc.writeSharedArray(0, (id+1)%50);
        PathFinding.init(rc);
        Micro.init(rc);

        if (id < 5) {
            robot = new Builder(rc, id);
        }
        else if (id < 13) {
            robot = new Scout(rc, id);
        }
        else if (id < 22) {
            robot = new Healer(rc, id);
        }
        else if (id > 45 && id < 49) {
            robot = new Defender(rc, id);
        }
        else if (id == 49) {
            robot = new Commander(rc, id);
        }
        else {
            robot = new Attacker(rc, id);
        }

        MapHelper.HEIGHT = rc.getMapHeight();
        MapHelper.WIDTH = rc.getMapWidth();

        while (true) {
            try {
                // if we aren't spawned, we want to spawn in
                // and then move to clear space for other ducks to spawn
                if (!rc.isSpawned()){
                    robot.playIfUnspawned();
                    robot.initTurn();
                }
                else  {
                    robot.initTurn();
                    robot.play();
                }
                robot.endTurn();
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                // System.out.println(Clock.getBytecodesLeft() + " " + id + " " + rc.isSpawned() + " " + rc.readSharedArray(0));
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    // returns whether the id'th bit (counted from the left) of comp is flipped
    public static boolean bitAt(int comp, int id) {
        return (comp & 1<<id) > 0;
    }
}