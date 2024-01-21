package Bot13;

import battlecode.common.*;

public class Attacker extends Robot {
    RobotController rc;
    int id;

    public Attacker(RobotController rc, int id) throws GameActionException {
        super(rc, id);

        this.rc = rc;
        this.id = id;
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
        Micro.buildMicro();

        if (rc.isActionReady()) {
            Micro.sense();
            Micro.attackMicro();
        }

        Micro.healMicro();

        if (super.getNearbyCrumbs().length > 0) {
            tempTarget = super.getNearbyCrumbs()[0];
        }

        if (rc.isMovementReady()) {
            if (super.getNearbyAllies().length * 1.2 < super.getNearbyEnemies().length) {
                super.move(MapHelper.poseDecoder(rc.readSharedArray(7+id%3)));
            }
            else if (rc.hasFlag() || super.getNearbyEnemies().length <= FEAR_LIMIT) {
                if (tempTarget != null) super.move(tempTarget);
                else super.move(curDest);
            }
            else {
                super.move(super.allyCenter());
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
