package Bot2;

import Bot2.fast.FastMath;
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
    }

    void endTurn() throws GameActionException {
        if (curDest == null || rc.getLocation().equals(curDest) || rc.getLocation().equals(lastPosition)) {
            curDest = FastMath.getRandomMapLocation();
        }

        lastPosition = rc.getLocation();
    }

    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(spawnLocs, id%27);

        // if nextOpenSpawn == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(spawnLocs[nextOpenSpawn]);

            atSpawnActions(nextOpenSpawn);
            initTurn();
            play();
        }
    }

    void atSpawnActions(int openSpawn) throws GameActionException {
        // move away from the flag to clear out space
        FlagInfo[] closeFlags = rc.senseNearbyFlags(2);
        if (rc.getRoundNum() == 0 && rc.canPickupFlag(rc.getLocation())) {
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

    public Direction pathfind(MapLocation dest) throws GameActionException {
        Direction wantedMove = rc.getLocation().directionTo(dest);
        MapLocation nextPosition = FastMath.addVec(rc.getLocation(), new MapLocation(wantedMove.dx, wantedMove.dy));

        if (rc.isLocationOccupied(nextPosition) || !rc.sensePassability(nextPosition)) return null;
        return rc.getLocation().directionTo(dest);
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
