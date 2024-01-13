package Bot5;

import Bot5.fast.FastMath;
import battlecode.common.*;

import java.util.Arrays;

public abstract class Robot {
    RobotController rc;
    int id;
    int byteZero;
    boolean justSpawned;
    MapLocation curDest;
    MapLocation tempTarget;

    Team myTeam;
    Team enemyTeam;

    RobotInfo[] nearbyEnemies;
    RobotInfo[] nearbyAllies;
    MapLocation[] nearbyCrumbs;
    FlagInfo[] enemyFlags;

    public Robot(RobotController rc, int id) throws GameActionException {
        this.rc = rc;
        this.id = id;

        this.myTeam = rc.getTeam();
        switch (this.myTeam) {
            case A: this.enemyTeam = Team.B; break;
            case B: this.enemyTeam = Team.A; break;
        }

        FastMath.initRand(rc);
    }

    abstract void play() throws GameActionException;

    void initTurn() throws GameActionException {
        this.nearbyEnemies = rc.senseNearbyRobots(-1, this.enemyTeam);
        this.nearbyAllies = rc.senseNearbyRobots(-1, this.myTeam);
        this.nearbyCrumbs = rc.senseNearbyCrumbs(-1);
        this.enemyFlags = rc.senseNearbyFlags(-1, this.enemyTeam);
        this.byteZero = rc.readSharedArray(0);

        if (this.justSpawned && RobotPlayer.bitAt(this.byteZero, 12-id%3)) {
            curDest = MapHelper.poseDecoder(rc.readSharedArray(7+id%3));
        }
        else {
            curDest = MapHelper.poseDecoder(rc.readSharedArray(4+id%3));
            this.justSpawned = false;
        }

        PathFinding.initTurn();
    }

    void endTurn() throws GameActionException {
        if (tempTarget != null && rc.getLocation().distanceSquaredTo(tempTarget) <= 4) tempTarget = null;
    }

    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(spawnLocs, id%27);

        // if nextOpenSpawn == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(spawnLocs[nextOpenSpawn]);
            this.justSpawned = true;

            atSpawnActions();
        }
    }

    void atSpawnActions() throws GameActionException {
        // update death counter for my group
//        int byteThirteen = rc.readSharedArray(13);
//        System.out.println(byteThirteen);
//        byteThirteen += 1<<(id%3*5);
//        rc.writeSharedArray(13, byteThirteen);

        // move away from the flag to clear out space
        FlagInfo[] closeFlags = rc.senseNearbyFlags(0);
        if (rc.getRoundNum() == 1) {
            System.out.println(id + " " + closeFlags.length);
            if (closeFlags.length > 0) {
                if (rc.readSharedArray(1) == 0) {
                    rc.writeSharedArray(1, MapHelper.poseEncoder(rc.getLocation()));

                    int impassables = 0;
                    MapInfo[] mi = rc.senseNearbyMapInfos(-1);
                    for (MapInfo mii : mi) if (!mii.isPassable()) impassables++;
                    rc.writeSharedArray(10, impassables);
                }
                else if (rc.readSharedArray(2) == 0) {
                    rc.writeSharedArray(2, MapHelper.poseEncoder(rc.getLocation()));

                    int impassables = 0;
                    MapInfo[] mi = rc.senseNearbyMapInfos(-1);
                    for (MapInfo mii : mi) if (!mii.isPassable()) impassables++;
                    rc.writeSharedArray(11, impassables);
                }
                else {
                    rc.writeSharedArray(3, MapHelper.poseEncoder(rc.getLocation()));

                    int impassables = 0;
                    MapInfo[] mi = rc.senseNearbyMapInfos(-1);
                    for (MapInfo mii : mi) if (!mii.isPassable()) impassables++;
                    rc.writeSharedArray(12, impassables);
                }
            }
            else {
                closeFlags = rc.senseNearbyFlags(2);
                if (rc.isMovementReady() && rc.canMove(closeFlags[0].getLocation().directionTo(rc.getLocation())))
                    rc.move(closeFlags[0].getLocation().directionTo(rc.getLocation()));
            }
        }
        else {
            // do something
        }
    }

    public Team getMyTeam() {
        return this.myTeam;
    }

    public Team getEnemyTeam() {
        return this.enemyTeam;
    }

    public RobotInfo[] getNearbyEnemies() {
        return this.nearbyEnemies;
    }

    public RobotInfo[] getNearbyAllies() {
        return this.nearbyAllies;
    }

    public MapLocation[] getNearbyCrumbs() {
        return this.nearbyCrumbs;
    }

    public FlagInfo[] getEnemyFlags() { return this.enemyFlags; }

    public int getNextSpawnableLocation(MapLocation[] spawns, int id) {
        for (int i=id; i<spawns.length; i++) {
            if (rc.canSpawn(spawns[i])) return i;
        }

        for (int i=id-1; i>=0; i--) {
            if (rc.canSpawn(spawns[i])) return i;
        }

        return -1;
    }
}
