package Bot16;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import Bot16.fast.FastMath;

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
        Micro.healMicro();
        Micro.attackMicro();

        if (rc.isActionReady()) {
            Micro.sense();
            Micro.attackMicro();
        }

        if (super.getNearbyCrumbs().length > 0) {
            tempTarget = super.getNearbyCrumbs()[0];
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
