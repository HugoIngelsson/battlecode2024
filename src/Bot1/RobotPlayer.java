package Bot1;

import battlecode.common.*;

import java.util.Random;

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

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        id = rc.readSharedArray(0);
        if (id < 10) {
            robot = new Scout(rc);
        }
        else if (id < 15) {
            robot = new Builder(rc);
        }
        else if (id < 25) {
            robot = new Healer(rc);
        }
        else if (id == 49) {
            rc.writeSharedArray(0, 0);
            robot = new Commander(rc);
        }
        else {
            robot = new Attacker(rc);
        }
        if (id != 49) rc.writeSharedArray(0, id+1);
        MapHelper.HEIGHT = rc.getMapHeight();
        MapHelper.WIDTH = rc.getMapWidth();

        while (true) {
            try {
                // if we aren't spawned, we want to spawn in
                // and then move to clear space for other ducks to spawn
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();

                    // gets the next open spot based on the duck's position in the turn order
                    int nextOpenSpawn = robot.getNextSpawnableLocation(spawnLocs, id%27);

                    // if id == -1, then there are no spawnable positions
                    if (id != -1) {
                        rc.spawn(spawnLocs[nextOpenSpawn]);

                        // move away from the flag to clear out space
                        FlagInfo[] flags = rc.senseNearbyFlags(-1);
                        if (rc.canPickupFlag(rc.getLocation())) {
                            System.out.println(rc.readSharedArray(1) + " " + rc.readSharedArray(2) + " " + rc.readSharedArray(3));
                            if (rc.readSharedArray(1) == 0) {
                                rc.writeSharedArray(1, MapHelper.poseEncoder(rc.getLocation()));
                            }
                            else if (rc.readSharedArray(2) == 0) {
                                rc.writeSharedArray(2, MapHelper.poseEncoder(rc.getLocation()));
                            }
                            else {
                                rc.writeSharedArray(3, MapHelper.poseEncoder(rc.getLocation()));
                            }
                            System.out.println(rc.readSharedArray(1) + " " + rc.readSharedArray(2) + " " + rc.readSharedArray(3));
                        }
                        else if (rc.isMovementReady() && rc.canMove(flags[0].getLocation().directionTo(rc.getLocation())))
                            rc.move(flags[0].getLocation().directionTo(rc.getLocation()));
                    }
                }
                else if (rc.getRoundNum() < 200) {
                    // do something
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                if (rc.getRoundNum() > 2) rc.resign();

//                System.out.println(rc.senseBroadcastFlagLocations()[0]);
                System.out.println(Clock.getBytecodesLeft() + " " + id + " " + rc.isSpawned() + " " + rc.readSharedArray(0));
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
