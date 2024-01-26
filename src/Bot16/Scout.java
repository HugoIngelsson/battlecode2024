package Bot16;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import Bot16.fast.FastMath;

public class Scout extends Robot {
    RobotController rc;
    int id;

    public Scout(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.curDest = FastMath.getRandomMapLocation();
        this.curDest = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    }

    void play() throws GameActionException {
        if (rc.hasFlag()) {
            curDest = super.getClosestBase();

            super.move(curDest);
            return;
        }
        else if (super.getEnemyFlags().length > 0) {
            FlagInfo flag = super.getEnemyFlags()[0];
            if (!flag.isPickedUp())
                tempTarget = flag.getLocation();
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) <= 2) {
                if (rc.canMove(flag.getLocation().directionTo(rc.getLocation()))) {
                    rc.move(flag.getLocation().directionTo(rc.getLocation()));
                }
            }
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) > 4)
                tempTarget = flag.getLocation();

            if (rc.getLocation().equals(tempTarget))
                if (!rc.canPickupFlag(tempTarget)) tempTarget = null;
                else if (rc.isActionReady()) {
                    rc.pickupFlag(tempTarget);
                    hadFlag = true;
                    lastTurn = rc.getRoundNum();
                }
        }

        Micro.sense();
        Micro.attackMicro();

        if (rc.isActionReady()) {
            Micro.sense();
            Micro.attackMicro();
        }

        Micro.healMicro();

        if (rc.isMovementReady()) {
            // Scouts shouldn't be afraid of charging into battle and setting off traps
            // so they don't consider how many enemies there are
            if (tempTarget != null) super.move(tempTarget);
            else super.move(curDest);
        }
    }

    @Override
    void initTurn() throws GameActionException {
        super.initTurn();
    }

    @Override
    void endTurn() throws GameActionException {
        super.endTurn();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(spawnLocs, id%27);

        // if nextOpenSpawn == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(spawnLocs[nextOpenSpawn]);
            super.justSpawned = true;

            super.atSpawnActions();
        }
    }

    @Override
    public int getNextSpawnableLocation(MapLocation[] spawns, int id) {
        double minDist = Integer.MAX_VALUE;
        int minDistID = -1;

        for (int i=spawns.length-1; i>=0; i--) {
            if (rc.canSpawn(spawns[i]) && spawns[i].distanceSquaredTo(curDest) < minDist) {
                minDist = spawns[i].distanceSquaredTo(curDest);
                minDistID = i;
            }
        }

        return minDistID;
    }
}
