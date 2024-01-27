package Bot19;

import Bot19.fast.FastMath;
import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Healer extends Robot {
    RobotController rc;
    int id;

    public Healer(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
        this.curDest = FastMath.getRandomMapLocation();
        this.curDest = new MapLocation(rc.getMapWidth()/2, rc.getMapHeight()/2);
    }

    void play() throws GameActionException {
        if (rc.hasFlag()) {
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
                rc.pickupFlag(tempTarget);
                hadFlag = true;
                lastTurn = rc.getRoundNum();
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
        Micro.healMicro();

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
