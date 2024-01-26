package Bot16;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import Bot16.fast.FastMath;

import java.util.Arrays;
import java.util.Random;

public class Builder extends Robot {
    static final int DEFENSE_RADIUS = 2;
    static final int BOMB_DEFENSE_RADIUS = 2;

    RobotController rc;
    int id;
    int dist;
    FlagInfo foundFlag;
    Random rng;

    public Builder(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.foundFlag = null;
        this.curDest = FastMath.getRandomMapLocation();;
        this.rng = new Random();
    }

    @Override
    void playIfUnspawned() throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        MapLocation[] subSpawns = null;

        switch (id % 3) {
            case 0:
                subSpawns = Arrays.copyOfRange(spawnLocs, 0, 9);
                break;
            case 1:
                subSpawns = Arrays.copyOfRange(spawnLocs, 9, 18);
                break;
            case 2:
                subSpawns = Arrays.copyOfRange(spawnLocs, 18, 27);
                break;
            default:
                break;
        }

        // gets the next open spot based on the duck's position in the turn order
        int nextOpenSpawn = getNextSpawnableLocation(subSpawns, id%9);

        // if id == -1, then there are no spawnable positions
        if (nextOpenSpawn != -1) {
            rc.spawn(subSpawns[nextOpenSpawn]);
            super.justSpawned = true;

            super.atSpawnActions();
        }
    }

    void play() throws GameActionException {
        FlagInfo[] nearbyFlags = rc.senseNearbyFlags(-1);
        if (rc.hasFlag()) {
            curDest = super.getClosestBase();
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
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) > 6)
                tempTarget = flag.getLocation();
            else tempTarget = null;

            if (rc.getLocation().equals(tempTarget))
                if (!rc.canPickupFlag(tempTarget)) tempTarget = null;
                else if (rc.isActionReady()) {
                    rc.pickupFlag(tempTarget);
                    hadFlag = true;
                    lastTurn = rc.getRoundNum();
                }
        }

        Micro.sense();
        Micro.buildMicro();
        Micro.attackMicro();

        if (rc.isActionReady()) {
            Micro.sense();
            Micro.attackMicro();
        }

        Micro.healMicro();

        if (super.getNearbyCrumbs().length > 0) {
             curDest = super.getNearbyCrumbs()[0];
        }

        if (rc.isMovementReady()) {
            if (Micro.enemyStrength > 150 && (rc.getHealth() < 200 || rc.hasFlag())) {
                Micro.kite(Micro.getEnemyMiddle());
            }
            else if (Micro.closeFriendsSize < 1 && Micro.allyCount >= 2 && Micro.enemyStrength >= 100 && !rc.hasFlag()) {
                rc.move(super.approachAlly());
            }
            else if (Micro.enemyStrength < 100) {
                if (tempTarget != null) super.move(tempTarget);
                else super.move(curDest);
            }
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
}