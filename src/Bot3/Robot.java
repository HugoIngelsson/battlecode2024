package Bot3;

import Bot3.fast.FastMath;
import battlecode.common.*;

public abstract class Robot {
    RobotController rc;
    int id;
    MapLocation curDest;
    MapLocation lastPosition;

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

        if (rc.getRoundNum() == 200) {
            int byteZero = rc.readSharedArray(0);
            MapLocation flag1 = MapHelper.poseDecoder(rc.readSharedArray(1));
            if (!RobotPlayer.bitAt(byteZero, 15)) {
                this.curDest = new MapLocation(flag1.x, MapHelper.HEIGHT - flag1.y - 1);
            }
            else if (!RobotPlayer.bitAt(byteZero, 14)) {
                this.curDest = new MapLocation(MapHelper.WIDTH - flag1.x - 1, flag1.y);
            }
            else this.curDest = new MapLocation(MapHelper.WIDTH - flag1.x - 1, MapHelper.HEIGHT - flag1.y - 1);
        }

        PathFinding.initTurn();
    }

    void endTurn() throws GameActionException {
//        if (curDest == null || rc.getLocation().equals(curDest) || rc.getLocation().equals(lastPosition)) {
//            curDest = FastMath.getRandomMapLocation();
//        }
//
//        lastPosition = rc.getLocation();
    }

    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(spawnLocs, id%27);

        // if nextOpenSpawn == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(spawnLocs[nextOpenSpawn]);

            atSpawnActions();
            initTurn();
            play();
        }
    }

    void atSpawnActions() throws GameActionException {
        // move away from the flag to clear out space
        FlagInfo[] closeFlags = rc.senseNearbyFlags(2);
        if (rc.getRoundNum() == 1 && rc.canPickupFlag(rc.getLocation())) {
            if (rc.readSharedArray(1) == 0) {
                rc.writeSharedArray(1, MapHelper.poseEncoder(rc.getLocation()));
            }
            else if (rc.readSharedArray(2) == 0) {
                rc.writeSharedArray(2, MapHelper.poseEncoder(rc.getLocation()));
            }
            else {
                rc.writeSharedArray(3, MapHelper.poseEncoder(rc.getLocation()));
            }
        }
        else if (rc.isMovementReady() && rc.canMove(closeFlags[0].getLocation().directionTo(rc.getLocation())))
            rc.move(closeFlags[0].getLocation().directionTo(rc.getLocation()));

        this.lastPosition = rc.getLocation();
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
        for (int i=id; i<27; i++) {
            if (rc.canSpawn(spawns[i])) return i;
        }

        for (int i=id-1; i>=0; i--) {
            if (rc.canSpawn(spawns[i])) return i;
        }

        return -1;
    }
}
