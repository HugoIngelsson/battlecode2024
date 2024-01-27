package Bot21;

import Bot21.fast.FastMath;
import battlecode.common.*;

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
        if (rc.hasFlag()) {
            if (rc.getRoundNum() < 101) return;
            else if (rc.getRoundNum() == 101) {
                rc.dropFlag(rc.getLocation().add(curDest.directionTo(rc.getLocation())));
            }

            curDest = super.getClosestBase();

            if (!microAttacker.doMicro())
                super.move(curDest);
            return;
        }
        else if (super.getEnemyFlags().length > 0) {
            FlagInfo flag = super.getEnemyFlags()[0];
            if (!flag.isPickedUp())
                tempTarget = flag.getLocation();
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) <= 2) {
                tempTarget = null;
            }
            else if (flag.getLocation().distanceSquaredTo(rc.getLocation()) > 6)
                tempTarget = flag.getLocation();
            else tempTarget = null;

            if (tempTarget == null);
            else if (rc.canPickupFlag(tempTarget)) {
                Direction dir = rc.getLocation().directionTo(tempTarget);
                if (shouldPickUpFlag(dir, rc.getLocation().directionTo(getClosestBase()).opposite())) {
                    rc.pickupFlag(tempTarget);
                    hadFlag = true;
                    lastTurn = rc.getRoundNum();
                }
            }
            else if (rc.getLocation().equals(tempTarget))
                tempTarget = null;
        }

        if (super.getNearbyCrumbs().length > 0) {
            tempTarget = super.getNearbyCrumbs()[0];
            if (rc.getRoundNum() < 200)
                super.move(tempTarget);
        }

        Micro.sense();
        Micro.buildMicro();
        while (attack());
        if (!microAttacker.doMicro()) {
            if (Micro.attackTarget != null) super.move(Micro.attackTarget.location);
            else if (Micro.chaseTarget != null) super.move(Micro.chaseTarget.location);
            else if (tempTarget != null) super.move(tempTarget);
            else super.move(curDest);
        }
        while (attack());

        if (rc.isActionReady()) {
            Micro.sense();
            Micro.attackMicro();
        }

        Micro.healMicro();
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