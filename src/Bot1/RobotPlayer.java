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
        else {
            robot = new Attacker(rc);
        }
        rc.writeSharedArray(0, (id+1)%50);
        MapHelper.HEIGHT = rc.getMapHeight();
        MapHelper.WIDTH = rc.getMapWidth();

        while (true) {
            //System.out.println(Clock.getBytecodesLeft());
            rc.writeSharedArray(1, 0);

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
                        if (rc.isMovementReady() && rc.canMove(flags[0].getLocation().directionTo(rc.getLocation())))
                            rc.move(flags[0].getLocation().directionTo(rc.getLocation()));
                    }
                }
                else if (rc.getRoundNum() < 200) {
                    int q = MapHelper.getQuadrant(rc.getLocation());
                    if (!bitAt(rc.readSharedArray(0), q)) {
                        rc.writeSharedArray(0, rc.readSharedArray(0) | (1<<q));

                        int byteZero = rc.readSharedArray(0);

                        // if we have visited both halves (width) of the field, break horizontal symmetry
                        if ((bitAt(byteZero, 12) || bitAt(byteZero, 11)) && (bitAt(byteZero, 10) || bitAt(byteZero, 9))) {
                            rc.writeSharedArray(0, rc.readSharedArray(0) | 0x8000);
                        }

                        // if we have visited both halves (height) of the field, break vertical symmetry
                        if ((bitAt(byteZero, 12) || bitAt(byteZero, 9)) && (bitAt(byteZero, 10) || bitAt(byteZero, 11))) {
                            rc.writeSharedArray(0, rc.readSharedArray(0) | 0x4000);
                        }

                        System.out.println("ID0: " + rc.readSharedArray(0));
                    }
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                if (rc.getRoundNum() > 2) rc.resign();

//                System.out.println(Clock.getBytecodesLeft() + " " + id + " " + rc.isSpawned());
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
